package com.pawcarehub.backend.dto.pet;

public record PetResponse(
    Long id,
    String name,
    String species,
    String breed,
    String age,
    String weight,
    String note,
    String status,
    String displayStatus
) {
}
