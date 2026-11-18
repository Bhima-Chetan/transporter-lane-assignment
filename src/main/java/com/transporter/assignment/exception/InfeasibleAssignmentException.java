package com.transporter.assignment.exception;

public class InfeasibleAssignmentException extends RuntimeException {

    private final int minimumTransportersRequired;

    public InfeasibleAssignmentException(int laneCount, int maxTransporters, int minimumTransportersRequired) {
        super("Covering all %d lanes needs at least %d transporters, but maxTransporters is %d."
                .formatted(laneCount, minimumTransportersRequired, maxTransporters));
        this.minimumTransportersRequired = minimumTransportersRequired;
    }

    public int getMinimumTransportersRequired() {
        return minimumTransportersRequired;
    }
}
