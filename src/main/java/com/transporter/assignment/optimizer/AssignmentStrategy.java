package com.transporter.assignment.optimizer;

import java.util.Optional;

public interface AssignmentStrategy {

    Optional<Solution> solve(AssignmentProblem problem, int maxTransporters);

    boolean guaranteesOptimal();
}
