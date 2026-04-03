package com.pawcarehub.backend.dto.auth;

public record LoginRequest(
    String email,
    String password
) {
}
