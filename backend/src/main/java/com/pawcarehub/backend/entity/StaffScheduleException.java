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
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "staff_schedule_exceptions")
public class StaffScheduleException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private boolean available;

    @Column
    private LocalTime customStartTime;

    @Column
    private LocalTime customEndTime;

    protected StaffScheduleException() {
    }

    public StaffScheduleException(
        Staff staff,
        LocalDate date,
        boolean available,
        LocalTime customStartTime,
        LocalTime customEndTime
    ) {
        this.staff = staff;
        this.date = date;
        this.available = available;
        this.customStartTime = customStartTime;
        this.customEndTime = customEndTime;
    }

    public Long getId() {
        return id;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public LocalTime getCustomStartTime() {
        return customStartTime;
    }

    public void setCustomStartTime(LocalTime customStartTime) {
        this.customStartTime = customStartTime;
    }

    public LocalTime getCustomEndTime() {
        return customEndTime;
    }

    public void setCustomEndTime(LocalTime customEndTime) {
        this.customEndTime = customEndTime;
    }
}
