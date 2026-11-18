package com.transporter.assignment.optimizer;

import com.transporter.assignment.Samples;
import com.transporter.assignment.TestComponents;
import com.transporter.assignment.exception.InfeasibleAssignmentException;
import com.transporter.assignment.exception.PlanNotFoundException;
import com.transporter.assignment.model.AssignmentPlan;
import com.transporter.assignment.model.Lane;
import com.transporter.assignment.model.LaneAssignment;
import com.transporter.assignment.model.LaneQuote;
import com.transporter.assignment.model.Transporter;
import com.transporter.assignment.model.TransporterInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssignmentOptimizerTest {

    private final AssignmentOptimizer optimizer = TestComponents.optimizer();

    @ParameterizedTest(name = "at most {0} transporters costs {1} using [{2}]")
    @CsvSource({
            "1,   96940, 4",
            "2,   62938, 1 4",
            "3,   60139, 1 4 5",
            "4,   59896, 1 3 4 5",
            "7,   59896, 1 3 4 5",
            "100, 59896, 1 3 4 5"
    })
    void findsTheCheapestPlanForTestCaseOne(int maxTransporters, long expectedCost, String expectedTransporters) {
        AssignmentPlan plan = optimizer.optimize(Samples.input(Samples.TEST_CASE_1), maxTransporters);

        assertThat(plan.totalCost()).isEqualTo(expectedCost);
        assertThat(plan.selectedTransporters()).containsExactlyElementsOf(ids(expectedTransporters));
        assertThat(plan.optimal()).isTrue();
    }

    @Test
    void givesEachLaneToTheCheapestSelectedTransporter() {
        AssignmentPlan plan = optimizer.optimize(Samples.input(Samples.TEST_CASE_1), 3);

        assertThat(plan.assignments()).containsExactly(
                new LaneAssignment(1, 5),
                new LaneAssignment(2, 1),
                new LaneAssignment(3, 4),
                new LaneAssignment(4, 4),
                new LaneAssignment(5, 4));
    }

    @ParameterizedTest(name = "at most {0} transporters costs {1} using [{2}]")
    @CsvSource({
            "1, 201259, 1",
            "2, 148402, 1 4",
            "3, 134876, 1 4 6",
            "4, 127844, 1 3 4 7",
            "5, 125045, 1 3 4 5 7",
            "7, 125045, 1 3 4 5 7"
    })
    void findsTheCheapestPlanForTheFullQuoteSheet(int maxTransporters, long expectedCost, String expectedTransporters) {
        AssignmentPlan plan = optimizer.optimize(Samples.quoteSheet(), maxTransporters);

        assertThat(plan.totalCost()).isEqualTo(expectedCost);
        assertThat(plan.selectedTransporters()).containsExactlyElementsOf(ids(expectedTransporters));
    }

    @ParameterizedTest(name = "at most {0} transporters costs {1} using [{2}]")
    @CsvSource({
            "3, 7500, 1 2 3",
            "4, 7450, 1 2 3 4"
    })
    void findsTheCheapestPlanForTestCaseTwo(int maxTransporters, long expectedCost, String expectedTransporters) {
        AssignmentPlan plan = optimizer.optimize(Samples.input(Samples.TEST_CASE_2), maxTransporters);

        assertThat(plan.totalCost()).isEqualTo(expectedCost);
        assertThat(plan.selectedTransporters()).containsExactlyElementsOf(ids(expectedTransporters));
    }

    @Test
    void reportsHowManyTransportersAreNeededWhenTheLimitIsTooLow() {
        assertThatThrownBy(() -> optimizer.optimize(Samples.input(Samples.TEST_CASE_2), 2))
                .isInstanceOf(InfeasibleAssignmentException.class)
                .hasMessage("Covering all 5 lanes needs at least 3 transporters, but maxTransporters is 2.");
    }

    @Test
    void cannotCoverTheTenLaneSpecExampleWithThreeTransporters() {
        assertThatThrownBy(() -> optimizer.optimize(Samples.input(Samples.SPEC_EXAMPLE), 3))
                .isInstanceOfSatisfying(InfeasibleAssignmentException.class,
                        ex -> assertThat(ex.getMinimumTransportersRequired()).isEqualTo(5));
    }

    @ParameterizedTest
    @CsvSource({"5", "7"})
    void coversTheTenLaneSpecExampleOnceEnoughTransportersAreAllowed(int maxTransporters) {
        AssignmentPlan plan = optimizer.optimize(Samples.input(Samples.SPEC_EXAMPLE), maxTransporters);

        assertThat(plan.totalCost()).isEqualTo(48000);
        assertThat(plan.selectedTransporters()).containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    void spreadsTiedLanesSoMoreTransportersGetUsed() {
        TransporterInput input = input(2,
                transporter(1, quote(1, 100), quote(2, 100)),
                transporter(2, quote(1, 100)));

        AssignmentPlan plan = optimizer.optimize(input, 2);

        assertThat(plan.totalCost()).isEqualTo(200);
        assertThat(plan.selectedTransporters()).containsExactly(1L, 2L);
        assertThat(plan.assignments()).containsExactly(new LaneAssignment(1, 2), new LaneAssignment(2, 1));
    }

    @Test
    void ignoresTransportersThatQuotedNothing() {
        TransporterInput input = input(1,
                transporter(1),
                transporter(2, quote(1, 500)));

        AssignmentPlan plan = optimizer.optimize(input, 1);

        assertThat(plan.selectedTransporters()).containsExactly(2L);
    }

    @Test
    void fallsBackToTheGreedyStrategyWhenTheExactSearchRunsOutOfSteps() {
        AssignmentOptimizer tinyBudget = TestComponents.optimizer(10);

        AssignmentPlan plan = tinyBudget.optimize(Samples.input(Samples.TEST_CASE_1), 3);

        assertThat(plan.optimal()).isFalse();
        assertThat(plan.assignments()).hasSize(5);
        assertThat(plan.selectedTransporters()).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    void reportsThatNoPlanWasFoundWhenTheFallbackCannotCoverEveryLane() {
        AssignmentOptimizer tinyBudget = TestComponents.optimizer(10);

        assertThatThrownBy(() -> tinyBudget.optimize(Samples.input(Samples.SPEC_EXAMPLE), 3))
                .isInstanceOf(PlanNotFoundException.class)
                .hasMessageStartingWith("Could not find a plan covering all 10 lanes with at most 3 transporters.");
    }

    static TransporterInput input(int laneCount, Transporter... transporters) {
        List<Lane> lanes = LongStream.rangeClosed(1, laneCount)
                .mapToObj(id -> new Lane(id, "Origin " + id, "Destination " + id))
                .toList();
        return new TransporterInput(lanes, List.of(transporters));
    }

    static Transporter transporter(long id, LaneQuote... quotes) {
        return new Transporter(id, "Transporter " + id, List.of(quotes));
    }

    static LaneQuote quote(long laneId, long amount) {
        return new LaneQuote(laneId, amount);
    }

    private static List<Long> ids(String spaceSeparated) {
        return Arrays.stream(spaceSeparated.trim().split("\\s+")).map(Long::valueOf).toList();
    }
}
