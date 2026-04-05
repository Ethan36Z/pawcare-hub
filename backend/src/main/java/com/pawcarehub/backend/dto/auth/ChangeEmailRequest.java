package com.pawcarehub.backend.dto.auth;

public record ChangeEmailRequest(
    String currentPassword,
    String newEmail
) {
}
