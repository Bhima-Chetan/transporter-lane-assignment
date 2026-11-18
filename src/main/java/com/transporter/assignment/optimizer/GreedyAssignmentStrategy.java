package com.transporter.assignment.optimizer;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.transporter.assignment.optimizer.AssignmentProblem.NO_QUOTE;
import static com.transporter.assignment.optimizer.AssignmentProblem.cheaperOf;
import static com.transporter.assignment.optimizer.AssignmentProblem.coversEveryLane;

@Component
public class GreedyAssignmentStrategy implements AssignmentStrategy {

    private static final int NONE = -1;

    @Override
    public Optional<Solution> solve(AssignmentProblem problem, int maxTransporters) {
        long[][] quotes = problem.quotes();
        List<Integer> chosen = new ArrayList<>();
        long[] cheapestChosenQuote = problem.noQuotes();

        while (chosen.size() < maxTransporters) {
            int bestAddition = NONE;
            Coverage bestCoverage = Coverage.of(cheapestChosenQuote);
            for (int candidate = 0; candidate < quotes.length; candidate++) {
                if (chosen.contains(candidate)) {
                    continue;
                }
                Coverage withCandidate = Coverage.of(cheaperOf(cheapestChosenQuote, quotes[candidate]));
                if (withCandidate.isBetterThan(bestCoverage)) {
                    bestAddition = candidate;
                    bestCoverage = withCandidate;
                }
            }
            if (bestAddition == NONE) {
                break;
            }
            chosen.add(bestAddition);
            cheapestChosenQuote = cheaperOf(cheapestChosenQuote, quotes[bestAddition]);
        }

        if (!coversEveryLane(cheapestChosenQuote)) {
            return Optional.empty();
        }
        return Optional.of(Solution.of(quotes, chosen, cheapestChosenQuote));
    }

    @Override
    public boolean guaranteesOptimal() {
        return false;
    }

    private record Coverage(int uncoveredLanes, long costOfCoveredLanes) {

        static Coverage of(long[] cheapestQuote) {
            int uncovered = 0;
            long cost = 0;
            for (long quote : cheapestQuote) {
                if (quote == NO_QUOTE) {
                    uncovered++;
                } else {
                    cost += quote;
                }
            }
            return new Coverage(uncovered, cost);
        }

        boolean isBetterThan(Coverage other) {
            if (uncoveredLanes != other.uncoveredLanes) {
                return uncoveredLanes < other.uncoveredLanes;
            }
            return costOfCoveredLanes < other.costOfCoveredLanes;
        }
    }
}
