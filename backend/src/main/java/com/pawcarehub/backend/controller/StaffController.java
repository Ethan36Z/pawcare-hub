package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.staff.PublicStaffProfileResponse;
import com.pawcarehub.backend.dto.staff.StaffResponse;
import com.pawcarehub.backend.dto.staffavailability.ResolvedStaffAvailabilityResponse;
import com.pawcarehub.backend.service.StaffAvailabilityService;
import com.pawcarehub.backend.service.StaffService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService staffService;
    private final StaffAvailabilityService staffAvailabilityService;

    public StaffController(StaffService staffService, StaffAvailabilityService staffAvailabilityService) {
        this.staffService = staffService;
        this.staffAvailabilityService = staffAvailabilityService;
    }

    @GetMapping
    public List<StaffResponse> getActiveStaff() {
        return staffService.getActiveStaff();
    }

    @GetMapping("/homepage")
    public List<PublicStaffProfileResponse> getHomepageStaff() {
        return staffService.getHomepageStaff();
    }

    @GetMapping("/{staffId}/availability")
    public List<ResolvedStaffAvailabilityResponse> getActiveAvailability(
        @PathVariable Long staffId,
        @RequestParam LocalDate date
    ) {
        return staffAvailabilityService.getBookableAvailability(staffId, date);
    }
}
