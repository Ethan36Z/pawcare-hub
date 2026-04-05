package com.pawcarehub.backend.dto.pet;

import java.util.List;

public record PetResponse(
    Long id,
    String name,
    String species,
    String breed,
    String age,
    String weight,
    String note,
    String sex,
    String dateOfBirth,
    String color,
    String microchipNumber,
    String allergies,
    String chronicConditions,
    String medications,
    String vaccinationNotes,
    String generalMedicalNotes,
    String status,
    String displayStatus,
    List<PetMedicalNoteResponse> medicalNotes
) {
}
