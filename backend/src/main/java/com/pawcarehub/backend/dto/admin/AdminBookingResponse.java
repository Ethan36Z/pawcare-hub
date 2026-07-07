package com.pawcarehub.backend.dto.admin;

public class AdminBookingResponse {

    private final Long id;
    private final String petName;
    private final String service;
    private final Long serviceId;
    private final String date;
    private final String time;
    private final String status;
    private final String clinic;
    private final String staff;
    private final Long staffId;
    private final Long ownerId;
    private final String ownerName;
    private final String ownerEmail;
    private final String ownerNote;
    private final String visitSummary;
    private final String diagnosisAssessment;
    private final String treatmentRecommendation;
    private final String followUpNote;

    public AdminBookingResponse(
        Long id,
        String petName,
        String service,
        Long serviceId,
        String date,
        String time,
        String status,
        String clinic,
        String staff,
        Long staffId,
        Long ownerId,
        String ownerName,
        String ownerEmail,
        String ownerNote,
        String visitSummary,
        String diagnosisAssessment,
        String treatmentRecommendation,
        String followUpNote
    ) {
        this.id = id;
        this.petName = petName;
        this.service = service;
        this.serviceId = serviceId;
        this.date = date;
        this.time = time;
        this.status = status;
        this.clinic = clinic;
        this.staff = staff;
        this.staffId = staffId;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
        this.ownerNote = ownerNote;
        this.visitSummary = visitSummary;
        this.diagnosisAssessment = diagnosisAssessment;
        this.treatmentRecommendation = treatmentRecommendation;
        this.followUpNote = followUpNote;
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

    public Long getServiceId() {
        return serviceId;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getStatus() {
        return status;
    }

    public String getClinic() {
        return clinic;
    }

    public String getStaff() {
        return staff;
    }

    public Long getStaffId() {
        return staffId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getOwnerNote() {
        return ownerNote;
    }

    public String getVisitSummary() {
        return visitSummary;
    }

    public String getDiagnosisAssessment() {
        return diagnosisAssessment;
    }

    public String getTreatmentRecommendation() {
        return treatmentRecommendation;
    }

    public String getFollowUpNote() {
        return followUpNote;
    }
}
