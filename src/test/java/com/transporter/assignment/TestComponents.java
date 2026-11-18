package com.transporter.assignment;

import com.transporter.assignment.optimizer.AssignmentOptimizer;
import com.transporter.assignment.optimizer.ExactAssignmentStrategy;
import com.transporter.assignment.optimizer.GreedyAssignmentStrategy;
import com.transporter.assignment.validation.EveryLaneQuotedRule;
import com.transporter.assignment.validation.KnownLanesRule;
import com.transporter.assignment.validation.SingleQuotePerLaneRule;
import com.transporter.assignment.validation.TransporterInputValidator;
import com.transporter.assignment.validation.UniqueIdsRule;

import java.util.List;

public final class TestComponents {

    public static final long DEFAULT_STEP_LIMIT = 20_000_000;

    private TestComponents() {
    }

    public static AssignmentOptimizer optimizer() {
        return optimizer(DEFAULT_STEP_LIMIT);
    }

    public static AssignmentOptimizer optimizer(long searchStepLimit) {
        return new AssignmentOptimizer(new ExactAssignmentStrategy(searchStepLimit), new GreedyAssignmentStrategy());
    }

    public static TransporterInputValidator validator() {
        return new TransporterInputValidator(List.of(
                new UniqueIdsRule(),
                new KnownLanesRule(),
                new SingleQuotePerLaneRule(),
                new EveryLaneQuotedRule()));
    }
}
