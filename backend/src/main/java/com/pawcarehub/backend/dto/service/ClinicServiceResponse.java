package com.pawcarehub.backend.dto.service;

public record ClinicServiceResponse(
    Long id,
    String name,
    String category,
    String description,
    String duration,
    String price
) {
}
