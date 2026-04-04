package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.booking.BookingResponse;
import com.pawcarehub.backend.dto.booking.CreateBookingRequest;
import com.pawcarehub.backend.dto.booking.RescheduleBookingRequest;
import com.pawcarehub.backend.service.BookingService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @GetMapping("/{id}")
    public BookingResponse getCurrentUserBooking(
        @RequestHeader("X-User-Email") String userEmailHeader,
        @PathVariable Long id
    ) {
        return bookingService.getCurrentUserBooking(userEmailHeader, id);
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
        @RequestHeader("X-User-Email") String userEmailHeader,
        @RequestBody CreateBookingRequest request
    ) {
        BookingResponse response = bookingService.createBooking(userEmailHeader, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
        @RequestHeader("X-User-Email") String userEmailHeader,
        @PathVariable Long id
    ) {
        BookingResponse response = bookingService.cancelBooking(userEmailHeader, id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reschedule")
    public ResponseEntity<BookingResponse> rescheduleBooking(
        @RequestHeader("X-User-Email") String userEmailHeader,
        @PathVariable Long id,
        @RequestBody RescheduleBookingRequest request
    ) {
        BookingResponse response = bookingService.rescheduleBooking(userEmailHeader, id, request);
        return ResponseEntity.ok(response);
    }
}
