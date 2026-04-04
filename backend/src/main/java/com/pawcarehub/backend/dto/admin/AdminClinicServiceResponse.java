package com.pawcarehub.backend.dto.admin;

public class AdminClinicServiceResponse {

    private final Long id;
    private final String name;
    private final String category;
    private final String description;
    private final String duration;
    private final String price;

    public AdminClinicServiceResponse(
        Long id,
        String name,
        String category,
        String description,
        String duration,
        String price
    ) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.duration = duration;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getDuration() {
        return duration;
    }

    public String getPrice() {
        return price;
    }
}
