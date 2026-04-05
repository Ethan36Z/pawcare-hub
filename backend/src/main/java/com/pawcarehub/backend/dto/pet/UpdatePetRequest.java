package com.pawcarehub.backend.dto.pet;

public record UpdatePetRequest(
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
    String status
) {
}
