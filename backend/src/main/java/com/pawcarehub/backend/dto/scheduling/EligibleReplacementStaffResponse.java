package com.pawcarehub.backend.dto.scheduling;

public record EligibleReplacementStaffResponse(
    Long staffId,
    String staffName,
    String role
) {
}
