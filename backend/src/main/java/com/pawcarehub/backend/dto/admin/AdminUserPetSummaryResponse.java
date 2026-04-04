package com.pawcarehub.backend.dto.admin;

public class AdminUserPetSummaryResponse {

    private final Long id;
    private final String name;
    private final String species;
    private final String breed;
    private final String status;

    public AdminUserPetSummaryResponse(Long id, String name, String species, String breed, String status) {
        this.id = id;
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSpecies() {
        return species;
    }

    public String getBreed() {
        return breed;
    }

    public String getStatus() {
        return status;
    }
}
