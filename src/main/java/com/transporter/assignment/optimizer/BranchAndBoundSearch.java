package com.transporter.assignment.optimizer;

import com.transporter.assignment.exception.SearchLimitExceededException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

final class BranchAndBoundSearch {

    static final long NO_QUOTE = Long.MAX_VALUE;

    private final long[][] quotes;
    private final int laneCount;
    private final int maxTransporters;
    private final long stepLimit;
    private final long[][] cheapestQuoteFrom;
    private final List<Integer> chosen = new ArrayList<>();
    private Solution best;
    private long steps;

    BranchAndBoundSearch(long[][] quotes, int laneCount, int maxTransporters, long stepLimit) {
        this.quotes = quotes;
        this.laneCount = laneCount;
        this.maxTransporters = maxTransporters;
        this.stepLimit = stepLimit;
        this.cheapestQuoteFrom = cheapestQuotesFromEachPosition();
    }

    Optional<Solution> run() {
        explore(0, noQuotes());
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
        for (int lane = 0; lane < laneCount; lane++) {
            long cheapestReachable = Math.min(cheapestChosenQuote[lane], cheapestQuoteFrom[next][lane]);
            if (cheapestReachable == NO_QUOTE) {
                return true;
            }
            bestReachableCost += cheapestReachable;
        }
        return best != null && bestReachableCost > best.totalCost();
    }

    private void consider(long[] cheapestChosenQuote) {
        long totalCost = 0;
        for (long quote : cheapestChosenQuote) {
            if (quote == NO_QUOTE) {
                return;
            }
            totalCost += quote;
        }
        if (best != null && totalCost > best.totalCost()) {
            return;
        }

        int[] winnerByLane = LaneWinners.pickSpreadingTies(quotes, chosen, cheapestChosenQuote);
        int transportersUsed = (int) Arrays.stream(winnerByLane).distinct().count();
        if (isBetterThanBest(totalCost, transportersUsed)) {
            best = new Solution(totalCost, winnerByLane, transportersUsed);
        }
    }

    private boolean isBetterThanBest(long totalCost, int transportersUsed) {
        if (best == null || totalCost < best.totalCost()) {
            return true;
        }
        return totalCost == best.totalCost() && transportersUsed > best.transportersUsed();
    }

    private long[][] cheapestQuotesFromEachPosition() {
        long[][] cheapest = new long[quotes.length + 1][];
        cheapest[quotes.length] = noQuotes();
        for (int position = quotes.length - 1; position >= 0; position--) {
            cheapest[position] = cheaperOf(cheapest[position + 1], quotes[position]);
        }
        return cheapest;
    }

    private long[] noQuotes() {
        long[] none = new long[laneCount];
        Arrays.fill(none, NO_QUOTE);
        return none;
    }

    private static long[] cheaperOf(long[] current, long[] offer) {
        long[] cheaper = current.clone();
        for (int lane = 0; lane < cheaper.length; lane++) {
            cheaper[lane] = Math.min(cheaper[lane], offer[lane]);
        }
        return cheaper;
    }

    record Solution(long totalCost, int[] winnerByLane, int transportersUsed) {
    }
}
