package prototype.simulationcore.evolution.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import prototype.ContainerizedSpringBootTest;
import prototype.simulationcore.evolution.dto.EvolutionStatus;
import prototype.simulationcore.evolution.dto.GenerationReport;

class EvolutionLoopIntegrationTest extends ContainerizedSpringBootTest {

    @Autowired
    private EvolutionLoopService evolutionLoopService;

    @Test
    void initializePopulationAndRunGenerationProducesReport() {
        EvolutionStatus status = evolutionLoopService.initializePopulation(4, null, null, 0.35);
        assertThat(status.populationSize()).isEqualTo(4);
        assertThat(status.mutationRate()).isBetween(0.0, 0.35);

        GenerationReport report = evolutionLoopService.runGeneration();

        assertThat(report.stats().averageFitness()).isGreaterThanOrEqualTo(0.0);
        assertThat(report.bestAgents()).isNotEmpty();
        assertThat(report.safetyViolations()).isNotNull();
    }
}

