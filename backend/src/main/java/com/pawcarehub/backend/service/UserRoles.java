package com.pawcarehub.backend.service;

import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

public final class UserRoles {
    public static final String USER = "user";
    public static final String ADMIN = "admin";
    public static final String FRONT_DESK = "front_desk";
    public static final String DOCTOR = "doctor";

    private static final Set<String> VALID_ROLES = Set.of(USER, ADMIN, FRONT_DESK, DOCTOR);

    private UserRoles() {
    }

    public static String normalize(String role) {
        return normalizeLegacyRole(role);
    }

    public static String normalizeLegacyRole(String role) {
        if (role == null) {
            return USER;
        }

        String normalized = role.trim().toLowerCase().replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case USER, "customer" -> USER;
            case ADMIN, "administrator" -> ADMIN;
            case FRONT_DESK, "frontdesk" -> FRONT_DESK;
            case DOCTOR -> DOCTOR;
            default -> USER;
        };
    }

    public static String validateRole(String role) {
        if (!StringUtils.hasText(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }

        String normalized = role.trim();
        if (!VALID_ROLES.contains(normalized)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid role. Allowed roles are user, doctor, front_desk, admin"
            );
        }

        return normalized;
    }

    public static boolean isClinicStaff(String role) {
        String normalized = normalizeLegacyRole(role);
        return ADMIN.equals(normalized) || FRONT_DESK.equals(normalized) || DOCTOR.equals(normalized);
    }
}
