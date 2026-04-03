package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.booking.BookingResponse;
import com.pawcarehub.backend.service.BookingService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/me")
    public List<BookingResponse> getCurrentUserBookings(
        @RequestHeader("X-User-Email") String userEmailHeader
    ) {
        return bookingService.getCurrentUserBookings(userEmailHeader);
    }
}
