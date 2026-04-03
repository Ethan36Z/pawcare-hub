package com.pawcarehub.backend.dto.auth;

public record AuthResponse(
    String message,
    String email,
    String name
) {
}
