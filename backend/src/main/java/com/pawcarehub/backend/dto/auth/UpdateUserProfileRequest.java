package com.pawcarehub.backend.dto.auth;

public record UpdateUserProfileRequest(
    String phone,
    String address,
    String preferredContactMethod,
    Boolean emailReminders,
    Boolean textReminders
) {
}
