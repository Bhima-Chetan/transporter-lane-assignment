package com.transporter.assignment.service;

import com.transporter.assignment.Samples;
import com.transporter.assignment.exception.InvalidInputException;
import com.transporter.assignment.model.Lane;
import com.transporter.assignment.model.LaneQuote;
import com.transporter.assignment.model.Transporter;
import com.transporter.assignment.model.TransporterInput;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class TransporterInputValidatorTest {

    private final TransporterInputValidator validator = new TransporterInputValidator();

    @Test
    void acceptsTheSampleInputs() {
        assertThatCode(() -> validator.validate(Samples.input(Samples.TEST_CASE_1))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(Samples.input(Samples.TEST_CASE_2))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(Samples.quoteSheet())).doesNotThrowAnyException();
    }

    @Test
    void rejectsDuplicateLaneIds() {
        TransporterInput input = new TransporterInput(
                List.of(lane(1), lane(1)),
                List.of(transporter(1, new LaneQuote(1L, 100L))));

        assertThat(problemsWith(input)).containsExactly("Lane id 1 appears 2 times");
    }

    @Test
    void rejectsDuplicateTransporterIds() {
        TransporterInput input = new TransporterInput(
                List.of(lane(1)),
                List.of(transporter(7, new LaneQuote(1L, 100L)), transporter(7, new LaneQuote(1L, 200L))));

        assertThat(problemsWith(input)).containsExactly("Transporter id 7 appears 2 times");
    }

    @Test
    void rejectsQuotesForLanesThatDoNotExist() {
        TransporterInput input = new TransporterInput(
                List.of(lane(1)),
                List.of(transporter(1, new LaneQuote(1L, 100L), new LaneQuote(9L, 100L))));

        assertThat(problemsWith(input)).containsExactly("Transporter 1 quotes lane 9, which does not exist");
    }

    @Test
    void rejectsATransporterQuotingTheSameLaneTwice() {
        TransporterInput input = new TransporterInput(
                List.of(lane(1)),
                List.of(transporter(1, new LaneQuote(1L, 100L), new LaneQuote(1L, 90L))));

        assertThat(problemsWith(input)).containsExactly("Transporter 1 quotes lane 1 more than once");
    }

    @Test
    void rejectsLanesThatNobodyQuoted() {
        TransporterInput input = new TransporterInput(
                List.of(lane(1), lane(2)),
                List.of(transporter(1, new LaneQuote(1L, 100L))));

        assertThat(problemsWith(input))
                .containsExactly("Lane 2 has no quotes from any transporter, so it can never be covered");
    }

    @Test
    void reportsEveryProblemInOneGo() {
        TransporterInput input = new TransporterInput(
                List.of(lane(1), lane(1), lane(2)),
                List.of(transporter(1, new LaneQuote(1L, 100L), new LaneQuote(5L, 100L))));

        assertThat(problemsWith(input)).containsExactlyInAnyOrder(
                "Lane id 1 appears 2 times",
                "Transporter 1 quotes lane 5, which does not exist",
                "Lane 2 has no quotes from any transporter, so it can never be covered");
    }

    private List<String> problemsWith(TransporterInput input) {
        return catchThrowableOfType(InvalidInputException.class, () -> validator.validate(input)).getProblems();
    }

    private static Lane lane(long id) {
        return new Lane(id, "Pune", "Mumbai");
    }

    private static Transporter transporter(long id, LaneQuote... quotes) {
        return new Transporter(id, "Transporter " + id, List.of(quotes));
    }
}
