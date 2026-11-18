package com.transporter.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MessageResponse(
        @Schema(example = "success") String status,
        @Schema(example = "Input data saved successfully.") String message) {

    public static MessageResponse success(String message) {
        return new MessageResponse("success", message);
    }
}
