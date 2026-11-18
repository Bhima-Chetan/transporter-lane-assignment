package com.transporter.assignment.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record Lane(
        @NotNull @Positive @Schema(example = "1") Long id,
        @NotBlank @Schema(example = "Mumbai") String origin,
        @NotBlank @Schema(example = "Delhi") String destination) {
}
