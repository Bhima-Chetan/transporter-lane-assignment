package com.transporter.assignment.service;

import com.transporter.assignment.Samples;
import com.transporter.assignment.exception.InvalidInputException;
import com.transporter.assignment.model.Lane;
import com.transporter.assignment.model.LaneQuote;
import com.transporter.assignment.model.Transporter;
import com.transporter.assignment.model.TransporterInput;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuoteSheetParserTest {

    private final QuoteSheetParser parser = new QuoteSheetParser();

    @Test
    void readsTheSharedQuoteSheet() {
        TransporterInput input = parser.parse(Samples.read(Samples.QUOTE_SHEET));

        assertThat(input.lanes()).extracting(Lane::id).containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
        assertThat(input.transporters()).extracting(Transporter::name)
                .containsExactly("T1", "T2", "T3", "T4", "T5", "T6", "T7");
        assertThat(input.transporters().getFirst().laneQuotes()).hasSize(9)
                .first().isEqualTo(new LaneQuote(1L, 20835L));
        assertThat(input.transporters().getLast().laneQuotes())
                .last().isEqualTo(new LaneQuote(9L, 31286L));
    }

    @Test
    void toleratesByteOrderMarkWindowsLineEndingsAndBlankLines() {
        String csv = "﻿Trade Lane,T1,T2\r\n\r\nLane 1,100,200\r\nLane 2,300,400\r\n\r\n";

        TransporterInput input = parser.parse(csv);

        assertThat(input.lanes()).hasSize(2);
        assertThat(input.transporters()).extracting(Transporter::name).containsExactly("T1", "T2");
    }

    @Test
    void treatsAnEmptyCellAsNoQuote() {
        TransporterInput input = parser.parse("""
                Trade Lane,T1,T2
                Lane 1,100,
                Lane 2, ,400
                """);

        assertThat(input.transporters().get(0).laneQuotes()).containsExactly(new LaneQuote(1L, 100L));
        assertThat(input.transporters().get(1).laneQuotes()).containsExactly(new LaneQuote(2L, 400L));
    }

    @Test
    void rejectsQuotesThatAreNotPositiveWholeNumbers() {
        assertThatThrownBy(() -> parser.parse("""
                Trade Lane,T1,T2
                Lane 1,100,abc
                """))
                .isInstanceOfSatisfying(InvalidInputException.class, ex -> assertThat(ex.getProblems())
                        .containsExactly("Row 2, column T2: 'abc' is not a positive whole number"));

        assertThatThrownBy(() -> parser.parse("""
                Trade Lane,T1
                Lane 1,0
                """))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void rejectsRowsWithTheWrongNumberOfColumns() {
        assertThatThrownBy(() -> parser.parse("""
                Trade Lane,T1,T2
                Lane 1,100
                """))
                .isInstanceOfSatisfying(InvalidInputException.class, ex -> assertThat(ex.getProblems())
                        .containsExactly("Row 2 has 2 columns but the header has 3"));
    }

    @Test
    void rejectsASheetWithoutLaneRows() {
        assertThatThrownBy(() -> parser.parse("Trade Lane,T1,T2"))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void rejectsAHeaderWithoutTransporterColumns() {
        assertThatThrownBy(() -> parser.parse("""
                Trade Lane
                Lane 1
                """))
                .isInstanceOf(InvalidInputException.class);
    }
}
