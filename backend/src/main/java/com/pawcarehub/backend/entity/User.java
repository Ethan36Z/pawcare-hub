package com.pawcarehub.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role = "user";

    private String phone;

    private String address;

    private String preferredContactMethod;

    private Boolean emailReminders;

    private Boolean textReminders;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean active = true;

    protected User() {
    }

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPreferredContactMethod() {
        return preferredContactMethod;
    }

    public void setPreferredContactMethod(String preferredContactMethod) {
        this.preferredContactMethod = preferredContactMethod;
    }

    public boolean isEmailRemindersEnabled() {
        return Boolean.TRUE.equals(emailReminders);
    }

    public void setEmailReminders(Boolean emailReminders) {
        this.emailReminders = emailReminders;
    }

    public boolean isTextRemindersEnabled() {
        return Boolean.TRUE.equals(textReminders);
    }

    public void setTextReminders(Boolean textReminders) {
        this.textReminders = textReminders;
    }

    public boolean isActive() {
        return active == null || active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @PrePersist
    protected void applyDefaults() {
        if (active == null) {
            active = true;
        }

        if (role == null) {
            role = "user";
        }
    }
}
