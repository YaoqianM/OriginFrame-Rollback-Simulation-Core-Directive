package prototype.simulationcore.safety;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import prototype.simulationcore.domain.Action;

@ConfigurationProperties(prefix = "safety")
public class SafetyProperties {

    private Boundary boundary = new Boundary();
    private Resource resource = new Resource();
    private Interaction interaction = new Interaction();
    private Rate rate = new Rate();
    private List<CustomRule> customRules = new ArrayList<>();
    private int eliminationThreshold = 3;

    public Boundary getBoundary() {
        return boundary;
    }

    public void setBoundary(Boundary boundary) {
        this.boundary = boundary;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Interaction getInteraction() {
        return interaction;
    }

    public void setInteraction(Interaction interaction) {
        this.interaction = interaction;
    }

    public Rate getRate() {
        return rate;
    }

    public void setRate(Rate rate) {
        this.rate = rate;
    }

    public List<CustomRule> getCustomRules() {
        return customRules;
    }

    public void setCustomRules(List<CustomRule> customRules) {
        this.customRules = customRules;
    }

    public int getEliminationThreshold() {
        return eliminationThreshold;
    }

    public void setEliminationThreshold(int eliminationThreshold) {
        this.eliminationThreshold = eliminationThreshold;
    }

    public static class Boundary {
        private double minX = -100.0;
        private double maxX = 100.0;
        private double minY = -100.0;
        private double maxY = 100.0;
        private double minZ = -10.0;
        private double maxZ = 10.0;
        private Severity severity = Severity.CRITICAL;

        public double getMinX() {
            return minX;
        }

        public void setMinX(double minX) {
            this.minX = minX;
        }

        public double getMaxX() {
            return maxX;
        }

        public void setMaxX(double maxX) {
            this.maxX = maxX;
        }

        public double getMinY() {
            return minY;
        }

        public void setMinY(double minY) {
            this.minY = minY;
        }

        public double getMaxY() {
            return maxY;
        }

        public void setMaxY(double maxY) {
            this.maxY = maxY;
        }

        public double getMinZ() {
            return minZ;
        }

        public void setMinZ(double minZ) {
            this.minZ = minZ;
        }

        public double getMaxZ() {
            return maxZ;
        }

        public void setMaxZ(double maxZ) {
            this.maxZ = maxZ;
        }

        public Severity getSeverity() {
            return severity;
        }

        public void setSeverity(Severity severity) {
            this.severity = severity;
        }
    }

    public static class Resource {
        private double minEnergy = 0.0;
        private double minResources = 0.0;
        private Map<Action, Double> minimumEnergyForAction = new EnumMap<>(Action.class);
        private Map<Action, Double> minimumResourcesForAction = new EnumMap<>(Action.class);
        private Severity severity = Severity.VIOLATION;

        public double getMinEnergy() {
            return minEnergy;
        }

        public void setMinEnergy(double minEnergy) {
            this.minEnergy = minEnergy;
        }

        public double getMinResources() {
            return minResources;
        }

        public void setMinResources(double minResources) {
            this.minResources = minResources;
        }

        public Map<Action, Double> getMinimumEnergyForAction() {
            return minimumEnergyForAction;
        }

        public void setMinimumEnergyForAction(Map<Action, Double> minimumEnergyForAction) {
            this.minimumEnergyForAction = minimumEnergyForAction;
        }

        public Map<Action, Double> getMinimumResourcesForAction() {
            return minimumResourcesForAction;
        }

        public void setMinimumResourcesForAction(Map<Action, Double> minimumResourcesForAction) {
            this.minimumResourcesForAction = minimumResourcesForAction;
        }

        public Severity getSeverity() {
            return severity;
        }

        public void setSeverity(Severity severity) {
            this.severity = severity;
        }
    }

    public static class Interaction {
        private String requiredSignalKey = "neighbors";
        private double minSignalValue = 0.5;
        private Severity severity = Severity.VIOLATION;
        private int minimumGeneration = 1;

        public String getRequiredSignalKey() {
            return requiredSignalKey;
        }

        public void setRequiredSignalKey(String requiredSignalKey) {
            this.requiredSignalKey = requiredSignalKey;
        }

        public double getMinSignalValue() {
            return minSignalValue;
        }

        public void setMinSignalValue(double minSignalValue) {
            this.minSignalValue = minSignalValue;
        }

        public Severity getSeverity() {
            return severity;
        }

        public void setSeverity(Severity severity) {
            this.severity = severity;
        }

        public int getMinimumGeneration() {
            return minimumGeneration;
        }

        public void setMinimumGeneration(int minimumGeneration) {
            this.minimumGeneration = minimumGeneration;
        }
    }

    public static class Rate {
        private List<RateLimitRule> limits = new ArrayList<>();

        public List<RateLimitRule> getLimits() {
            return limits;
        }

        public void setLimits(List<RateLimitRule> limits) {
            this.limits = limits;
        }
    }

    public static class RateLimitRule {
        private Action action = Action.MOVE;
        private int maxAttempts = 10;
        private Duration window = Duration.ofSeconds(5);
        private Severity severity = Severity.WARNING;

        public RateLimitRule() {
        }

        public RateLimitRule(Action action,
                             int maxAttempts,
                             Duration window,
                             @DefaultValue("WARNING") Severity severity) {
            this.action = action;
            this.maxAttempts = maxAttempts;
            this.window = window;
            this.severity = severity;
        }

        public Action getAction() {
            return action;
        }

        public void setAction(Action action) {
            this.action = action;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }

        public Severity getSeverity() {
            return severity;
        }

        public void setSeverity(Severity severity) {
            this.severity = severity;
        }
    }

    public static class CustomRule {
        private String id;
        private Severity severity = Severity.WARNING;
        private String rule;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Severity getSeverity() {
            return severity;
        }

        public void setSeverity(Severity severity) {
            this.severity = severity;
        }

        public String getRule() {
            return rule;
        }

        public void setRule(String rule) {
            this.rule = rule;
        }
    }
}

