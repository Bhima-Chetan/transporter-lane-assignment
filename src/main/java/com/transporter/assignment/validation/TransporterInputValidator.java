package com.transporter.assignment.validation;

import com.transporter.assignment.exception.InvalidInputException;
import com.transporter.assignment.model.TransporterInput;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransporterInputValidator {

    private final List<InputRule> rules;

    public TransporterInputValidator(List<InputRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public void validate(TransporterInput input) {
        List<String> problems = rules.stream()
                .flatMap(rule -> rule.findProblems(input).stream())
                .toList();

        if (!problems.isEmpty()) {
            throw new InvalidInputException(problems);
        }
    }
}
