package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.admin.CreateStaffRequest;
import com.pawcarehub.backend.dto.staff.StaffResponse;
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

    public AdminStaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    public List<StaffResponse> getStaff() {
        return staffService.getAllStaff();
    }

    @PostMapping
    public StaffResponse createStaff(@RequestBody CreateStaffRequest request) {
        return staffService.createStaff(request);
    }

    @PatchMapping("/{id}")
    public StaffResponse updateStaff(
        @PathVariable Long id,
        @RequestBody CreateStaffRequest request
    ) {
        return staffService.updateStaff(id, request);
    }

    @PatchMapping("/{id}/toggle")
    public StaffResponse toggleStaff(@PathVariable Long id) {
        return staffService.toggleStaff(id);
    }
}
