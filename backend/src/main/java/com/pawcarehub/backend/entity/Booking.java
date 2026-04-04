package com.pawcarehub.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String petName;

    @Column(nullable = false)
    private String service;

    @Column(nullable = false)
    private String date;

    @Column(nullable = false)
    private String time;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String clinic;

    @Column(nullable = false)
    private String staff;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    protected Booking() {
    }

    public Booking(
        String petName,
        String service,
        String date,
        String time,
        String status,
        String clinic,
        String staff,
        User owner
    ) {
        this.petName = petName;
        this.service = service;
        this.date = date;
        this.time = time;
        this.status = status;
        this.clinic = clinic;
        this.staff = staff;
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }

    public String getPetName() {
        return petName;
    }

    public String getService() {
        return service;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getClinic() {
        return clinic;
    }

    public String getStaff() {
        return staff;
    }

    public void setStaff(String staff) {
        this.staff = staff;
    }

    public User getOwner() {
        return owner;
    }
}
