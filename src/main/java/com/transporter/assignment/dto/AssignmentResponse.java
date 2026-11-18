package com.transporter.assignment.dto;

import com.transporter.assignment.model.AssignmentPlan;
import com.transporter.assignment.model.LaneAssignment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record AssignmentResponse(
        @Schema(example = "success") String status,
        @Schema(example = "60139") long totalCost,
        List<LaneAssignment> assignments,
        @Schema(example = "[1, 4, 5]") List<Long> selectedTransporters) {

    public static AssignmentResponse success(AssignmentPlan plan) {
        return new AssignmentResponse("success", plan.totalCost(), plan.assignments(), plan.selectedTransporters());
    }
}
