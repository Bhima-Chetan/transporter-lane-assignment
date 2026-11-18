package com.transporter.assignment.validation;

import com.transporter.assignment.model.Lane;
import com.transporter.assignment.model.LaneQuote;
import com.transporter.assignment.model.TransporterInput;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(4)
public class EveryLaneQuotedRule implements InputRule {

    @Override
    public List<String> findProblems(TransporterInput input) {
        Set<Long> quotedLaneIds = input.transporters().stream()
                .flatMap(transporter -> transporter.laneQuotes().stream())
                .map(LaneQuote::laneId)
                .collect(Collectors.toSet());

        return input.lanes().stream()
                .map(Lane::id)
                .distinct()
                .filter(laneId -> !quotedLaneIds.contains(laneId))
                .map(laneId -> "Lane %d has no quotes from any transporter, so it can never be covered".formatted(laneId))
                .toList();
    }
}
