package com.transporter.assignment.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record Transporter(
        @NotNull @Positive @Schema(example = "1") Long id,
        @NotBlank @Schema(example = "Transporter T1") String name,
        @NotNull List<@Valid @NotNull LaneQuote> laneQuotes) {
}
