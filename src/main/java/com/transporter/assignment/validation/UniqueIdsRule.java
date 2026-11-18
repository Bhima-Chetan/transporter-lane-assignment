package com.transporter.assignment.validation;

import com.transporter.assignment.model.Lane;
import com.transporter.assignment.model.Transporter;
import com.transporter.assignment.model.TransporterInput;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Order(1)
public class UniqueIdsRule implements InputRule {

    @Override
    public List<String> findProblems(TransporterInput input) {
        return Stream.concat(
                        duplicates(input.lanes(), Lane::id, "Lane"),
                        duplicates(input.transporters(), Transporter::id, "Transporter"))
                .toList();
    }

    private static <T> Stream<String> duplicates(List<T> items, Function<T, Long> idOf, String label) {
        return items.stream()
                .collect(Collectors.groupingBy(idOf, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .filter(occurrences -> occurrences.getValue() > 1)
                .map(occurrences -> "%s id %d appears %d times"
                        .formatted(label, occurrences.getKey(), occurrences.getValue()));
    }
}
