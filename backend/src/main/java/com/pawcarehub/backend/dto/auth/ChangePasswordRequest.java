package com.pawcarehub.backend.dto.auth;

public record ChangePasswordRequest(
    String currentPassword,
    String newPassword
) {
}
