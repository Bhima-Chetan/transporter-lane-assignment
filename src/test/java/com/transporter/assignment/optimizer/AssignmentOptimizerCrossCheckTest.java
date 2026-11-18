package com.transporter.assignment.optimizer;

import com.transporter.assignment.exception.InfeasibleAssignmentException;
import com.transporter.assignment.model.AssignmentPlan;
import com.transporter.assignment.model.Lane;
import com.transporter.assignment.model.LaneAssignment;
import com.transporter.assignment.model.LaneQuote;
import com.transporter.assignment.model.Transporter;
import com.transporter.assignment.model.TransporterInput;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssignmentOptimizerCrossCheckTest {

    private static final int ROUNDS = 500;

    private final AssignmentOptimizer optimizer = new AssignmentOptimizer(20_000_000);
    private final Random random = new Random(20241101);

    @Test
    void agreesWithExhaustiveSearchOnRandomInputsFullOfTiedQuotes() {
        for (int round = 0; round < ROUNDS; round++) {
            TransporterInput input = randomInput();
            int maxTransporters = 1 + random.nextInt(input.transporters().size());
            Expected expected = exhaustiveSearch(input, maxTransporters);

            if (expected.feasible()) {
                AssignmentPlan plan = optimizer.optimize(input, maxTransporters);
                assertThat(plan.totalCost()).as("total cost in round %d", round).isEqualTo(expected.totalCost());
                assertThat(plan.selectedTransporters()).as("transporters used in round %d", round)
                        .hasSize(expected.transportersUsed());
                assertIsConsistent(plan, input, maxTransporters);
            } else {
                assertThatThrownBy(() -> optimizer.optimize(input, maxTransporters))
                        .as("round %d should be infeasible", round)
                        .isInstanceOfSatisfying(InfeasibleAssignmentException.class, ex ->
                                assertThat(ex.getMinimumTransportersRequired()).isEqualTo(expected.fewestCoveringAllLanes()));
            }
        }
    }

    private TransporterInput randomInput() {
        int laneCount = 1 + random.nextInt(5);
        int transporterCount = 1 + random.nextInt(6);

        List<List<LaneQuote>> quotesByTransporter = new ArrayList<>();
        for (int transporter = 0; transporter < transporterCount; transporter++) {
            List<LaneQuote> quotes = new ArrayList<>();
            for (long laneId = 1; laneId <= laneCount; laneId++) {
                if (random.nextDouble() < 0.6) {
                    quotes.add(new LaneQuote(laneId, randomQuote()));
                }
            }
            quotesByTransporter.add(quotes);
        }
        for (long laneId = 1; laneId <= laneCount; laneId++) {
            long lane = laneId;
            boolean quoted = quotesByTransporter.stream().flatMap(List::stream).anyMatch(q -> q.laneId() == lane);
            if (!quoted) {
                quotesByTransporter.get(random.nextInt(transporterCount)).add(new LaneQuote(laneId, randomQuote()));
            }
        }

        List<Lane> lanes = new ArrayList<>();
        for (long laneId = 1; laneId <= laneCount; laneId++) {
            lanes.add(new Lane(laneId, "From " + laneId, "To " + laneId));
        }
        List<Transporter> transporters = new ArrayList<>();
        for (int transporter = 0; transporter < transporterCount; transporter++) {
            transporters.add(new Transporter(transporter + 1L, "T" + (transporter + 1), quotesByTransporter.get(transporter)));
        }
        return new TransporterInput(lanes, transporters);
    }

    private long randomQuote() {
        return 1L + random.nextInt(4);
    }

    private static Expected exhaustiveSearch(TransporterInput input, int maxTransporters) {
        List<Transporter> transporters = input.transporters();
        long bestCost = Long.MAX_VALUE;
        int bestUsed = 0;
        int fewestCovering = Integer.MAX_VALUE;

        for (int mask = 1; mask < (1 << transporters.size()); mask++) {
            List<Transporter> subset = new ArrayList<>();
            for (int bit = 0; bit < transporters.size(); bit++) {
                if ((mask & (1 << bit)) != 0) {
                    subset.add(transporters.get(bit));
                }
            }

            long cost = 0;
            List<List<Long>> tiedWinnersByLane = new ArrayList<>();
            boolean coversEveryLane = true;
            for (Lane lane : input.lanes()) {
                Long cheapest = subset.stream()
                        .map(transporter -> quoteFor(transporter, lane.id()))
                        .filter(Objects::nonNull)
                        .min(Long::compare)
                        .orElse(null);
                if (cheapest == null) {
                    coversEveryLane = false;
                    break;
                }
                cost += cheapest;
                tiedWinnersByLane.add(subset.stream()
                        .filter(transporter -> cheapest.equals(quoteFor(transporter, lane.id())))
                        .map(Transporter::id)
                        .toList());
            }
            if (!coversEveryLane) {
                continue;
            }

            fewestCovering = Math.min(fewestCovering, subset.size());
            if (subset.size() > maxTransporters) {
                continue;
            }
            int used = mostDistinctWinners(tiedWinnersByLane, 0, new ArrayDeque<>());
            if (cost < bestCost || (cost == bestCost && used > bestUsed)) {
                bestCost = cost;
                bestUsed = used;
            }
        }
        return new Expected(bestCost == Long.MAX_VALUE ? null : bestCost, bestUsed, fewestCovering);
    }

    private static int mostDistinctWinners(List<List<Long>> tiedWinnersByLane, int lane, Deque<Long> picked) {
        if (lane == tiedWinnersByLane.size()) {
            return (int) picked.stream().distinct().count();
        }
        int most = 0;
        for (Long transporterId : tiedWinnersByLane.get(lane)) {
            picked.push(transporterId);
            most = Math.max(most, mostDistinctWinners(tiedWinnersByLane, lane + 1, picked));
            picked.pop();
        }
        return most;
    }

    private static void assertIsConsistent(AssignmentPlan plan, TransporterInput input, int maxTransporters) {
        assertThat(plan.assignments()).extracting(LaneAssignment::laneId)
                .containsExactlyElementsOf(input.lanes().stream().map(Lane::id).toList());

        long recomputedCost = 0;
        for (LaneAssignment assignment : plan.assignments()) {
            Transporter transporter = input.transporters().stream()
                    .filter(candidate -> candidate.id() == assignment.transporterId())
                    .findFirst()
                    .orElseThrow();
            Long quote = quoteFor(transporter, assignment.laneId());
            assertThat(quote).as("transporter %d must have quoted lane %d", transporter.id(), assignment.laneId()).isNotNull();
            recomputedCost += quote;
        }
        assertThat(recomputedCost).isEqualTo(plan.totalCost());

        assertThat(plan.selectedTransporters())
                .containsExactlyElementsOf(plan.assignments().stream().map(LaneAssignment::transporterId).distinct().sorted().toList())
                .hasSizeLessThanOrEqualTo(maxTransporters);
    }

    private static Long quoteFor(Transporter transporter, long laneId) {
        return transporter.laneQuotes().stream()
                .filter(quote -> quote.laneId() == laneId)
                .map(LaneQuote::quote)
                .findFirst()
                .orElse(null);
    }

    private record Expected(Long totalCost, int transportersUsed, int fewestCoveringAllLanes) {

        boolean feasible() {
            return totalCost != null;
        }
    }
}
