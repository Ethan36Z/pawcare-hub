package com.pawcarehub.backend.dto.auth;

public record UserProfileResponse(
    String name,
    String email,
    String phone,
    String address,
    String preferredContactMethod,
    boolean active,
    boolean emailReminders,
    boolean textReminders
) {
}
