package com.pawcarehub.backend.dto.staff;

public record PublicStaffProfileResponse(
    Long id,
    String displayName,
    String title,
    String bio,
    String photoUrl
) {
}
