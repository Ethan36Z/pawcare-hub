package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.admin.UpsertStaffAvailabilityRequest;
import com.pawcarehub.backend.dto.scheduling.ResolveAvailabilityConflictRequest;
import com.pawcarehub.backend.dto.staffavailability.StaffAvailabilityResponse;
import com.pawcarehub.backend.service.RoleAccessService;
import com.pawcarehub.backend.service.StaffAvailabilityService;
import com.pawcarehub.backend.service.UserRoles;
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
    private final RoleAccessService roleAccessService;

    public AdminStaffAvailabilityController(
        StaffAvailabilityService staffAvailabilityService,
        RoleAccessService roleAccessService
    ) {
        this.staffAvailabilityService = staffAvailabilityService;
        this.roleAccessService = roleAccessService;
    }

    @GetMapping
    public List<StaffAvailabilityResponse> getAvailability(@PathVariable Long staffId) {
        roleAccessService.requireAnyRole(UserRoles.ADMIN, UserRoles.FRONT_DESK);
        return staffAvailabilityService.getStaffAvailability(staffId);
    }

    @PostMapping
    public StaffAvailabilityResponse createAvailability(
        @PathVariable Long staffId,
        @RequestBody UpsertStaffAvailabilityRequest request
    ) {
        roleAccessService.requireAnyRole(UserRoles.ADMIN, UserRoles.FRONT_DESK);
        return staffAvailabilityService.createAvailability(staffId, request);
    }

    @PatchMapping("/{availabilityId}")
    public StaffAvailabilityResponse updateAvailability(
        @PathVariable Long staffId,
        @PathVariable Long availabilityId,
        @RequestBody UpsertStaffAvailabilityRequest request
    ) {
        roleAccessService.requireAnyRole(UserRoles.ADMIN, UserRoles.FRONT_DESK);
        return staffAvailabilityService.updateAvailability(staffId, availabilityId, request);
    }

    @PostMapping("/{availabilityId}/resolve-conflicts")
    public StaffAvailabilityResponse resolveAvailabilityConflicts(
        @PathVariable Long staffId,
        @PathVariable Long availabilityId,
        @RequestBody ResolveAvailabilityConflictRequest request
    ) {
        roleAccessService.requireAnyRole(UserRoles.ADMIN, UserRoles.FRONT_DESK);
        return staffAvailabilityService.resolveAndUpdateAvailability(
            staffId,
            availabilityId,
            request.scheduleChange(),
            request.reassignments()
        );
    }

    @PatchMapping("/{availabilityId}/toggle")
    public StaffAvailabilityResponse toggleAvailability(
        @PathVariable Long staffId,
        @PathVariable Long availabilityId
    ) {
        roleAccessService.requireAnyRole(UserRoles.ADMIN, UserRoles.FRONT_DESK);
        return staffAvailabilityService.toggleAvailability(staffId, availabilityId);
    }

    @DeleteMapping("/{availabilityId}")
    public ResponseEntity<Void> deleteAvailability(
        @PathVariable Long staffId,
        @PathVariable Long availabilityId
    ) {
        roleAccessService.requireAnyRole(UserRoles.ADMIN, UserRoles.FRONT_DESK);
        staffAvailabilityService.deleteAvailability(staffId, availabilityId);
        return ResponseEntity.noContent().build();
    }
}
