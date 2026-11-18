package com.transporter.assignment.controller;

import com.transporter.assignment.dto.AssignmentRequest;
import com.transporter.assignment.dto.AssignmentResponse;
import com.transporter.assignment.dto.ErrorResponse;
import com.transporter.assignment.dto.MessageResponse;
import com.transporter.assignment.model.TransporterInput;
import com.transporter.assignment.service.TransporterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/transporters")
@Tag(name = "Transporter assignment")
public class TransporterController {

    private final TransporterService transporterService;

    public TransporterController(TransporterService transporterService) {
        this.transporterService = transporterService;
    }

    @Operation(
            summary = "Submit lanes and transporter quotes",
            description = "Replaces whatever was submitted before. Every lane must be quoted by at least one transporter. "
                    + "Pick one of the samples from the Examples dropdown to try it out.")
    @ApiResponse(responseCode = "200", description = "Input saved")
    @ApiResponse(responseCode = "400", description = "Malformed JSON, missing fields, duplicate ids, quotes for unknown lanes or lanes nobody quoted",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(path = "/input", consumes = MediaType.APPLICATION_JSON_VALUE)
    public MessageResponse submitInput(@Valid @RequestBody TransporterInput input) {
        transporterService.saveInput(input);
        return MessageResponse.success("Input data saved successfully.");
    }

    @Operation(
            summary = "Submit lanes and transporter quotes as a CSV sheet",
            description = "Same as /input, but takes the quote sheet format shared with the assignment: a header row "
                    + "'Trade Lane,T1,T2,...' followed by one row per lane with a quote under each transporter. "
                    + "Lanes and transporters get ids from their position, so 'Lane 1' is lane 1 and column T1 is transporter 1. "
                    + "Leave a cell empty if that transporter did not quote the lane.")
    @ApiResponse(responseCode = "200", description = "Input saved")
    @ApiResponse(responseCode = "400", description = "Missing file, wrong column count, non-numeric quotes or lanes nobody quoted",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(path = "/input/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MessageResponse submitQuoteSheet(@RequestPart("file") MultipartFile file) throws IOException {
        TransporterInput input = transporterService.importQuoteSheet(new String(file.getBytes(), StandardCharsets.UTF_8));
        return MessageResponse.success("Input data saved successfully. Loaded %d lanes and %d transporters."
                .formatted(input.lanes().size(), input.transporters().size()));
    }

    @Operation(
            summary = "Get the cheapest assignment using at most maxTransporters",
            description = "Covers every lane at the lowest total cost while using no more than maxTransporters. "
                    + "When two plans cost the same, the one that uses more transporters wins. "
                    + "selectedTransporters only lists transporters that actually won a lane, so it can be shorter than "
                    + "maxTransporters when adding another transporter would not lower the cost. "
                    + "The exact search is used first; if the input is too large for it, a greedy strategy answers "
                    + "instead and optimal comes back false.")
    @ApiResponse(responseCode = "200", description = "Optimal assignment found")
    @ApiResponse(responseCode = "400", description = "maxTransporters missing or not a positive number",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No input has been submitted yet",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "Every lane cannot be covered with that many transporters; the message says how many are needed when that can be proven",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(path = "/assignment", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AssignmentResponse findAssignment(@Valid @RequestBody AssignmentRequest request) {
        return AssignmentResponse.success(transporterService.findOptimalAssignment(request.maxTransporters()));
    }
}
