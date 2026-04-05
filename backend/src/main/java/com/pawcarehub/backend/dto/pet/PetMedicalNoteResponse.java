package com.pawcarehub.backend.dto.pet;

public record PetMedicalNoteResponse(
    Long id,
    String date,
    String author,
    Long relatedBookingId,
    String noteText,
    String createdAt,
    String updatedAt
) {
}
