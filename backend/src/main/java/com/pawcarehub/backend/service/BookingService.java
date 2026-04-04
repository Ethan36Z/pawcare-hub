package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.auth.AuthenticatedUser;
import com.pawcarehub.backend.dto.booking.BookingResponse;
import com.pawcarehub.backend.dto.booking.CreateBookingRequest;
import com.pawcarehub.backend.dto.booking.RescheduleBookingRequest;
import com.pawcarehub.backend.entity.Booking;
import com.pawcarehub.backend.entity.ClinicService;
import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.repository.BookingRepository;
import com.pawcarehub.backend.repository.ClinicServiceRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingService {

    private final AuthService authService;
    private final BookingRepository bookingRepository;
    private final ClinicServiceRepository clinicServiceRepository;

    public BookingService(
        AuthService authService,
        BookingRepository bookingRepository,
        ClinicServiceRepository clinicServiceRepository
    ) {
        this.authService = authService;
        this.bookingRepository = bookingRepository;
        this.clinicServiceRepository = clinicServiceRepository;
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getCurrentUserBookings(String userEmailHeader) {
        AuthenticatedUser user = authService.getAuthenticatedUser(userEmailHeader);
        return bookingRepository.findByOwnerEmailOrderByIdAsc(user.email()).stream()
            .map(booking -> toBookingResponse(booking, user.email()))
            .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getCurrentUserBooking(String userEmailHeader, Long bookingId) {
        AuthenticatedUser user = authService.getAuthenticatedUser(userEmailHeader);
        Booking booking = bookingRepository.findByIdAndOwnerEmail(bookingId, user.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        return toBookingResponse(booking, user.email());
    }

    @Transactional
    public BookingResponse createBooking(String userEmailHeader, CreateBookingRequest request) {
        User owner = authService.getAuthenticatedUserEntity(userEmailHeader);
        ClinicService serviceRecord = resolveServiceRecord(request.serviceId());
        String serviceName = resolveServiceName(request, serviceRecord);

        Booking savedBooking = bookingRepository.save(new Booking(
            normalizeRequiredField(request.petName(), "petName"),
            serviceName,
            normalizeRequiredField(request.date(), "date"),
            normalizeRequiredField(request.time(), "time"),
            normalizeRequiredField(request.status(), "status"),
            normalizeRequiredField(request.clinic(), "clinic"),
            normalizeRequiredField(request.staff(), "staff"),
            serviceRecord,
            owner
        ));

        return toBookingResponse(savedBooking, owner.getEmail());
    }

    @Transactional
    public BookingResponse cancelBooking(String userEmailHeader, Long bookingId) {
        AuthenticatedUser user = authService.getAuthenticatedUser(userEmailHeader);
        Booking booking = bookingRepository.findByIdAndOwnerEmail(bookingId, user.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if ("Cancelled".equalsIgnoreCase(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking is already cancelled");
        }

        booking.setStatus("Cancelled");
        Booking savedBooking = bookingRepository.save(booking);
        return toBookingResponse(savedBooking, user.email());
    }

    @Transactional
    public BookingResponse rescheduleBooking(String userEmailHeader, Long bookingId, RescheduleBookingRequest request) {
        AuthenticatedUser user = authService.getAuthenticatedUser(userEmailHeader);
        Booking booking = bookingRepository.findByIdAndOwnerEmail(bookingId, user.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if ("Cancelled".equalsIgnoreCase(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancelled bookings cannot be rescheduled");
        }

        booking.setDate(normalizeRequiredField(request.date(), "date"));
        booking.setTime(normalizeRequiredField(request.time(), "time"));

        if (StringUtils.hasText(request.staff())) {
            booking.setStaff(request.staff().trim());
        }

        Booking savedBooking = bookingRepository.save(booking);
        return toBookingResponse(savedBooking, user.email());
    }

    private BookingResponse toBookingResponse(Booking booking, String ownerEmail) {
        return new BookingResponse(
            booking.getId(),
            booking.getPetName(),
            booking.getServiceRecord() != null ? booking.getServiceRecord().getId() : null,
            booking.getResolvedServiceName(),
            booking.getDate(),
            booking.getTime(),
            booking.getStatus(),
            booking.getClinic(),
            booking.getStaff(),
            ownerEmail
        );
    }

    private ClinicService resolveServiceRecord(Long serviceId) {
        if (serviceId == null) {
            return null;
        }

        return clinicServiceRepository.findByIdAndActiveTrue(serviceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected service is not available"));
    }

    private String resolveServiceName(CreateBookingRequest request, ClinicService serviceRecord) {
        if (serviceRecord != null) {
            return serviceRecord.getName();
        }

        return normalizeRequiredField(request.service(), "service");
    }

    private String normalizeRequiredField(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }

        return value.trim();
    }
}
