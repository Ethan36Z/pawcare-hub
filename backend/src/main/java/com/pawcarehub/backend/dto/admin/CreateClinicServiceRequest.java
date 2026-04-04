package com.pawcarehub.backend.dto.admin;

public record CreateClinicServiceRequest(
    String name,
    String category,
    String description,
    String duration,
    String price,
    Boolean active
) {
}
