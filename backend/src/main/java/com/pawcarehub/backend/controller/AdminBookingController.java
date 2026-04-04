package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.admin.AdminBookingResponse;
import com.pawcarehub.backend.service.AdminBookingService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
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
    public List<AdminBookingResponse> getBookings() {
        return adminBookingService.getAllBookings();
    }
}
