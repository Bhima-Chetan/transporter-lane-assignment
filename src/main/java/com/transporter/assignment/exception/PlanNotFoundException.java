package com.transporter.assignment.exception;

public class PlanNotFoundException extends RuntimeException {

    public PlanNotFoundException(int laneCount, int maxTransporters) {
        super(("Could not find a plan covering all %d lanes with at most %d transporters. The input is too large to "
                + "check every combination, so a higher maxTransporters may be needed.").formatted(laneCount, maxTransporters));
    }
}
