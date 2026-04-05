package com.pawcarehub.backend.dto.pet;

public record PetMedicalNoteResponse(
    Long id,
    String date,
    String author,
    Long relatedBookingId,
    String noteText,
    String createdAt,
    String updatedAt,
    RelatedVisitSummary relatedVisit
) {
    public record RelatedVisitSummary(
        Long bookingId,
        String service,
        String staff,
        String date,
        String time,
        String status,
        String visitSummary,
        String diagnosisAssessment,
        String treatmentRecommendation,
        String followUpNote
    ) {
    }
}
