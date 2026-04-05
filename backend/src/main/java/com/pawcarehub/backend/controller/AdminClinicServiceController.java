package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.admin.CreateClinicServiceRequest;
import com.pawcarehub.backend.dto.admin.AdminClinicServiceResponse;
import com.pawcarehub.backend.service.RoleAccessService;
import com.pawcarehub.backend.service.UserRoles;
import com.pawcarehub.backend.service.AdminClinicServiceService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/services")
public class AdminClinicServiceController {

    private final AdminClinicServiceService adminClinicServiceService;
    private final RoleAccessService roleAccessService;

    public AdminClinicServiceController(
        AdminClinicServiceService adminClinicServiceService,
        RoleAccessService roleAccessService
    ) {
        this.adminClinicServiceService = adminClinicServiceService;
        this.roleAccessService = roleAccessService;
    }

    @GetMapping
    public List<AdminClinicServiceResponse> getServices(
        @RequestHeader("X-User-Email") String userEmailHeader,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String name,
        @RequestParam(defaultValue = "name-asc") String sort
    ) {
        roleAccessService.requireAnyRole(userEmailHeader, UserRoles.ADMIN);
        return adminClinicServiceService.getAllServices(active, category, name, sort);
    }

    @PostMapping
    public AdminClinicServiceResponse createService(
        @RequestHeader("X-User-Email") String userEmailHeader,
        @RequestBody CreateClinicServiceRequest request
    ) {
        roleAccessService.requireAnyRole(userEmailHeader, UserRoles.ADMIN);
        return adminClinicServiceService.createService(request);
    }

    @PatchMapping("/{id}")
    public AdminClinicServiceResponse updateService(
        @RequestHeader("X-User-Email") String userEmailHeader,
        @PathVariable Long id,
        @RequestBody CreateClinicServiceRequest request
    ) {
        roleAccessService.requireAnyRole(userEmailHeader, UserRoles.ADMIN);
        return adminClinicServiceService.updateService(id, request);
    }

    @PatchMapping("/{id}/toggle")
    public AdminClinicServiceResponse toggleService(
        @RequestHeader("X-User-Email") String userEmailHeader,
        @PathVariable Long id
    ) {
        roleAccessService.requireAnyRole(userEmailHeader, UserRoles.ADMIN);
        return adminClinicServiceService.toggleServiceAvailability(id);
    }
}
