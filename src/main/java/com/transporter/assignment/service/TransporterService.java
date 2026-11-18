package com.transporter.assignment.service;

import com.transporter.assignment.exception.InputNotFoundException;
import com.transporter.assignment.model.AssignmentPlan;
import com.transporter.assignment.model.TransporterInput;
import com.transporter.assignment.optimizer.AssignmentOptimizer;
import com.transporter.assignment.repository.TransporterInputRepository;
import com.transporter.assignment.validation.TransporterInputValidator;
import org.springframework.stereotype.Service;

@Service
public class TransporterService {

    private final TransporterInputRepository repository;
    private final TransporterInputValidator validator;
    private final QuoteSheetParser quoteSheetParser;
    private final AssignmentOptimizer optimizer;

    public TransporterService(TransporterInputRepository repository, TransporterInputValidator validator,
                              QuoteSheetParser quoteSheetParser, AssignmentOptimizer optimizer) {
        this.repository = repository;
        this.validator = validator;
        this.quoteSheetParser = quoteSheetParser;
        this.optimizer = optimizer;
    }

    public void saveInput(TransporterInput input) {
        validator.validate(input);
        repository.save(input);
    }

    public TransporterInput importQuoteSheet(String csv) {
        TransporterInput input = quoteSheetParser.parse(csv);
        saveInput(input);
        return input;
    }

    public AssignmentPlan findOptimalAssignment(int maxTransporters) {
        TransporterInput input = repository.findCurrent().orElseThrow(InputNotFoundException::new);
        return optimizer.optimize(input, maxTransporters);
    }
}
