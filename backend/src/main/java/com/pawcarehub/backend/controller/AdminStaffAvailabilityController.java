package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.admin.UpsertStaffAvailabilityRequest;
import com.pawcarehub.backend.dto.staffavailability.StaffAvailabilityResponse;
import com.pawcarehub.backend.service.StaffAvailabilityService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/staff/{staffId}/availability")
public class AdminStaffAvailabilityController {

    private final StaffAvailabilityService staffAvailabilityService;

    public AdminStaffAvailabilityController(StaffAvailabilityService staffAvailabilityService) {
        this.staffAvailabilityService = staffAvailabilityService;
    }

    @GetMapping
    public List<StaffAvailabilityResponse> getAvailability(@PathVariable Long staffId) {
        return staffAvailabilityService.getStaffAvailability(staffId);
    }

    @PostMapping
    public StaffAvailabilityResponse createAvailability(
        @PathVariable Long staffId,
        @RequestBody UpsertStaffAvailabilityRequest request
    ) {
        return staffAvailabilityService.createAvailability(staffId, request);
    }

    @PatchMapping("/{availabilityId}")
    public StaffAvailabilityResponse updateAvailability(
        @PathVariable Long staffId,
        @PathVariable Long availabilityId,
        @RequestBody UpsertStaffAvailabilityRequest request
    ) {
        return staffAvailabilityService.updateAvailability(staffId, availabilityId, request);
    }

    @PatchMapping("/{availabilityId}/toggle")
    public StaffAvailabilityResponse toggleAvailability(
        @PathVariable Long staffId,
        @PathVariable Long availabilityId
    ) {
        return staffAvailabilityService.toggleAvailability(staffId, availabilityId);
    }

    @DeleteMapping("/{availabilityId}")
    public ResponseEntity<Void> deleteAvailability(
        @PathVariable Long staffId,
        @PathVariable Long availabilityId
    ) {
        staffAvailabilityService.deleteAvailability(staffId, availabilityId);
        return ResponseEntity.noContent().build();
    }
}
