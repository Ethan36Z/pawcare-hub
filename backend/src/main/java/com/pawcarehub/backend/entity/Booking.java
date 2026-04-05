package com.pawcarehub.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ClinicService serviceRecord;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staffRecord;

    @Column(length = 2000)
    private String visitSummary;

    @Column(length = 2000)
    private String diagnosisAssessment;

    @Column(length = 2000)
    private String treatmentRecommendation;

    @Column(length = 2000)
    private String followUpNote;

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
        ClinicService serviceRecord,
        Staff staffRecord,
        User owner
    ) {
        this.petName = petName;
        this.service = service;
        this.date = date;
        this.time = time;
        this.status = status;
        this.clinic = clinic;
        this.staff = staff;
        this.serviceRecord = serviceRecord;
        this.staffRecord = staffRecord;
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

    public String getResolvedServiceName() {
        return serviceRecord != null ? serviceRecord.getName() : service;
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

    public String getResolvedStaffName() {
        return staffRecord != null ? staffRecord.getName() : staff;
    }

    public void setStaff(String staff) {
        this.staff = staff;
    }

    public Staff getStaffRecord() {
        return staffRecord;
    }

    public void setStaffRecord(Staff staffRecord) {
        this.staffRecord = staffRecord;
        if (staffRecord != null) {
            this.staff = staffRecord.getName();
        }
    }

    public User getOwner() {
        return owner;
    }

    public ClinicService getServiceRecord() {
        return serviceRecord;
    }

    public String getVisitSummary() {
        return visitSummary;
    }

    public void setVisitSummary(String visitSummary) {
        this.visitSummary = visitSummary;
    }

    public String getDiagnosisAssessment() {
        return diagnosisAssessment;
    }

    public void setDiagnosisAssessment(String diagnosisAssessment) {
        this.diagnosisAssessment = diagnosisAssessment;
    }

    public String getTreatmentRecommendation() {
        return treatmentRecommendation;
    }

    public void setTreatmentRecommendation(String treatmentRecommendation) {
        this.treatmentRecommendation = treatmentRecommendation;
    }

    public String getFollowUpNote() {
        return followUpNote;
    }

    public void setFollowUpNote(String followUpNote) {
        this.followUpNote = followUpNote;
    }

    @PrePersist
    @PreUpdate
    @SuppressWarnings("unused")
    protected void syncReferenceNames() {
        if (serviceRecord != null) {
            service = serviceRecord.getName();
        }

        if (staffRecord != null) {
            staff = staffRecord.getName();
        }
    }
}
