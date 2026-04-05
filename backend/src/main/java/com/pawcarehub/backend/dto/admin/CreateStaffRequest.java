package com.pawcarehub.backend.dto.admin;

public record CreateStaffRequest(
    String name,
    String role,
    Boolean active
) {
}
