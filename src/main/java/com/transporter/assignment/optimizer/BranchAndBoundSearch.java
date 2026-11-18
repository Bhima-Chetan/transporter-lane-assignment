package com.transporter.assignment.optimizer;

import com.transporter.assignment.exception.SearchLimitExceededException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.transporter.assignment.optimizer.AssignmentProblem.NO_QUOTE;
import static com.transporter.assignment.optimizer.AssignmentProblem.cheaperOf;
import static com.transporter.assignment.optimizer.AssignmentProblem.coversEveryLane;

final class BranchAndBoundSearch {

    private final AssignmentProblem problem;
    private final long[][] quotes;
    private final int maxTransporters;
    private final long stepLimit;
    private final long[][] cheapestQuoteFrom;
    private final List<Integer> chosen = new ArrayList<>();
    private Solution best;
    private long steps;

    BranchAndBoundSearch(AssignmentProblem problem, int maxTransporters, long stepLimit) {
        this.problem = problem;
        this.quotes = problem.quotes();
        this.maxTransporters = maxTransporters;
        this.stepLimit = stepLimit;
        this.cheapestQuoteFrom = cheapestQuotesFromEachPosition();
    }

    Optional<Solution> run() {
        explore(0, problem.noQuotes());
        return Optional.ofNullable(best);
    }

    private void explore(int next, long[] cheapestChosenQuote) {
        if (++steps > stepLimit) {
            throw new SearchLimitExceededException(stepLimit);
        }
        if (cannotBeatBest(next, cheapestChosenQuote)) {
            return;
        }
        if (chosen.size() == maxTransporters || next == quotes.length) {
            consider(cheapestChosenQuote);
            return;
        }

        chosen.add(next);
        explore(next + 1, cheaperOf(cheapestChosenQuote, quotes[next]));
        chosen.removeLast();

        explore(next + 1, cheapestChosenQuote);
    }

    private boolean cannotBeatBest(int next, long[] cheapestChosenQuote) {
        long bestReachableCost = 0;
        for (int lane = 0; lane < problem.laneCount(); lane++) {
            long cheapestReachable = Math.min(cheapestChosenQuote[lane], cheapestQuoteFrom[next][lane]);
            if (cheapestReachable == NO_QUOTE) {
                return true;
            }
            bestReachableCost += cheapestReachable;
        }
        return best != null && bestReachableCost > best.totalCost();
    }

    private void consider(long[] cheapestChosenQuote) {
        if (!coversEveryLane(cheapestChosenQuote)) {
            return;
        }
        if (best != null && Arrays.stream(cheapestChosenQuote).sum() > best.totalCost()) {
            return;
        }

        Solution candidate = Solution.of(quotes, chosen, cheapestChosenQuote);
        if (candidate.isBetterThan(best)) {
            best = candidate;
        }
    }

    private long[][] cheapestQuotesFromEachPosition() {
        long[][] cheapest = new long[quotes.length + 1][];
        cheapest[quotes.length] = problem.noQuotes();
        for (int position = quotes.length - 1; position >= 0; position--) {
            cheapest[position] = cheaperOf(cheapest[position + 1], quotes[position]);
        }
        return cheapest;
    }
}
