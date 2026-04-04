package com.pawcarehub.backend.dto.admin;

public class AdminUserBookingSummaryResponse {

    private final Long id;
    private final String petName;
    private final String service;
    private final String date;
    private final String time;
    private final String status;
    private final String clinic;
    private final String staff;

    public AdminUserBookingSummaryResponse(
        Long id,
        String petName,
        String service,
        String date,
        String time,
        String status,
        String clinic,
        String staff
    ) {
        this.id = id;
        this.petName = petName;
        this.service = service;
        this.date = date;
        this.time = time;
        this.status = status;
        this.clinic = clinic;
        this.staff = staff;
    }

    public Long getId() {
        return id;
    }

    public String getPetName() {
        return petName;
    }

    public String getService() {
        return service;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getStatus() {
        return status;
    }

    public String getClinic() {
        return clinic;
    }

    public String getStaff() {
        return staff;
    }
}
