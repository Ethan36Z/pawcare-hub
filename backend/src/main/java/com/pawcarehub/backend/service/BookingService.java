package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.auth.AuthenticatedUser;
import com.pawcarehub.backend.dto.booking.BookingResponse;
import com.pawcarehub.backend.entity.Booking;
import com.pawcarehub.backend.repository.BookingRepository;
import java.util.List;
import org.springframework.stereotype.Service;

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
}
