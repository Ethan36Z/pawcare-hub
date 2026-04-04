package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.admin.AdminBookingResponse;
import com.pawcarehub.backend.entity.Booking;
import com.pawcarehub.backend.repository.BookingRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBookingService {

    private final BookingRepository bookingRepository;

    public AdminBookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminBookingResponse> getAllBookings() {
        return bookingRepository.findAllByOrderByIdDesc().stream()
            .map(this::toAdminBookingResponse)
            .toList();
    }

    private AdminBookingResponse toAdminBookingResponse(Booking booking) {
        return new AdminBookingResponse(
            booking.getId(),
            booking.getPetName(),
            booking.getService(),
            booking.getDate(),
            booking.getTime(),
            booking.getStatus(),
            booking.getClinic(),
            booking.getStaff(),
            booking.getOwner().getId(),
            booking.getOwner().getName(),
            booking.getOwner().getEmail()
        );
    }
}
