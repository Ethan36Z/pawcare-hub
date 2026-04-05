package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.admin.AdminBookingResponse;
import com.pawcarehub.backend.entity.Booking;
import com.pawcarehub.backend.repository.BookingRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminBookingService {

    private final BookingRepository bookingRepository;

    public AdminBookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminBookingResponse> getAllBookings(String status, String service, String owner, String sort) {
        return bookingRepository.findAll().stream()
            .filter(booking -> matchesFilter(booking.getStatus(), status))
            .filter(booking -> matchesFilter(booking.getService(), service))
            .filter(booking -> matchesOwnerFilter(booking, owner))
            .sorted(resolveSort(sort))
            .map(this::toAdminBookingResponse)
            .toList();
    }

    @Transactional
    public AdminBookingResponse confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if ("Cancelled".equalsIgnoreCase(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancelled bookings cannot be confirmed");
        }

        booking.setStatus("Confirmed");
        Booking savedBooking = bookingRepository.save(booking);
        return toAdminBookingResponse(savedBooking);
    }

    @Transactional
    public AdminBookingResponse cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if ("Cancelled".equalsIgnoreCase(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking is already cancelled");
        }

        booking.setStatus("Cancelled");
        Booking savedBooking = bookingRepository.save(booking);
        return toAdminBookingResponse(savedBooking);
    }

    private boolean matchesFilter(String value, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }

        return value != null && value.equalsIgnoreCase(filter.trim());
    }

    private boolean matchesOwnerFilter(Booking booking, String owner) {
        if (owner == null || owner.isBlank()) {
            return true;
        }

        String normalizedOwner = owner.trim().toLowerCase();
        return containsIgnoreCase(booking.getOwner().getName(), normalizedOwner)
            || containsIgnoreCase(booking.getOwner().getEmail(), normalizedOwner);
    }

    private boolean containsIgnoreCase(String value, String normalizedFilter) {
        return value != null && value.toLowerCase().contains(normalizedFilter);
    }

    private Comparator<Booking> resolveSort(String sort) {
        if ("oldest".equalsIgnoreCase(sort)) {
            return Comparator.comparing(Booking::getId);
        }

        return Comparator.comparing(Booking::getId).reversed();
    }

    private AdminBookingResponse toAdminBookingResponse(Booking booking) {
        return new AdminBookingResponse(
            booking.getId(),
            booking.getPetName(),
            booking.getResolvedServiceName(),
            booking.getDate(),
            booking.getTime(),
            booking.getStatus(),
            booking.getClinic(),
            booking.getResolvedStaffName(),
            booking.getOwner().getId(),
            booking.getOwner().getName(),
            booking.getOwner().getEmail()
        );
    }
}
