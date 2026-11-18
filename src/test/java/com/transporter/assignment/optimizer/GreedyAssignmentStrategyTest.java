package com.transporter.assignment.optimizer;

import com.transporter.assignment.Samples;
import com.transporter.assignment.model.AssignmentPlan;
import com.transporter.assignment.model.TransporterInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GreedyAssignmentStrategyTest {

    private final GreedyAssignmentStrategy greedy = new GreedyAssignmentStrategy();

    @ParameterizedTest(name = "at most {0} transporters costs {1}")
    @CsvSource({
            "1, 96940",
            "2, 62938",
            "3, 60139"
    })
    void findsTheOptimumForTestCaseOne(int maxTransporters, long expectedCost) {
        assertThat(plan(Samples.input(Samples.TEST_CASE_1), maxTransporters))
                .get()
                .extracting(AssignmentPlan::totalCost)
                .isEqualTo(expectedCost);
    }

    @Test
    void picksTransportersThatCoverMissingLanesBeforeCheaperOnes() {
        Optional<AssignmentPlan> plan = plan(Samples.input(Samples.TEST_CASE_2), 3);

        assertThat(plan).get().satisfies(found -> {
            assertThat(found.totalCost()).isEqualTo(7500);
            assertThat(found.selectedTransporters()).containsExactly(1L, 2L, 3L);
        });
    }

    @Test
    void returnsNothingWhenItCannotCoverEveryLane() {
        assertThat(plan(Samples.input(Samples.SPEC_EXAMPLE), 3)).isEmpty();
    }

    @Test
    void doesNotClaimItsPlansAreOptimal() {
        assertThat(greedy.guaranteesOptimal()).isFalse();
    }

    private Optional<AssignmentPlan> plan(TransporterInput input, int maxTransporters) {
        AssignmentProblem problem = AssignmentProblem.from(input);
        return greedy.solve(problem, maxTransporters).map(solution -> problem.toPlan(solution, false));
    }
}
