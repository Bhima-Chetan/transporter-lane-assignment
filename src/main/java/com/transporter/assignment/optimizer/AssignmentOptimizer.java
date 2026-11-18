package com.transporter.assignment.optimizer;

import com.transporter.assignment.exception.InfeasibleAssignmentException;
import com.transporter.assignment.model.AssignmentPlan;
import com.transporter.assignment.model.Lane;
import com.transporter.assignment.model.LaneAssignment;
import com.transporter.assignment.model.LaneQuote;
import com.transporter.assignment.model.Transporter;
import com.transporter.assignment.model.TransporterInput;
import com.transporter.assignment.optimizer.BranchAndBoundSearch.Solution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static com.transporter.assignment.optimizer.BranchAndBoundSearch.NO_QUOTE;

@Component
public class AssignmentOptimizer {

    private final long searchStepLimit;

    public AssignmentOptimizer(@Value("${assignment.search-step-limit}") long searchStepLimit) {
        this.searchStepLimit = searchStepLimit;
    }

    public AssignmentPlan optimize(TransporterInput input, int maxTransporters) {
        List<Lane> lanes = input.lanes();
        List<Transporter> candidates = input.transporters().stream()
                .filter(transporter -> !transporter.laneQuotes().isEmpty())
                .toList();
        long[][] quotes = quoteMatrix(lanes, candidates);
        int usableTransporters = Math.min(maxTransporters, candidates.size());

        Solution best = cheapestCover(quotes, lanes.size(), usableTransporters)
                .orElseThrow(() -> new InfeasibleAssignmentException(
                        lanes.size(),
                        maxTransporters,
                        fewestTransportersCoveringAllLanes(quotes, lanes.size(), usableTransporters + 1)));

        return toPlan(best, lanes, candidates);
    }

    private Optional<Solution> cheapestCover(long[][] quotes, int laneCount, int maxTransporters) {
        return new BranchAndBoundSearch(quotes, laneCount, maxTransporters, searchStepLimit).run();
    }

    private int fewestTransportersCoveringAllLanes(long[][] quotes, int laneCount, int startingFrom) {
        return IntStream.rangeClosed(startingFrom, quotes.length)
                .filter(count -> cheapestCover(quotes, laneCount, count).isPresent())
                .findFirst()
                .orElseThrow();
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

    private static AssignmentPlan toPlan(Solution solution, List<Lane> lanes, List<Transporter> candidates) {
        List<LaneAssignment> assignments = IntStream.range(0, lanes.size())
                .mapToObj(lane -> new LaneAssignment(
                        lanes.get(lane).id(),
                        candidates.get(solution.winnerByLane()[lane]).id()))
                .toList();

        List<Long> selectedTransporters = assignments.stream()
                .map(LaneAssignment::transporterId)
                .distinct()
                .sorted()
                .toList();

        return new AssignmentPlan(solution.totalCost(), assignments, selectedTransporters);
    }
}
