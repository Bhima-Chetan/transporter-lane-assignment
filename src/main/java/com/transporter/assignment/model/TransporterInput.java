package com.transporter.assignment.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TransporterInput(
        @NotEmpty List<@Valid @NotNull Lane> lanes,
        @NotEmpty List<@Valid @NotNull Transporter> transporters) {
}
