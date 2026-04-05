package com.pawcarehub.backend.dto.pet;

public record PetMedicalNoteRequest(
    String date,
    String author,
    Long relatedBookingId,
    String noteText
) {
}
