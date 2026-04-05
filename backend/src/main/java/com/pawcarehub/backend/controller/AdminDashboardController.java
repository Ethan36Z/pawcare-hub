package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.admin.AdminDashboardStatsResponse;
import com.pawcarehub.backend.service.RoleAccessService;
import com.pawcarehub.backend.service.UserRoles;
import com.pawcarehub.backend.service.AdminDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final RoleAccessService roleAccessService;

    public AdminDashboardController(
        AdminDashboardService adminDashboardService,
        RoleAccessService roleAccessService
    ) {
        this.adminDashboardService = adminDashboardService;
        this.roleAccessService = roleAccessService;
    }

    @GetMapping("/stats")
    public AdminDashboardStatsResponse getStats(
        @RequestHeader("X-User-Email") String userEmailHeader
    ) {
        roleAccessService.requireAnyRole(userEmailHeader, UserRoles.ADMIN, UserRoles.FRONT_DESK, UserRoles.DOCTOR);
        return adminDashboardService.getStats();
    }
}
