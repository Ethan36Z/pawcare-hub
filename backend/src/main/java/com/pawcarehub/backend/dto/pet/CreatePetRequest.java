package com.pawcarehub.backend.dto.pet;

public record CreatePetRequest(
    String name,
    String species,
    String breed,
    String age,
    String weight,
    String note,
    String status
) {
}
