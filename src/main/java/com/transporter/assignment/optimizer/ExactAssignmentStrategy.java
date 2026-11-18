package com.transporter.assignment.optimizer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ExactAssignmentStrategy implements AssignmentStrategy {

    private final long searchStepLimit;

    public ExactAssignmentStrategy(@Value("${assignment.search-step-limit}") long searchStepLimit) {
        this.searchStepLimit = searchStepLimit;
    }

    @Override
    public Optional<Solution> solve(AssignmentProblem problem, int maxTransporters) {
        return new BranchAndBoundSearch(problem, maxTransporters, searchStepLimit).run();
    }

    @Override
    public boolean guaranteesOptimal() {
        return true;
    }
}
