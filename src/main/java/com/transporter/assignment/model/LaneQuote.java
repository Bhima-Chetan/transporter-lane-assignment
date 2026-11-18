package com.transporter.assignment.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LaneQuote(
        @NotNull @Positive @Schema(example = "1") Long laneId,
        @NotNull @Positive @Schema(example = "5000") Long quote) {
}
