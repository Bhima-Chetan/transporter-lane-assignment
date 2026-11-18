package com.transporter.assignment.optimizer;

import java.util.Arrays;
import java.util.List;

public record Solution(long totalCost, int[] winnerByLane, int transportersUsed) {

    static Solution of(long[][] quotes, List<Integer> chosen, long[] cheapestQuote) {
        long totalCost = Arrays.stream(cheapestQuote).sum();
        int[] winnerByLane = LaneWinners.pickSpreadingTies(quotes, chosen, cheapestQuote);
        int transportersUsed = (int) Arrays.stream(winnerByLane).distinct().count();
        return new Solution(totalCost, winnerByLane, transportersUsed);
    }

    boolean isBetterThan(Solution other) {
        if (other == null || totalCost < other.totalCost) {
            return true;
        }
        return totalCost == other.totalCost && transportersUsed > other.transportersUsed;
    }
}
