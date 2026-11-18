package com.transporter.assignment.service;

import com.transporter.assignment.Samples;
import com.transporter.assignment.TestComponents;
import com.transporter.assignment.exception.InputNotFoundException;
import com.transporter.assignment.exception.InvalidInputException;
import com.transporter.assignment.model.Lane;
import com.transporter.assignment.model.Transporter;
import com.transporter.assignment.model.TransporterInput;
import com.transporter.assignment.repository.InMemoryTransporterInputRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransporterServiceTest {

    private final TransporterService service = new TransporterService(
            new InMemoryTransporterInputRepository(),
            TestComponents.validator(),
            new QuoteSheetParser(),
            TestComponents.optimizer());

    @Test
    void asksForInputBeforeAnythingHasBeenSubmitted() {
        assertThatThrownBy(() -> service.findOptimalAssignment(3))
                .isInstanceOf(InputNotFoundException.class)
                .hasMessageContaining("POST /api/v1/transporters/input");
    }

    @Test
    void optimisesTheMostRecentlySubmittedInput() {
        service.saveInput(Samples.input(Samples.TEST_CASE_1));
        service.saveInput(Samples.input(Samples.TEST_CASE_2));

        assertThat(service.findOptimalAssignment(3).totalCost()).isEqualTo(7500);
    }

    @Test
    void keepsThePreviousInputWhenNewInputIsRejected() {
        service.saveInput(Samples.input(Samples.TEST_CASE_1));
        TransporterInput unquotedLane = new TransporterInput(
                List.of(new Lane(1L, "Pune", "Nashik")),
                List.of(new Transporter(1L, "Transporter X", List.of())));

        assertThatThrownBy(() -> service.saveInput(unquotedLane)).isInstanceOf(InvalidInputException.class);
        assertThat(service.findOptimalAssignment(3).totalCost()).isEqualTo(60139);
    }

    @Test
    void importsTheQuoteSheet() {
        TransporterInput imported = service.importQuoteSheet(Samples.read(Samples.QUOTE_SHEET));

        assertThat(imported.lanes()).hasSize(9);
        assertThat(service.findOptimalAssignment(3).totalCost()).isEqualTo(134876);
    }
}
