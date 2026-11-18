package com.transporter.assignment.exception;

public class SearchLimitExceededException extends RuntimeException {

    public SearchLimitExceededException(long searchStepLimit) {
        super("Exact search stopped after %,d steps".formatted(searchStepLimit));
    }
}
