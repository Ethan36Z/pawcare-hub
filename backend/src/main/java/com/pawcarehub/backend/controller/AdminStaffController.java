package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.admin.CreateStaffRequest;
import com.pawcarehub.backend.dto.staff.StaffResponse;
import com.pawcarehub.backend.service.RoleAccessService;
import com.pawcarehub.backend.service.UserRoles;
import com.pawcarehub.backend.service.StaffService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/staff")
public class AdminStaffController {

    private final StaffService staffService;
    private final RoleAccessService roleAccessService;

    public AdminStaffController(StaffService staffService, RoleAccessService roleAccessService) {
        this.staffService = staffService;
        this.roleAccessService = roleAccessService;
    }

    @GetMapping
    public List<StaffResponse> getStaff() {
        roleAccessService.requireAnyRole(UserRoles.ADMIN);
        return staffService.getAllStaff();
    }

    @GetMapping("/operations-list")
    public List<StaffResponse> getOperationsStaff() {
        roleAccessService.requireAnyRole(UserRoles.ADMIN, UserRoles.FRONT_DESK);
        return staffService.getAllStaff();
    }

    @PostMapping
    public StaffResponse createStaff(
        @RequestBody CreateStaffRequest request
    ) {
        roleAccessService.requireAnyRole(UserRoles.ADMIN);
        return staffService.createStaff(request);
    }

    @PatchMapping("/{id}")
    public StaffResponse updateStaff(
        @PathVariable Long id,
        @RequestBody CreateStaffRequest request
    ) {
        roleAccessService.requireAnyRole(UserRoles.ADMIN);
        return staffService.updateStaff(id, request);
    }

    @PatchMapping("/{id}/toggle")
    public StaffResponse toggleStaff(@PathVariable Long id) {
        roleAccessService.requireAnyRole(UserRoles.ADMIN);
        return staffService.toggleStaff(id);
    }
}
