package com.pawcarehub.backend.dto.auth;

public record AuthenticatedUser(
    String name,
    String email
) {
}
