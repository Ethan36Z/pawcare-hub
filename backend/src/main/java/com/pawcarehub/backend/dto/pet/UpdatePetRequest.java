package com.pawcarehub.backend.dto.pet;

public record UpdatePetRequest(
    String name,
    String species,
    String breed,
    String age,
    String weight,
    String note,
    String status
) {
}
