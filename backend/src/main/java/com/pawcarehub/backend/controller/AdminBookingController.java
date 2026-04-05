package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.admin.AdminBookingResponse;
import com.pawcarehub.backend.dto.admin.CompleteBookingRequest;
import com.pawcarehub.backend.service.RoleAccessService;
import com.pawcarehub.backend.service.UserRoles;
import com.pawcarehub.backend.service.AdminBookingService;
import java.util.List;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

    private final AdminBookingService adminBookingService;
    private final RoleAccessService roleAccessService;

    public AdminBookingController(
        AdminBookingService adminBookingService,
        RoleAccessService roleAccessService
    ) {
        this.adminBookingService = adminBookingService;
        this.roleAccessService = roleAccessService;
    }

    @GetMapping
    public List<AdminBookingResponse> getBookings(
        @RequestHeader("X-User-Email") String userEmailHeader,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String service,
        @RequestParam(required = false) String owner,
        @RequestParam(defaultValue = "latest") String sort
    ) {
        roleAccessService.requireAnyRole(userEmailHeader, UserRoles.ADMIN, UserRoles.FRONT_DESK, UserRoles.DOCTOR);
        return adminBookingService.getAllBookings(status, service, owner, sort);
    }

    @PatchMapping("/{id}/confirm")
    public AdminBookingResponse confirmBooking(
        @RequestHeader("X-User-Email") String userEmailHeader,
        @PathVariable Long id
    ) {
        roleAccessService.requireAnyRole(userEmailHeader, UserRoles.ADMIN, UserRoles.FRONT_DESK);
        return adminBookingService.confirmBooking(id);
    }

    @PatchMapping("/{id}/cancel")
    public AdminBookingResponse cancelBooking(
        @RequestHeader("X-User-Email") String userEmailHeader,
        @PathVariable Long id
    ) {
        roleAccessService.requireAnyRole(userEmailHeader, UserRoles.ADMIN, UserRoles.FRONT_DESK);
        return adminBookingService.cancelBooking(id);
    }

    @PatchMapping("/{id}/complete")
    public AdminBookingResponse completeBooking(
        @RequestHeader("X-User-Email") String userEmailHeader,
        @PathVariable Long id,
        @RequestBody CompleteBookingRequest request
    ) {
        roleAccessService.requireAnyRole(userEmailHeader, UserRoles.ADMIN, UserRoles.DOCTOR);
        return adminBookingService.completeBooking(id, request);
    }
}
