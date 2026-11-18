package com.transporter.assignment.optimizer;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

final class LaneWinners {

    private static final int NOBODY = -1;

    private final long[][] quotes;
    private final List<Integer> chosen;
    private final long[] cheapestQuote;
    private final int[] winnerByLane;

    private LaneWinners(long[][] quotes, List<Integer> chosen, long[] cheapestQuote) {
        this.quotes = quotes;
        this.chosen = chosen;
        this.cheapestQuote = cheapestQuote;
        this.winnerByLane = new int[cheapestQuote.length];
        Arrays.fill(winnerByLane, NOBODY);
    }

    static int[] pickSpreadingTies(long[][] quotes, List<Integer> chosen, long[] cheapestQuote) {
        return new LaneWinners(quotes, chosen, cheapestQuote).pick();
    }

    private int[] pick() {
        for (int transporter : chosen) {
            findLaneFor(transporter, new boolean[winnerByLane.length]);
        }
        for (int lane = 0; lane < winnerByLane.length; lane++) {
            if (winnerByLane[lane] == NOBODY) {
                winnerByLane[lane] = anyCheapestTransporterFor(lane);
            }
        }
        return winnerByLane;
    }

    private boolean findLaneFor(int transporter, boolean[] lanesTried) {
        for (int lane = 0; lane < winnerByLane.length; lane++) {
            if (lanesTried[lane] || quotes[transporter][lane] != cheapestQuote[lane]) {
                continue;
            }
            lanesTried[lane] = true;
            int currentWinner = winnerByLane[lane];
            if (currentWinner == NOBODY || findLaneFor(currentWinner, lanesTried)) {
                winnerByLane[lane] = transporter;
                return true;
            }
        }
        return false;
    }

    private int anyCheapestTransporterFor(int lane) {
        return chosen.stream()
                .min(Comparator.comparingLong(transporter -> quotes[transporter][lane]))
                .orElseThrow();
    }
}
