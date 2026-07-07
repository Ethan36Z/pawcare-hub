package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.admin.AdminBookingResponse;
import com.pawcarehub.backend.dto.admin.CompleteBookingRequest;
import com.pawcarehub.backend.dto.admin.UpdateAdminBookingScheduleRequest;
import com.pawcarehub.backend.entity.Booking;
import com.pawcarehub.backend.entity.Pet;
import com.pawcarehub.backend.entity.PetMedicalNote;
import com.pawcarehub.backend.entity.Staff;
import com.pawcarehub.backend.entity.StaffScheduleException;
import com.pawcarehub.backend.repository.BookingRepository;
import com.pawcarehub.backend.repository.PetMedicalNoteRepository;
import com.pawcarehub.backend.repository.PetRepository;
import com.pawcarehub.backend.repository.StaffAvailabilityRepository;
import com.pawcarehub.backend.repository.StaffRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminBookingService {
    private static final DateTimeFormatter BOOKING_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.US);
    private static final DateTimeFormatter BOOKING_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Set<String> EDITABLE_STATUSES = Set.of("upcoming", "confirmed");
    private static final Set<String> ACTIVE_BOOKING_STATUSES = Set.of("upcoming", "confirmed");
    private static final Pattern DURATION_NUMBER_PATTERN = Pattern.compile("(\\d+)");
    private static final int FALLBACK_APPOINTMENT_MINUTES = 30;
    private static final LocalTime CLINIC_OPEN_TIME = LocalTime.of(9, 0);
    private static final LocalTime CLINIC_CLOSE_TIME = LocalTime.of(17, 0);

    private final BookingRepository bookingRepository;
    private final PetRepository petRepository;
    private final PetMedicalNoteRepository petMedicalNoteRepository;
    private final StaffRepository staffRepository;
    private final StaffAvailabilityRepository staffAvailabilityRepository;
    private final StaffScheduleExceptionService staffScheduleExceptionService;

    public AdminBookingService(
        BookingRepository bookingRepository,
        PetRepository petRepository,
        PetMedicalNoteRepository petMedicalNoteRepository,
        StaffRepository staffRepository,
        StaffAvailabilityRepository staffAvailabilityRepository,
        StaffScheduleExceptionService staffScheduleExceptionService
    ) {
        this.bookingRepository = bookingRepository;
        this.petRepository = petRepository;
        this.petMedicalNoteRepository = petMedicalNoteRepository;
        this.staffRepository = staffRepository;
        this.staffAvailabilityRepository = staffAvailabilityRepository;
        this.staffScheduleExceptionService = staffScheduleExceptionService;
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

        if ("Cancelled".equalsIgnoreCase(booking.getStatus()) || "Completed".equalsIgnoreCase(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This booking cannot be confirmed");
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

        if ("Completed".equalsIgnoreCase(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed bookings cannot be cancelled");
        }

        booking.setStatus("Cancelled");
        Booking savedBooking = bookingRepository.save(booking);
        return toAdminBookingResponse(savedBooking);
    }

    @Transactional
    public AdminBookingResponse updateBookingSchedule(Long bookingId, UpdateAdminBookingScheduleRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (!isEditableStatus(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only Upcoming or Confirmed bookings can be edited");
        }

        Staff staff = resolveActiveStaff(request.staffId());
        LocalDate appointmentDate = parseRequestedDate(request.date());
        LocalTime appointmentStart = parseRequestedTime(request.time());
        int durationMinutes = resolveDurationMinutes(booking);
        LocalTime appointmentEnd = appointmentStart.plusMinutes(durationMinutes);

        validateAdvanceBookingRule(appointmentDate, appointmentStart);
        validateClinicHours(appointmentDate, appointmentStart, appointmentEnd);
        validateStaffAvailability(staff.getId(), appointmentDate, appointmentStart, appointmentEnd);
        validateNoStaffOverlap(booking.getId(), staff, appointmentDate, appointmentStart, appointmentEnd);

        booking.setStaffRecord(staff);
        booking.setDate(appointmentDate.format(BOOKING_DATE_FORMATTER));
        booking.setTime(appointmentStart.format(BOOKING_TIME_FORMATTER));

        return toAdminBookingResponse(bookingRepository.save(booking));
    }

    @Transactional
    public AdminBookingResponse completeBooking(Long bookingId, CompleteBookingRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if ("Cancelled".equalsIgnoreCase(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancelled bookings cannot be completed");
        }

        booking.setStatus("Completed");
        booking.setVisitSummary(normalizeOptionalField(request.visitSummary()));
        booking.setDiagnosisAssessment(normalizeOptionalField(request.diagnosisAssessment()));
        booking.setTreatmentRecommendation(normalizeOptionalField(request.treatmentRecommendation()));
        booking.setFollowUpNote(normalizeOptionalField(request.followUpNote()));

        Booking savedBooking = bookingRepository.save(booking);
        syncPetMedicalNote(savedBooking);
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
            booking.getServiceRecord() != null ? booking.getServiceRecord().getId() : null,
            booking.getDate(),
            booking.getTime(),
            booking.getStatus(),
            booking.getClinic(),
            booking.getResolvedStaffName(),
            booking.getStaffRecord() != null ? booking.getStaffRecord().getId() : null,
            booking.getOwner().getId(),
            booking.getOwner().getName(),
            booking.getOwner().getEmail(),
            booking.getOwnerNote(),
            booking.getVisitSummary(),
            booking.getDiagnosisAssessment(),
            booking.getTreatmentRecommendation(),
            booking.getFollowUpNote()
        );
    }

    private void syncPetMedicalNote(Booking booking) {
        if (!hasOutcomeContent(booking)) {
            return;
        }

        Pet pet = petRepository.findFirstByOwnerIdAndNameIgnoreCase(booking.getOwner().getId(), booking.getPetName())
            .orElse(null);
        if (pet == null) {
            return;
        }

        String noteText = buildOutcomeNoteText(booking);
        PetMedicalNote existingNote = petMedicalNoteRepository.findFirstByRelatedBookingId(booking.getId()).orElse(null);

        if (existingNote != null) {
            existingNote.setNoteDate(resolveNoteDate(booking));
            existingNote.setAuthor(booking.getResolvedStaffName());
            existingNote.setNoteText(noteText);
            petMedicalNoteRepository.save(existingNote);
            return;
        }

        petMedicalNoteRepository.save(new PetMedicalNote(
            pet,
            resolveNoteDate(booking),
            booking.getResolvedStaffName(),
            booking.getId(),
            noteText
        ));
    }

    private boolean hasOutcomeContent(Booking booking) {
        return StringUtils.hasText(booking.getVisitSummary())
            || StringUtils.hasText(booking.getDiagnosisAssessment())
            || StringUtils.hasText(booking.getTreatmentRecommendation())
            || StringUtils.hasText(booking.getFollowUpNote());
    }

    private LocalDate resolveNoteDate(Booking booking) {
        try {
            return LocalDate.parse(booking.getDate(), BOOKING_DATE_FORMATTER);
        } catch (Exception exception) {
            return LocalDate.now();
        }
    }

    private String buildOutcomeNoteText(Booking booking) {
        StringBuilder builder = new StringBuilder("Completed visit outcome");

        appendOutcomeSection(builder, "Visit summary", booking.getVisitSummary());
        appendOutcomeSection(builder, "Assessment", booking.getDiagnosisAssessment());
        appendOutcomeSection(builder, "Treatment", booking.getTreatmentRecommendation());
        appendOutcomeSection(builder, "Follow-up", booking.getFollowUpNote());

        return builder.toString();
    }

    private void appendOutcomeSection(StringBuilder builder, String label, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }

        builder.append("\n\n").append(label).append(": ").append(value.trim());
    }

    private String normalizeOptionalField(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private boolean isEditableStatus(String status) {
        return status != null && EDITABLE_STATUSES.contains(status.trim().toLowerCase(Locale.US));
    }

    private boolean isActiveBooking(Booking booking) {
        return booking.getStatus() != null && ACTIVE_BOOKING_STATUSES.contains(booking.getStatus().trim().toLowerCase(Locale.US));
    }

    private Staff resolveActiveStaff(Long staffId) {
        if (staffId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned staff is required");
        }

        return staffRepository.findByIdAndActiveTrue(staffId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected staff member is not available"));
    }

    private LocalDate parseRequestedDate(String value) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment date is required");
        }

        String normalized = value.trim();
        try {
            return LocalDate.parse(normalized, ISO_DATE_FORMATTER);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(normalized, BOOKING_DATE_FORMATTER);
            } catch (DateTimeParseException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment date is invalid");
            }
        }
    }

    private LocalTime parseRequestedTime(String value) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment time is required");
        }

        String normalized = value.trim();
        try {
            return LocalTime.parse(normalized);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalTime.parse(normalized.toUpperCase(Locale.US), BOOKING_TIME_FORMATTER);
            } catch (DateTimeParseException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment time is invalid");
            }
        }
    }

    private void validateAdvanceBookingRule(LocalDate date, LocalTime time) {
        if (LocalDateTime.of(date, time).isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointments must be scheduled at least 1 hour in advance");
        }
    }

    private void validateClinicHours(LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment is outside clinic operating hours");
        }

        if (startTime.isBefore(CLINIC_OPEN_TIME) || endTime.isAfter(CLINIC_CLOSE_TIME)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment is outside clinic operating hours");
        }
    }

    private void validateStaffAvailability(Long staffId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        StaffScheduleException scheduleException = staffScheduleExceptionService.findException(staffId, date);
        if (scheduleException != null) {
            if (!scheduleException.isAvailable() ||
                startTime.isBefore(scheduleException.getCustomStartTime()) ||
                endTime.isAfter(scheduleException.getCustomEndTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected appointment time is outside this staff member's availability");
            }
            return;
        }

        boolean contained = staffAvailabilityRepository.findByStaffIdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(
                staffId,
                date.getDayOfWeek()
            ).stream()
            .anyMatch(availability ->
                !startTime.isBefore(availability.getStartTime()) &&
                    !endTime.isAfter(availability.getEndTime())
            );

        if (!contained) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected appointment time is outside this staff member's availability");
        }
    }

    private void validateNoStaffOverlap(
        Long bookingId,
        Staff staff,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
    ) {
        boolean overlaps = bookingRepository.findAll().stream()
            .filter(booking -> !booking.getId().equals(bookingId))
            .filter(this::isActiveBooking)
            .filter(booking -> bookingAssignedToStaff(booking, staff))
            .filter(booking -> date.equals(parseExistingBookingDate(booking)))
            .anyMatch(booking -> {
                LocalTime existingStart = parseExistingBookingTime(booking);
                if (existingStart == null) {
                    return false;
                }

                LocalTime existingEnd = existingStart.plusMinutes(resolveDurationMinutes(booking));
                return startTime.isBefore(existingEnd) && endTime.isAfter(existingStart);
            });

        if (overlaps) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected staff already has an active booking at this time");
        }
    }

    private boolean bookingAssignedToStaff(Booking booking, Staff staff) {
        if (booking.getStaffRecord() != null) {
            return staff.getId().equals(booking.getStaffRecord().getId());
        }

        return StringUtils.hasText(booking.getStaff()) && booking.getStaff().trim().equalsIgnoreCase(staff.getName());
    }

    private LocalDate parseExistingBookingDate(Booking booking) {
        try {
            return LocalDate.parse(booking.getDate(), BOOKING_DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private LocalTime parseExistingBookingTime(Booking booking) {
        try {
            return LocalTime.parse(booking.getTime(), BOOKING_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private int resolveDurationMinutes(Booking booking) {
        String duration = booking.getServiceRecord() != null ? booking.getServiceRecord().getDuration() : null;
        if (!StringUtils.hasText(duration)) {
            return FALLBACK_APPOINTMENT_MINUTES;
        }

        Matcher matcher = DURATION_NUMBER_PATTERN.matcher(duration);
        if (!matcher.find()) {
            return FALLBACK_APPOINTMENT_MINUTES;
        }

        int value = Integer.parseInt(matcher.group(1));
        return duration.toLowerCase(Locale.US).contains("hour") ? value * 60 : value;
    }
}
