package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.auth.AuthenticatedUser;
import com.pawcarehub.backend.dto.booking.CreateBookingRequest;
import com.pawcarehub.backend.dto.booking.BookingResponse;
import com.pawcarehub.backend.entity.Booking;
import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.repository.BookingRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingService {

    private final AuthService authService;
    private final BookingRepository bookingRepository;

    public BookingService(AuthService authService, BookingRepository bookingRepository) {
        this.authService = authService;
        this.bookingRepository = bookingRepository;
    }

    public List<BookingResponse> getCurrentUserBookings(String userEmailHeader) {
        AuthenticatedUser user = authService.getAuthenticatedUser(userEmailHeader);
        return bookingRepository.findByOwnerEmailOrderByIdAsc(user.email()).stream()
            .map(booking -> toBookingResponse(booking, user.email()))
            .toList();
    }

    public BookingResponse createBooking(String userEmailHeader, CreateBookingRequest request) {
        User owner = authService.getAuthenticatedUserEntity(userEmailHeader);

        Booking savedBooking = bookingRepository.save(new Booking(
            normalizeRequiredField(request.petName(), "petName"),
            normalizeRequiredField(request.service(), "service"),
            normalizeRequiredField(request.date(), "date"),
            normalizeRequiredField(request.time(), "time"),
            normalizeRequiredField(request.status(), "status"),
            normalizeRequiredField(request.clinic(), "clinic"),
            normalizeRequiredField(request.staff(), "staff"),
            owner
        ));

        return toBookingResponse(savedBooking, owner.getEmail());
    }

    private BookingResponse toBookingResponse(Booking booking, String ownerEmail) {
        return new BookingResponse(
            booking.getPetName(),
            booking.getService(),
            booking.getDate(),
            booking.getTime(),
            booking.getStatus(),
            booking.getClinic(),
            booking.getStaff(),
            ownerEmail
        );
    }

    private String normalizeRequiredField(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }

        return value.trim();
    }
}
