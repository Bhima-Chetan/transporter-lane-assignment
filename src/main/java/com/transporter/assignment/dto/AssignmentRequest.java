package com.transporter.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignmentRequest(@NotNull @Positive @Schema(example = "3") Integer maxTransporters) {
}
