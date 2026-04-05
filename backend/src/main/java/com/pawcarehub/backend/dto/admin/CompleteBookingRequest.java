package com.pawcarehub.backend.dto.admin;

public record CompleteBookingRequest(
    String visitSummary,
    String diagnosisAssessment,
    String treatmentRecommendation,
    String followUpNote
) {
}
