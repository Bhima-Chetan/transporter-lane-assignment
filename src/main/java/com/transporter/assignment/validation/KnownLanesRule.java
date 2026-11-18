package com.transporter.assignment.validation;

import com.transporter.assignment.model.Lane;
import com.transporter.assignment.model.TransporterInput;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(2)
public class KnownLanesRule implements InputRule {

    @Override
    public List<String> findProblems(TransporterInput input) {
        Set<Long> laneIds = input.lanes().stream().map(Lane::id).collect(Collectors.toSet());

        return input.transporters().stream()
                .flatMap(transporter -> transporter.laneQuotes().stream()
                        .filter(quote -> !laneIds.contains(quote.laneId()))
                        .map(quote -> "Transporter %d quotes lane %d, which does not exist"
                                .formatted(transporter.id(), quote.laneId())))
                .toList();
    }
}
