package com.transporter.assignment.validation;

import com.transporter.assignment.model.LaneQuote;
import com.transporter.assignment.model.TransporterInput;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Order(3)
public class SingleQuotePerLaneRule implements InputRule {

    @Override
    public List<String> findProblems(TransporterInput input) {
        return input.transporters().stream()
                .flatMap(transporter -> transporter.laneQuotes().stream()
                        .collect(Collectors.groupingBy(LaneQuote::laneId, LinkedHashMap::new, Collectors.counting()))
                        .entrySet().stream()
                        .filter(quotesForLane -> quotesForLane.getValue() > 1)
                        .map(quotesForLane -> "Transporter %d quotes lane %d more than once"
                                .formatted(transporter.id(), quotesForLane.getKey())))
                .toList();
    }
}
