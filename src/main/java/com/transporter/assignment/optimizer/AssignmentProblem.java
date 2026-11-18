package com.transporter.assignment.optimizer;

import com.transporter.assignment.model.AssignmentPlan;
import com.transporter.assignment.model.Lane;
import com.transporter.assignment.model.LaneAssignment;
import com.transporter.assignment.model.LaneQuote;
import com.transporter.assignment.model.Transporter;
import com.transporter.assignment.model.TransporterInput;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public final class AssignmentProblem {

    static final long NO_QUOTE = Long.MAX_VALUE;

    private final List<Lane> lanes;
    private final List<Transporter> candidates;
    private final long[][] quotes;

    private AssignmentProblem(List<Lane> lanes, List<Transporter> candidates, long[][] quotes) {
        this.lanes = lanes;
        this.candidates = candidates;
        this.quotes = quotes;
    }

    public static AssignmentProblem from(TransporterInput input) {
        List<Transporter> candidates = input.transporters().stream()
                .filter(transporter -> !transporter.laneQuotes().isEmpty())
                .toList();
        return new AssignmentProblem(input.lanes(), candidates, quoteMatrix(input.lanes(), candidates));
    }

    public int laneCount() {
        return lanes.size();
    }

    public int transporterCount() {
        return candidates.size();
    }

    long[][] quotes() {
        return quotes;
    }

    long[] noQuotes() {
        long[] none = new long[laneCount()];
        Arrays.fill(none, NO_QUOTE);
        return none;
    }

    AssignmentPlan toPlan(Solution solution, boolean optimal) {
        List<LaneAssignment> assignments = IntStream.range(0, laneCount())
                .mapToObj(lane -> new LaneAssignment(
                        lanes.get(lane).id(),
                        candidates.get(solution.winnerByLane()[lane]).id()))
                .toList();

        List<Long> selectedTransporters = assignments.stream()
                .map(LaneAssignment::transporterId)
                .distinct()
                .sorted()
                .toList();

        return new AssignmentPlan(solution.totalCost(), assignments, selectedTransporters, optimal);
    }

    static long[] cheaperOf(long[] current, long[] offer) {
        long[] cheaper = current.clone();
        for (int lane = 0; lane < cheaper.length; lane++) {
            cheaper[lane] = Math.min(cheaper[lane], offer[lane]);
        }
        return cheaper;
    }

    static boolean coversEveryLane(long[] cheapestQuote) {
        return Arrays.stream(cheapestQuote).noneMatch(quote -> quote == NO_QUOTE);
    }

    private static long[][] quoteMatrix(List<Lane> lanes, List<Transporter> transporters) {
        Map<Long, Integer> laneIndexById = new HashMap<>();
        for (int lane = 0; lane < lanes.size(); lane++) {
            laneIndexById.put(lanes.get(lane).id(), lane);
        }

        long[][] quotes = new long[transporters.size()][lanes.size()];
        for (int transporter = 0; transporter < transporters.size(); transporter++) {
            Arrays.fill(quotes[transporter], NO_QUOTE);
            for (LaneQuote laneQuote : transporters.get(transporter).laneQuotes()) {
                quotes[transporter][laneIndexById.get(laneQuote.laneId())] = laneQuote.quote();
            }
        }
        return quotes;
    }
}
