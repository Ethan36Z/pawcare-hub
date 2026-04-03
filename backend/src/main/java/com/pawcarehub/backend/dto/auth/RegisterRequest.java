package com.pawcarehub.backend.dto.auth;

public record RegisterRequest(
    String name,
    String email,
    String password
) {
}
