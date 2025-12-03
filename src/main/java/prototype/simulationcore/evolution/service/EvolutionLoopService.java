package prototype.simulationcore.evolution.service;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import prototype.lineageruntime.checkpoint.RuntimeServiceIds;
import prototype.lineageruntime.checkpoint.ServiceOperationEvent;
import prototype.lineageruntime.kafka.EventProducer;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.domain.LineageEvent;
import prototype.simulationcore.evolution.dto.AgentSummary;
import prototype.simulationcore.evolution.dto.EvolutionStatus;
import prototype.simulationcore.evolution.dto.GenerationReport;
import prototype.simulationcore.evolution.dto.GenerationStats;
import prototype.simulationcore.evolution.dto.LeaderboardEntry;
import prototype.simulationcore.evolution.selection.SelectionSettings;
import prototype.simulationcore.evolution.selection.SelectionStrategy;
import prototype.simulationcore.evolution.selection.SelectionStrategyFactory;
import prototype.simulationcore.repository.AgentPolicyRepository;
import prototype.simulationcore.repository.AgentRepository;
import prototype.simulationcore.service.AgentDynamics;
import prototype.simulationcore.environment.DefaultEnvironment;
import prototype.simulationcore.environment.Environment;
import prototype.simulationcore.policy.AbstractAgentPolicy;
import prototype.simulationcore.policy.RuleBasedPolicy;
import prototype.simulationcore.policy.RuleBasedPolicy.DecisionRule;
import prototype.simulationcore.policy.WeightedPolicy;

@Service
public class EvolutionLoopService {

    private static final int HISTORY_LIMIT = 25;

    private final AgentRepository agentRepository;
    private final AgentPolicyRepository policyRepository;
    private final RewardTracker rewardTracker;
    private final SelectionStrategyFactory selectionStrategyFactory;
    private final PolicyMutationService policyMutationService;
    private final AgentDynamics agentDynamics;
    private final EventProducer eventProducer;
    private final ApplicationEventPublisher eventPublisher;

    private final Object monitor = new Object();
    private EvolutionRunContext activeRun;

    public EvolutionLoopService(AgentRepository agentRepository,
                                AgentPolicyRepository policyRepository,
                                RewardTracker rewardTracker,
                                SelectionStrategyFactory selectionStrategyFactory,
                                PolicyMutationService policyMutationService,
                                AgentDynamics agentDynamics,
                                EventProducer eventProducer,
                                ApplicationEventPublisher eventPublisher) {
        this.agentRepository = agentRepository;
        this.policyRepository = policyRepository;
        this.rewardTracker = rewardTracker;
        this.selectionStrategyFactory = selectionStrategyFactory;
        this.policyMutationService = policyMutationService;
        this.agentDynamics = agentDynamics;
        this.eventProducer = eventProducer;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public EvolutionStatus initializePopulation(int populationSize,
                                                UUID basePolicyId,
                                                SelectionSettings requestedSettings,
                                                double mutationRate) {
        if (populationSize <= 0) {
            throw new IllegalArgumentException("Population size must be positive");
        }
        SelectionSettings settings = requestedSettings == null
                ? SelectionSettings.defaults(populationSize)
                : requestedSettings;
        double boundedMutationRate = Math.max(0.0, Math.min(1.0, mutationRate));

        synchronized (monitor) {
            AbstractAgentPolicy basePolicy = resolveBasePolicy(basePolicyId);
            List<Agent> seeds = new ArrayList<>(populationSize);
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < populationSize; i++) {
                double noise = boundedMutationRate * random.nextDouble();
                AbstractAgentPolicy policy = noise > 0
                        ? policyMutationService.mutate(basePolicy, noise)
                        : basePolicy;
                Agent agent = Agent.bootstrap(policy);
                agent.setGeneration(0);
                seeds.add(agent);
            }
            List<Agent> persisted = agentRepository.saveAll(seeds);
            persisted.forEach(agent -> rewardTracker.recordReward(agent.getAgentId(), 0.0, 0L));

            activeRun = new EvolutionRunContext(persisted, settings, boundedMutationRate);
            return activeRun.status();
        }
    }

    @Transactional
    public GenerationReport runGeneration() {
        synchronized (monitor) {
            EvolutionRunContext context = ensureActiveRun();
            List<Agent> population = agentRepository.findAllById(context.populationAgentIds);
            if (population.isEmpty()) {
                return GenerationReport.empty();
            }

            SelectionStrategy strategy = selectionStrategyFactory.create(context.selectionSettings);
            List<Double> rewardSamples = new ArrayList<>(population.size());
            List<UUID> violators = new ArrayList<>();

            for (Agent agent : population) {
                AgentState previous = agent.snapshotState();
                Environment environment = new DefaultEnvironment(previous);
                Action action = agent.decide(environment);
                AgentState updated = agentDynamics.apply(action, previous);
                agent.setState(updated);
                double reward = agentDynamics.score(action);
                rewardSamples.add(reward);
                rewardTracker.recordReward(agent.getAgentId(), reward, context.nextTick());
                agent.adjustFitness(reward);
                agent.incrementGeneration();
                agentDynamics.evaluateSafety(updated).ifPresent(reason -> {
                    agent.recordSafetyViolation();
                    violators.add(agent.getAgentId());
                });
                LineageEvent event = LineageEvent.capture(agent.getAgentId().toString(), previous, updated);
                eventProducer.send(event);
            }

            agentRepository.saveAll(population);

            int survivorTarget = Math.max(1, Math.min(context.selectionSettings.survivorCount(), population.size()));
            List<Agent> survivors = new ArrayList<>(strategy.select(population, survivorTarget));
            if (survivors.isEmpty()) {
                survivors.add(population.stream()
                        .max(Comparator.comparingDouble(Agent::getFitness))
                        .orElse(population.get(0)));
            }
            List<Agent> distinctSurvivors = survivors.stream()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toCollection(LinkedHashSet::new), ArrayList::new));

            context.rankSurvivors(distinctSurvivors, rewardTracker);
            List<Agent> offspring = generateOffspring(context, distinctSurvivors);
            List<Agent> persistedOffspring = offspring.isEmpty() ? List.of() : agentRepository.saveAll(offspring);

            context.refreshPopulation(distinctSurvivors, persistedOffspring);

            GenerationReport report = context.buildReport(population, rewardSamples, violators, rewardTracker);
            context.recordReport(report);
            eventPublisher.publishEvent(new ServiceOperationEvent(RuntimeServiceIds.SIMULATION_CORE));
            return report;
        }
    }

    public GenerationReport getGenerationReport(int generation) {
        synchronized (monitor) {
            if (activeRun == null) {
                return GenerationReport.empty();
            }
            GenerationReport latest = activeRun.history.peekLast();
            return activeRun.history.stream()
                    .filter(report -> report.generation() == generation)
                    .findFirst()
                    .orElse(latest == null ? GenerationReport.empty() : latest);
        }
    }

    public EvolutionStatus pause() {
        synchronized (monitor) {
            if (activeRun == null) {
                return EvolutionStatus.idle();
            }
            activeRun.running = false;
            activeRun.lastUpdated = Instant.now();
            return activeRun.status();
        }
    }

    public EvolutionStatus status() {
        synchronized (monitor) {
            return activeRun == null ? EvolutionStatus.idle() : activeRun.status();
        }
    }

    public List<LeaderboardEntry> leaderboard() {
        return rewardTracker.leaderboard(10);
    }

    private List<Agent> generateOffspring(EvolutionRunContext context, List<Agent> survivors) {
        if (survivors.isEmpty()) {
            return List.of();
        }
        int target = context.populationSize - survivors.size();
        if (target <= 0) {
            return List.of();
        }
        List<Agent> offspring = new ArrayList<>(target);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < target; i++) {
            Agent parent = survivors.get(i % survivors.size());
            double noise = context.mutationRate * random.nextDouble();
            AbstractAgentPolicy template = toAbstractPolicy(parent.getPolicy());
            AbstractAgentPolicy policy = noise > 0
                    ? policyMutationService.mutate(template, noise)
                    : policyMutationService.replicate(template);
            Agent child = Agent.bootstrap(policy);
            child.setParentId(parent.getAgentId());
            child.setGeneration(context.generation + 1);
            offspring.add(child);
        }
        return offspring;
    }

    private AbstractAgentPolicy resolveBasePolicy(UUID basePolicyId) {
        if (basePolicyId != null) {
            return policyRepository.findById(basePolicyId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown policy: " + basePolicyId));
        }
        return policyRepository.findTopByOrderByCreatedAtAsc()
                .orElseGet(this::seedFallbackPolicy);
    }

    private AbstractAgentPolicy seedFallbackPolicy() {
        WeightedPolicy weightedPolicy = new WeightedPolicy();
        weightedPolicy.setActionWeights(Map.of(
                Action.MOVE, 5.0,
                Action.CONSUME, 3.0,
                Action.INTERACT, 1.0,
                Action.REPLICATE, -2.0,
                Action.WAIT, 0.5,
                Action.REST, 1.0
        ));
        weightedPolicy.setParameters(Map.of(
                "interaction_bias", -1.0,
                "replication_bias", -5.0,
                "rest_bias", 0.5
        ));
        policyRepository.save(weightedPolicy);

        RuleBasedPolicy ruleBasedPolicy = new RuleBasedPolicy();
        ruleBasedPolicy.addRule(new DecisionRule("energy", 20.0, Action.REST, false));
        ruleBasedPolicy.addRule(new DecisionRule("resources", 75.0, Action.REPLICATE, true));
        ruleBasedPolicy.addRule(new DecisionRule("threat", 50.0, Action.MOVE, true));
        return policyRepository.save(ruleBasedPolicy);
    }

    private EvolutionRunContext ensureActiveRun() {
        if (activeRun == null) {
            throw new IllegalStateException("No evolution run is active");
        }
        return activeRun;
    }

    private AbstractAgentPolicy toAbstractPolicy(prototype.simulationcore.policy.AgentPolicy policy) {
        if (policy instanceof AbstractAgentPolicy abstractPolicy) {
            return abstractPolicy;
        }
        throw new IllegalStateException("Agent policy is not persistent: " + policy);
    }

    private static double computeTrend(List<Double> samples) {
        if (samples.size() < 2) {
            return 0.0;
        }
        double meanIndex = (samples.size() - 1) / 2.0;
        double meanValue = samples.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double numerator = 0.0;
        double denominator = 0.0;
        for (int i = 0; i < samples.size(); i++) {
            double delta = i - meanIndex;
            numerator += delta * (samples.get(i) - meanValue);
            denominator += delta * delta;
        }
        return denominator == 0.0 ? 0.0 : numerator / denominator;
    }

    private static class EvolutionRunContext {

        private final UUID runId = UUID.randomUUID();
        private final int populationSize;
        private final SelectionSettings selectionSettings;
        private final Deque<GenerationReport> history = new ArrayDeque<>();
        private final Instant startedAt = Instant.now();

        private List<UUID> populationAgentIds;
        private int generation;
        private long tickCounter;
        private boolean running = true;
        private double mutationRate;
        private Instant lastUpdated = startedAt;

        private EvolutionRunContext(List<Agent> population,
                                    SelectionSettings settings,
                                    double mutationRate) {
            this.populationSize = population.size();
            this.selectionSettings = Objects.requireNonNull(settings, "selectionSettings");
            this.populationAgentIds = population.stream()
                    .map(Agent::getAgentId)
                    .toList();
            this.mutationRate = mutationRate;
        }

        private long nextTick() {
            return tickCounter++;
        }

        private void refreshPopulation(List<Agent> survivors, List<Agent> offspring) {
            List<UUID> survivorIds = survivors.stream()
                    .map(Agent::getAgentId)
                    .toList();
            List<UUID> offspringIds = offspring.stream()
                    .map(Agent::getAgentId)
                    .toList();
            this.populationAgentIds = new ArrayList<>(populationSize);
            this.populationAgentIds.addAll(survivorIds);
            this.populationAgentIds.addAll(offspringIds);
            this.lastUpdated = Instant.now();
            this.generation++;
        }

        private void recordReport(GenerationReport report) {
            if (report == null) {
                return;
            }
            history.addFirst(report);
            while (history.size() > HISTORY_LIMIT) {
                history.removeLast();
            }
            lastUpdated = Instant.now();
        }

        private void rankSurvivors(List<Agent> survivors, RewardTracker tracker) {
            for (int i = 0; i < survivors.size(); i++) {
                tracker.updateGenerationRank(survivors.get(i).getAgentId(), i + 1);
            }
        }

        private GenerationReport buildReport(List<Agent> population,
                                             List<Double> rewardSamples,
                                             List<UUID> violators,
                                             RewardTracker tracker) {
            double averageFitness = population.stream()
                    .mapToDouble(Agent::getFitness)
                    .average()
                    .orElse(0.0);
            double maxFitness = population.stream()
                    .mapToDouble(Agent::getFitness)
                    .max()
                    .orElse(0.0);
            double rewardMean = rewardSamples.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            double rewardVariance = rewardSamples.stream()
                    .mapToDouble(sample -> Math.pow(sample - rewardMean, 2))
                    .average()
                    .orElse(0.0);
            double rewardTrend = computeTrend(rewardSamples);

            GenerationStats stats = new GenerationStats(
                    averageFitness,
                    maxFitness,
                    rewardMean,
                    rewardVariance,
                    rewardTrend
            );

            List<AgentSummary> bestAgents = population.stream()
                    .sorted(Comparator.comparingDouble(Agent::getFitness).reversed())
                    .limit(3)
                    .map(agent -> new AgentSummary(
                            agent.getAgentId(),
                            agent.getFitness(),
                            agent.getSafetyViolations(),
                            tracker.getCumulativeReward(agent.getAgentId())
                    ))
                    .toList();

            return new GenerationReport(
                    runId,
                    generation,
                    stats,
                    bestAgents,
                    violators
            );
        }

        private EvolutionStatus status() {
            return new EvolutionStatus(
                    runId,
                    running,
                    generation,
                    populationSize,
                    selectionSettings.strategyType(),
                    mutationRate,
                    startedAt,
                    lastUpdated
            );
        }
    }
}


