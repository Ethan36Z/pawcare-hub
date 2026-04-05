package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.auth.AuthenticatedUser;
import com.pawcarehub.backend.dto.booking.BookingResponse;
import com.pawcarehub.backend.dto.booking.CreateBookingRequest;
import com.pawcarehub.backend.dto.booking.RescheduleBookingRequest;
import com.pawcarehub.backend.entity.Booking;
import com.pawcarehub.backend.entity.ClinicService;
import com.pawcarehub.backend.entity.Staff;
import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.repository.BookingRepository;
import com.pawcarehub.backend.repository.ClinicServiceRepository;
import com.pawcarehub.backend.repository.StaffRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingService {
    private static final DateTimeFormatter APPOINTMENT_DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("MMMM d, uuuu h:mm a", Locale.US);

    private final AuthService authService;
    private final BookingRepository bookingRepository;
    private final ClinicServiceRepository clinicServiceRepository;
    private final StaffRepository staffRepository;
    private final StaffAvailabilityService staffAvailabilityService;

    public BookingService(
        AuthService authService,
        BookingRepository bookingRepository,
        ClinicServiceRepository clinicServiceRepository,
        StaffRepository staffRepository,
        StaffAvailabilityService staffAvailabilityService
    ) {
        this.authService = authService;
        this.bookingRepository = bookingRepository;
        this.clinicServiceRepository = clinicServiceRepository;
        this.staffRepository = staffRepository;
        this.staffAvailabilityService = staffAvailabilityService;
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
        Staff staffRecord = resolveStaffRecord(request.staffId(), request.staff());
        String serviceName = resolveServiceName(request, serviceRecord);
        String staffName = resolveStaffName(request, staffRecord);
        String appointmentDate = normalizeRequiredField(request.date(), "date");
        String appointmentTime = normalizeRequiredField(request.time(), "time");

        validateAppointmentTiming(appointmentDate, appointmentTime, staffRecord);

        Booking savedBooking = bookingRepository.save(new Booking(
            normalizeRequiredField(request.petName(), "petName"),
            serviceName,
            appointmentDate,
            appointmentTime,
            normalizeRequiredField(request.status(), "status"),
            normalizeRequiredField(request.clinic(), "clinic"),
            staffName,
            serviceRecord,
            staffRecord,
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

        if ("Completed".equalsIgnoreCase(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed bookings cannot be cancelled");
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

        if ("Completed".equalsIgnoreCase(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed bookings cannot be rescheduled");
        }

        String appointmentDate = normalizeRequiredField(request.date(), "date");
        String appointmentTime = normalizeRequiredField(request.time(), "time");

        Staff resolvedStaffRecord = booking.getStaffRecord();

        if (request.staffId() != null || StringUtils.hasText(request.staff())) {
            Staff staffRecord = resolveStaffRecord(request.staffId(), request.staff());
            booking.setStaff(resolveStaffName(request.staff(), staffRecord));
            booking.setStaffRecord(staffRecord);
            resolvedStaffRecord = staffRecord;
        }

        validateAppointmentTiming(appointmentDate, appointmentTime, resolvedStaffRecord);

        booking.setDate(appointmentDate);
        booking.setTime(appointmentTime);

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
            booking.getStaffRecord() != null ? booking.getStaffRecord().getId() : null,
            booking.getResolvedStaffName(),
            ownerEmail,
            booking.getVisitSummary(),
            booking.getDiagnosisAssessment(),
            booking.getTreatmentRecommendation(),
            booking.getFollowUpNote()
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

    private Staff resolveStaffRecord(Long staffId, String staffName) {
        if (staffId != null) {
            return staffRepository.findByIdAndActiveTrue(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected staff member is not available"));
        }

        if (!StringUtils.hasText(staffName)) {
            return null;
        }

        return staffRepository.findFirstByNameIgnoreCaseAndActiveTrue(staffName.trim()).orElse(null);
    }

    private String resolveStaffName(CreateBookingRequest request, Staff staffRecord) {
        return resolveStaffName(request.staff(), staffRecord);
    }

    private String resolveStaffName(String staffName, Staff staffRecord) {
        if (staffRecord != null) {
            return staffRecord.getName();
        }

        return normalizeRequiredField(staffName, "staff");
    }

    private String normalizeRequiredField(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }

        return value.trim();
    }

    private void validateAppointmentTiming(String date, String time, Staff staffRecord) {
        LocalDateTime appointmentDateTime;

        try {
            appointmentDateTime = LocalDateTime.parse(date + " " + time, APPOINTMENT_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment date and time are invalid");
        }

        if (appointmentDateTime.isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Appointments must be scheduled at least 1 hour in advance"
            );
        }

        if (staffRecord != null && !staffAvailabilityService.isWithinActiveAvailability(
            staffRecord.getId(),
            appointmentDateTime.toLocalDate(),
            appointmentDateTime.toLocalTime()
        )) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Selected appointment time is outside this staff member's availability"
            );
        }
    }
}
