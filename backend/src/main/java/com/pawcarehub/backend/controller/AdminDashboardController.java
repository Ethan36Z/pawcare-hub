package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.admin.AdminDashboardStatsResponse;
import com.pawcarehub.backend.service.AdminDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/stats")
    public AdminDashboardStatsResponse getStats() {
        return adminDashboardService.getStats();
    }
}
