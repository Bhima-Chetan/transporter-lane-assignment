package com.transporter.assignment.exception;

import java.util.List;

public class InvalidInputException extends RuntimeException {

    private final List<String> problems;

    public InvalidInputException(List<String> problems) {
        super("Input data is not valid");
        this.problems = List.copyOf(problems);
    }

    public InvalidInputException(String problem) {
        this(List.of(problem));
    }

    public List<String> getProblems() {
        return problems;
    }
}
