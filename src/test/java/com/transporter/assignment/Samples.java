package com.transporter.assignment;

import com.transporter.assignment.model.TransporterInput;
import com.transporter.assignment.service.QuoteSheetParser;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public final class Samples {

    public static final String TEST_CASE_1 = "test-case-1.json";
    public static final String TEST_CASE_2 = "test-case-2.json";
    public static final String SPEC_EXAMPLE = "spec-example.json";
    public static final String QUOTE_SHEET = "transporter_assignment_data.csv";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private Samples() {
    }

    public static TransporterInput input(String fileName) {
        return JSON.readValue(read(fileName), TransporterInput.class);
    }

    public static TransporterInput quoteSheet() {
        return new QuoteSheetParser().parse(read(QUOTE_SHEET));
    }

    public static String read(String fileName) {
        try {
            return new ClassPathResource("samples/" + fileName).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
