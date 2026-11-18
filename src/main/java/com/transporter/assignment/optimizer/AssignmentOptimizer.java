package com.transporter.assignment.optimizer;

import com.transporter.assignment.exception.InfeasibleAssignmentException;
import com.transporter.assignment.exception.PlanNotFoundException;
import com.transporter.assignment.exception.SearchLimitExceededException;
import com.transporter.assignment.model.AssignmentPlan;
import com.transporter.assignment.model.TransporterInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.stream.IntStream;

@Component
public class AssignmentOptimizer {

    private static final Logger log = LoggerFactory.getLogger(AssignmentOptimizer.class);

    private final AssignmentStrategy exact;
    private final AssignmentStrategy fallback;

    public AssignmentOptimizer(ExactAssignmentStrategy exact, GreedyAssignmentStrategy fallback) {
        this.exact = exact;
        this.fallback = fallback;
    }

    public AssignmentPlan optimize(TransporterInput input, int maxTransporters) {
        AssignmentProblem problem = AssignmentProblem.from(input);
        int usableTransporters = Math.min(maxTransporters, problem.transporterCount());
        try {
            return solveExactly(problem, usableTransporters, maxTransporters);
        } catch (SearchLimitExceededException tooLargeToSolveExactly) {
            log.info("{} lanes and {} transporters are too many to solve exactly, using the greedy strategy",
                    problem.laneCount(), problem.transporterCount());
            return solveWithFallback(problem, usableTransporters, maxTransporters);
        }
    }

    private AssignmentPlan solveExactly(AssignmentProblem problem, int usableTransporters, int maxTransporters) {
        return exact.solve(problem, usableTransporters)
                .map(solution -> problem.toPlan(solution, exact.guaranteesOptimal()))
                .orElseThrow(() -> new InfeasibleAssignmentException(
                        problem.laneCount(),
                        maxTransporters,
                        fewestTransportersCoveringAllLanes(problem, usableTransporters + 1)));
    }

    private AssignmentPlan solveWithFallback(AssignmentProblem problem, int usableTransporters, int maxTransporters) {
        return fallback.solve(problem, usableTransporters)
                .map(solution -> problem.toPlan(solution, fallback.guaranteesOptimal()))
                .orElseThrow(() -> new PlanNotFoundException(problem.laneCount(), maxTransporters));
    }

    private int fewestTransportersCoveringAllLanes(AssignmentProblem problem, int startingFrom) {
        return IntStream.rangeClosed(startingFrom, problem.transporterCount())
                .filter(count -> exact.solve(problem, count).isPresent())
                .findFirst()
                .orElseThrow();
    }
}
