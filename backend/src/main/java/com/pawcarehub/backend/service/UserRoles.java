package com.pawcarehub.backend.service;

import java.util.Set;

public final class UserRoles {
    public static final String USER = "user";
    public static final String ADMIN = "admin";
    public static final String FRONT_DESK = "front_desk";
    public static final String DOCTOR = "doctor";

    private static final Set<String> VALID_ROLES = Set.of(USER, ADMIN, FRONT_DESK, DOCTOR);

    private UserRoles() {
    }

    public static String normalize(String role) {
        if (role == null) {
            return USER;
        }

        String normalized = role.trim().toLowerCase();
        return VALID_ROLES.contains(normalized) ? normalized : USER;
    }

    public static boolean isClinicStaff(String role) {
        String normalized = normalize(role);
        return ADMIN.equals(normalized) || FRONT_DESK.equals(normalized) || DOCTOR.equals(normalized);
    }
}
