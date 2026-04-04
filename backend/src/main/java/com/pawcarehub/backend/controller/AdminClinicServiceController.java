package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.admin.CreateClinicServiceRequest;
import com.pawcarehub.backend.dto.admin.AdminClinicServiceResponse;
import com.pawcarehub.backend.service.AdminClinicServiceService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/services")
public class AdminClinicServiceController {

    private final AdminClinicServiceService adminClinicServiceService;

    public AdminClinicServiceController(AdminClinicServiceService adminClinicServiceService) {
        this.adminClinicServiceService = adminClinicServiceService;
    }

    @GetMapping
    public List<AdminClinicServiceResponse> getServices() {
        return adminClinicServiceService.getAllServices();
    }

    @PostMapping
    public AdminClinicServiceResponse createService(@RequestBody CreateClinicServiceRequest request) {
        return adminClinicServiceService.createService(request);
    }

    @PatchMapping("/{id}/toggle")
    public AdminClinicServiceResponse toggleService(@PathVariable Long id) {
        return adminClinicServiceService.toggleServiceAvailability(id);
    }
}
