package com.transporter.assignment.model;

import java.util.List;

public record AssignmentPlan(long totalCost, List<LaneAssignment> assignments, List<Long> selectedTransporters) {
}
