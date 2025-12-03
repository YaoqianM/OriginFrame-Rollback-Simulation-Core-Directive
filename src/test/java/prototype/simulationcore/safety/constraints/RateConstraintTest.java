package prototype.simulationcore.safety.constraints;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.safety.ConstraintContext;
import prototype.simulationcore.safety.ConstraintPhase;
import prototype.simulationcore.safety.SafetyProperties;
import prototype.simulationcore.safety.ValidationResult;

class RateConstraintTest {

    private RateConstraint constraint;
    private Agent agent;
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        SafetyProperties properties = new SafetyProperties();
        SafetyProperties.RateLimitRule rule = new SafetyProperties.RateLimitRule(
                Action.MOVE,
                2,
                Duration.ofSeconds(2),
                null
        );
        properties.getRate().setLimits(java.util.List.of(rule));
        clock = new MutableClock(Instant.now());
        constraint = new RateConstraint(properties, clock);

        agent = new Agent();
        agent.setState(AgentState.initial());
    }

    @AfterEach
    void clearContext() {
        ConstraintContext.clear();
    }

    @Test
    void blocksActionsThatExceedRateLimitWindow() {
        ConstraintContext.setPhase(ConstraintPhase.PRE_ACTION);
        ValidationResult first = constraint.validate(agent, Action.MOVE, null);
        ValidationResult second = constraint.validate(agent, Action.MOVE, null);
        ValidationResult third = constraint.validate(agent, Action.MOVE, null);

        assertThat(first.valid()).isTrue();
        assertThat(second.valid()).isTrue();
        assertThat(third.valid()).isFalse();
        assertThat(third.message()).contains("rate limit");
    }

    @Test
    void allowsActionAfterWindowExpires() {
        ConstraintContext.setPhase(ConstraintPhase.PRE_ACTION);
        constraint.validate(agent, Action.MOVE, null);
        constraint.validate(agent, Action.MOVE, null);
        clock.advance(Duration.ofSeconds(3));

        ValidationResult result = constraint.validate(agent, Action.MOVE, null);

        assertThat(result.valid()).isTrue();
    }

    @Test
    void skipsEvaluationDuringPostActionPhase() {
        ConstraintContext.setPhase(ConstraintPhase.POST_ACTION);
        ValidationResult result = constraint.validate(agent, Action.MOVE, null);

        assertThat(result.valid()).isTrue();
    }

    private static final class MutableClock extends Clock {

        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}

