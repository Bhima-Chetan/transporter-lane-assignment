package com.transporter.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        @Schema(example = "error") String status,
        @Schema(example = "Input data is not valid") String message,
        @Schema(example = "[\"Lane 6 has no quotes from any transporter, so it can never be covered\"]") List<String> details) {

    public static ErrorResponse of(String message) {
        return of(message, List.of());
    }

    public static ErrorResponse of(String message, List<String> details) {
        return new ErrorResponse("error", message, details);
    }
}
