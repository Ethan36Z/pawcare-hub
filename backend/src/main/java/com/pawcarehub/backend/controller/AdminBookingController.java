package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.admin.AdminBookingResponse;
import com.pawcarehub.backend.service.AdminBookingService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    public AdminBookingController(AdminBookingService adminBookingService) {
        this.adminBookingService = adminBookingService;
    }

    @GetMapping
    public List<AdminBookingResponse> getBookings(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String service,
        @RequestParam(required = false) String owner,
        @RequestParam(defaultValue = "latest") String sort
    ) {
        return adminBookingService.getAllBookings(status, service, owner, sort);
    }

    @PatchMapping("/{id}/confirm")
    public AdminBookingResponse confirmBooking(@PathVariable Long id) {
        return adminBookingService.confirmBooking(id);
    }

    @PatchMapping("/{id}/cancel")
    public AdminBookingResponse cancelBooking(@PathVariable Long id) {
        return adminBookingService.cancelBooking(id);
    }
}
