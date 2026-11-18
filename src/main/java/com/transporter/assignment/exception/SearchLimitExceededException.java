package com.transporter.assignment.exception;

public class SearchLimitExceededException extends RuntimeException {

    public SearchLimitExceededException(long searchStepLimit) {
        super("This input is too large to solve exactly within %,d search steps. Try a smaller maxTransporters or fewer transporters."
                .formatted(searchStepLimit));
    }
}
