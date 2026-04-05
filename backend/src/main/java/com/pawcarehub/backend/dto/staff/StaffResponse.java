package com.pawcarehub.backend.dto.staff;

public record StaffResponse(
    Long id,
    String name,
    String role,
    boolean active
) {
}
