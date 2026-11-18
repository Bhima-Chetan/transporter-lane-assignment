package com.transporter.assignment.service;

import com.transporter.assignment.exception.InvalidInputException;
import com.transporter.assignment.model.Lane;
import com.transporter.assignment.model.LaneQuote;
import com.transporter.assignment.model.Transporter;
import com.transporter.assignment.model.TransporterInput;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TransporterInputValidator {

    public void validate(TransporterInput input) {
        List<String> problems = new ArrayList<>();

        Set<Long> laneIds = idsReportingDuplicates(input.lanes(), Lane::id, "Lane", problems);
        idsReportingDuplicates(input.transporters(), Transporter::id, "Transporter", problems);

        Set<Long> quotedLaneIds = new HashSet<>();
        for (Transporter transporter : input.transporters()) {
            Set<Long> lanesQuotedByTransporter = new HashSet<>();
            for (LaneQuote laneQuote : transporter.laneQuotes()) {
                if (!laneIds.contains(laneQuote.laneId())) {
                    problems.add("Transporter %d quotes lane %d, which does not exist"
                            .formatted(transporter.id(), laneQuote.laneId()));
                } else if (!lanesQuotedByTransporter.add(laneQuote.laneId())) {
                    problems.add("Transporter %d quotes lane %d more than once"
                            .formatted(transporter.id(), laneQuote.laneId()));
                }
                quotedLaneIds.add(laneQuote.laneId());
            }
        }

        laneIds.stream()
                .filter(laneId -> !quotedLaneIds.contains(laneId))
                .forEach(laneId -> problems.add(
                        "Lane %d has no quotes from any transporter, so it can never be covered".formatted(laneId)));

        if (!problems.isEmpty()) {
            throw new InvalidInputException(problems);
        }
    }

    private static <T> Set<Long> idsReportingDuplicates(List<T> items, Function<T, Long> idOf, String label,
                                                        List<String> problems) {
        Map<Long, Long> occurrencesById = items.stream()
                .collect(Collectors.groupingBy(idOf, LinkedHashMap::new, Collectors.counting()));

        occurrencesById.forEach((id, occurrences) -> {
            if (occurrences > 1) {
                problems.add("%s id %d appears %d times".formatted(label, id, occurrences));
            }
        });
        return occurrencesById.keySet();
    }
}
