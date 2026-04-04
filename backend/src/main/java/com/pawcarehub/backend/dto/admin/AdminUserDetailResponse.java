package com.pawcarehub.backend.dto.admin;

import java.util.List;

public class AdminUserDetailResponse {

    private final Long id;
    private final String name;
    private final String email;
    private final String role;
    private final boolean active;
    private final String phone;
    private final String address;
    private final String preferredContactMethod;
    private final boolean emailRemindersEnabled;
    private final boolean textRemindersEnabled;
    private final int petCount;
    private final int bookingCount;
    private final List<AdminUserPetSummaryResponse> pets;
    private final List<AdminUserBookingSummaryResponse> recentBookings;

    public AdminUserDetailResponse(
        Long id,
        String name,
        String email,
        String role,
        boolean active,
        String phone,
        String address,
        String preferredContactMethod,
        boolean emailRemindersEnabled,
        boolean textRemindersEnabled,
        int petCount,
        int bookingCount,
        List<AdminUserPetSummaryResponse> pets,
        List<AdminUserBookingSummaryResponse> recentBookings
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.active = active;
        this.phone = phone;
        this.address = address;
        this.preferredContactMethod = preferredContactMethod;
        this.emailRemindersEnabled = emailRemindersEnabled;
        this.textRemindersEnabled = textRemindersEnabled;
        this.petCount = petCount;
        this.bookingCount = bookingCount;
        this.pets = pets;
        this.recentBookings = recentBookings;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getPreferredContactMethod() {
        return preferredContactMethod;
    }

    public boolean isEmailRemindersEnabled() {
        return emailRemindersEnabled;
    }

    public boolean isTextRemindersEnabled() {
        return textRemindersEnabled;
    }

    public int getPetCount() {
        return petCount;
    }

    public int getBookingCount() {
        return bookingCount;
    }

    public List<AdminUserPetSummaryResponse> getPets() {
        return pets;
    }

    public List<AdminUserBookingSummaryResponse> getRecentBookings() {
        return recentBookings;
    }
}
