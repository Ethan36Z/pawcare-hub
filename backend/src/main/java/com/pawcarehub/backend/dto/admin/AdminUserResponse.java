package com.pawcarehub.backend.dto.admin;

public class AdminUserResponse {

    private final Long id;
    private final String name;
    private final String email;
    private final String role;
    private final boolean active;

    public AdminUserResponse(Long id, String name, String email, String role, boolean active) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }
}
