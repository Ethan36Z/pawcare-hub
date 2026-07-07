package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.admin.UpsertStaffAvailabilityRequest;
import com.pawcarehub.backend.dto.admin.UpsertStaffScheduleExceptionRequest;
import com.pawcarehub.backend.dto.scheduling.BookingReassignmentRequest;
import com.pawcarehub.backend.dto.scheduling.EligibleReplacementStaffResponse;
import com.pawcarehub.backend.dto.scheduling.ScheduleBookingConflictItemResponse;
import com.pawcarehub.backend.dto.scheduling.ScheduleBookingConflictResponse;
import com.pawcarehub.backend.entity.Booking;
import com.pawcarehub.backend.entity.Staff;
import com.pawcarehub.backend.entity.StaffAvailability;
import com.pawcarehub.backend.entity.StaffScheduleException;
import com.pawcarehub.backend.repository.BookingRepository;
import com.pawcarehub.backend.repository.StaffAvailabilityRepository;
import com.pawcarehub.backend.repository.StaffRepository;
import com.pawcarehub.backend.repository.StaffScheduleExceptionRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SchedulingConflictService {
    public static final String CONFLICT_CODE = "SCHEDULE_BOOKING_CONFLICT";

    private static final DateTimeFormatter BOOKING_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.US);
    private static final DateTimeFormatter BOOKING_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final Set<String> ACTIVE_BOOKING_STATUSES = Set.of("upcoming", "confirmed");
    private static final int FALLBACK_APPOINTMENT_MINUTES = 30;
    private static final Pattern DURATION_NUMBER_PATTERN = Pattern.compile("(\\d+)");

    private final BookingRepository bookingRepository;
    private final StaffRepository staffRepository;
    private final StaffAvailabilityRepository staffAvailabilityRepository;
    private final StaffScheduleExceptionRepository staffScheduleExceptionRepository;

    public SchedulingConflictService(
        BookingRepository bookingRepository,
        StaffRepository staffRepository,
        StaffAvailabilityRepository staffAvailabilityRepository,
        StaffScheduleExceptionRepository staffScheduleExceptionRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.staffRepository = staffRepository;
        this.staffAvailabilityRepository = staffAvailabilityRepository;
        this.staffScheduleExceptionRepository = staffScheduleExceptionRepository;
    }

    public void assertNoScheduleExceptionConflicts(Long staffId, UpsertStaffScheduleExceptionRequest request) {
        List<ScheduleBookingConflictItemResponse> conflicts = findScheduleExceptionConflicts(staffId, request);
        throwIfConflicts(conflicts);
    }

    public void assertNoScheduleExceptionDeleteConflicts(Long staffId, StaffScheduleException scheduleException) {
        List<ScheduleBookingConflictItemResponse> conflicts = findDateConflicts(
            staffId,
            scheduleException.getDate(),
            weeklyIntervals(staffId, scheduleException.getDate().getDayOfWeek())
        );
        throwIfConflicts(conflicts);
    }

    public void assertNoAvailabilityUpdateConflicts(
        Long staffId,
        Long availabilityId,
        UpsertStaffAvailabilityRequest request
    ) {
        List<ScheduleBookingConflictItemResponse> conflicts = findAvailabilityConflicts(
            staffId,
            availabilityId,
            AvailabilityChange.UPDATE,
            request
        );
        throwIfConflicts(conflicts);
    }

    public void assertNoAvailabilityToggleConflicts(Long staffId, StaffAvailability availability) {
        UpsertStaffAvailabilityRequest request = new UpsertStaffAvailabilityRequest(
            availability.getDayOfWeek().name(),
            availability.getStartTime().toString(),
            availability.getEndTime().toString(),
            !availability.isActive()
        );
        List<ScheduleBookingConflictItemResponse> conflicts = findAvailabilityConflicts(
            staffId,
            availability.getId(),
            AvailabilityChange.UPDATE,
            request
        );
        throwIfConflicts(conflicts);
    }

    public void assertNoAvailabilityDeleteConflicts(Long staffId, Long availabilityId) {
        List<ScheduleBookingConflictItemResponse> conflicts = findAvailabilityConflicts(
            staffId,
            availabilityId,
            AvailabilityChange.DELETE,
            null
        );
        throwIfConflicts(conflicts);
    }

    public void reassignAndValidateScheduleException(
        Long staffId,
        UpsertStaffScheduleExceptionRequest request,
        List<BookingReassignmentRequest> reassignments
    ) {
        List<ScheduleBookingConflictItemResponse> conflicts = findScheduleExceptionConflicts(staffId, request);
        reassignConflictingBookings(conflicts, reassignments);
    }

    public void reassignAndValidateAvailabilityUpdate(
        Long staffId,
        Long availabilityId,
        UpsertStaffAvailabilityRequest request,
        List<BookingReassignmentRequest> reassignments
    ) {
        List<ScheduleBookingConflictItemResponse> conflicts = findAvailabilityConflicts(
            staffId,
            availabilityId,
            AvailabilityChange.UPDATE,
            request
        );
        reassignConflictingBookings(conflicts, reassignments);
    }

    private List<ScheduleBookingConflictItemResponse> findScheduleExceptionConflicts(
        Long staffId,
        UpsertStaffScheduleExceptionRequest request
    ) {
        LocalDate date = parseDate(request.date());
        return findDateConflicts(staffId, date, intervalsForExceptionRequest(request));
    }

    private List<ScheduleBookingConflictItemResponse> findAvailabilityConflicts(
        Long staffId,
        Long availabilityId,
        AvailabilityChange change,
        UpsertStaffAvailabilityRequest request
    ) {
        StaffAvailability existingAvailability = availabilityId != null
            ? staffAvailabilityRepository.findByIdAndStaffId(availabilityId, staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Availability slot not found"))
            : null;
        DayOfWeek requestDay = request != null ? parseDayOfWeek(request.dayOfWeek()) : null;

        return activeBookingsForStaff(staffId).stream()
            .filter(booking -> {
                LocalDate date = parseBookingDate(booking);
                if (date == null) {
                    return false;
                }

                if (staffScheduleExceptionRepository.findByStaffIdAndDate(staffId, date).isPresent()) {
                    return false;
                }

                DayOfWeek bookingDay = date.getDayOfWeek();
                return bookingDay == requestDay ||
                    (existingAvailability != null && bookingDay == existingAvailability.getDayOfWeek());
            })
            .filter(booking -> !bookingFits(booking, proposedWeeklyIntervals(
                staffId,
                parseBookingDate(booking).getDayOfWeek(),
                availabilityId,
                change,
                request
            )))
            .map(booking -> toConflictResponse(booking))
            .toList();
    }

    private List<ScheduleBookingConflictItemResponse> findDateConflicts(
        Long staffId,
        LocalDate date,
        List<TimeInterval> proposedIntervals
    ) {
        return activeBookingsForStaff(staffId).stream()
            .filter(booking -> date.equals(parseBookingDate(booking)))
            .filter(booking -> !bookingFits(booking, proposedIntervals))
            .map(booking -> toConflictResponse(booking))
            .toList();
    }

    private List<Booking> activeBookingsForStaff(Long staffId) {
        Staff staff = staffRepository.findById(staffId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff member not found"));

        return bookingRepository.findAll().stream()
            .filter(booking -> isBookingAssignedToStaff(booking, staff))
            .filter(this::isActiveBooking)
            .toList();
    }

    private boolean bookingFits(Booking booking, List<TimeInterval> intervals) {
        LocalTime startTime = parseBookingTime(booking);
        if (startTime == null) {
            return false;
        }

        LocalTime endTime = startTime.plusMinutes(resolveDurationMinutes(booking));
        return intervals.stream().anyMatch(interval -> interval.contains(startTime, endTime));
    }

    private ScheduleBookingConflictItemResponse toConflictResponse(Booking booking) {
        LocalDate date = parseBookingDate(booking);
        LocalTime startTime = parseBookingTime(booking);
        LocalTime endTime = startTime != null ? startTime.plusMinutes(resolveDurationMinutes(booking)) : null;
        Staff currentStaff = resolveBookingStaff(booking);

        return new ScheduleBookingConflictItemResponse(
            booking.getId(),
            date != null ? date.toString() : booking.getDate(),
            startTime != null ? startTime.toString() : null,
            endTime != null ? endTime.toString() : null,
            booking.getPetName(),
            booking.getOwner().getName(),
            booking.getOwner().getEmail(),
            booking.getResolvedServiceName(),
            currentStaff != null ? currentStaff.getId() : null,
            booking.getResolvedStaffName(),
            booking.getStatus(),
            eligibleReplacementsFor(booking)
        );
    }

    private List<EligibleReplacementStaffResponse> eligibleReplacementsFor(Booking booking) {
        Staff currentStaff = resolveBookingStaff(booking);
        LocalDate date = parseBookingDate(booking);
        LocalTime startTime = parseBookingTime(booking);
        if (currentStaff == null || date == null || startTime == null) {
            return List.of();
        }

        LocalTime endTime = startTime.plusMinutes(resolveDurationMinutes(booking));
        return staffRepository.findByActiveTrueOrderByNameAsc().stream()
            .filter(staff -> !staff.getId().equals(currentStaff.getId()))
            .filter(staff -> rolesCompatible(currentStaff.getRole(), staff.getRole()))
            .filter(staff -> currentEffectiveIntervals(staff.getId(), date).stream()
                .anyMatch(interval -> interval.contains(startTime, endTime)))
            .filter(staff -> hasNoOverlappingBooking(staff.getId(), booking.getId(), date, startTime, endTime))
            .map(staff -> new EligibleReplacementStaffResponse(staff.getId(), staff.getName(), staff.getRole()))
            .toList();
    }

    private void reassignConflictingBookings(
        List<ScheduleBookingConflictItemResponse> conflicts,
        List<BookingReassignmentRequest> reassignments
    ) {
        if (conflicts.isEmpty()) {
            return;
        }

        Map<Long, Long> replacementsByBookingId = reassignments == null
            ? Map.of()
            : reassignments.stream()
                .filter(request -> request.bookingId() != null && request.replacementStaffId() != null)
                .collect(Collectors.toMap(
                    BookingReassignmentRequest::bookingId,
                    BookingReassignmentRequest::replacementStaffId,
                    (first, second) -> second
                ));

        boolean missingReplacement = conflicts.stream()
            .anyMatch(conflict -> !replacementsByBookingId.containsKey(conflict.bookingId()));
        if (missingReplacement) {
            throwIfConflicts(conflicts);
        }

        for (ScheduleBookingConflictItemResponse conflict : conflicts) {
            Booking booking = bookingRepository.findById(conflict.bookingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Affected booking no longer exists"));
            if (!isActiveBooking(booking)) {
                continue;
            }

            Long replacementStaffId = replacementsByBookingId.get(booking.getId());
            Staff replacementStaff = staffRepository.findByIdAndActiveTrue(replacementStaffId)
                .orElseThrow(() -> new ScheduleBookingConflictException(buildConflictResponse(conflicts)));
            boolean eligible = eligibleReplacementsFor(booking).stream()
                .anyMatch(candidate -> candidate.staffId().equals(replacementStaff.getId()));
            if (!eligible) {
                throwIfConflicts(conflicts);
            }

            booking.setStaffRecord(replacementStaff);
            bookingRepository.save(booking);
        }
    }

    private boolean hasNoOverlappingBooking(
        Long staffId,
        Long bookingId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
    ) {
        return bookingRepository.findAll().stream()
            .filter(booking -> {
                Staff staff = staffRepository.findById(staffId).orElse(null);
                return staff != null && isBookingAssignedToStaff(booking, staff);
            })
            .filter(booking -> !Objects.equals(booking.getId(), bookingId))
            .filter(this::isActiveBooking)
            .filter(booking -> date.equals(parseBookingDate(booking)))
            .noneMatch(booking -> {
                LocalTime existingStart = parseBookingTime(booking);
                if (existingStart == null) {
                    return false;
                }

                LocalTime existingEnd = existingStart.plusMinutes(resolveDurationMinutes(booking));
                return startTime.isBefore(existingEnd) && endTime.isAfter(existingStart);
            });
    }

    private boolean isBookingAssignedToStaff(Booking booking, Staff staff) {
        if (booking.getStaffRecord() != null) {
            return staff.getId().equals(booking.getStaffRecord().getId());
        }

        return StringUtils.hasText(booking.getStaff()) && booking.getStaff().trim().equalsIgnoreCase(staff.getName());
    }

    private Staff resolveBookingStaff(Booking booking) {
        if (booking.getStaffRecord() != null) {
            return booking.getStaffRecord();
        }

        if (!StringUtils.hasText(booking.getStaff())) {
            return null;
        }

        return staffRepository.findFirstByNameIgnoreCaseAndActiveTrue(booking.getStaff().trim()).orElse(null);
    }

    private List<TimeInterval> currentEffectiveIntervals(Long staffId, LocalDate date) {
        StaffScheduleException exception = staffScheduleExceptionRepository.findByStaffIdAndDate(staffId, date).orElse(null);
        if (exception != null) {
            if (!exception.isAvailable()) {
                return List.of();
            }

            return List.of(new TimeInterval(exception.getCustomStartTime(), exception.getCustomEndTime()));
        }

        return weeklyIntervals(staffId, date.getDayOfWeek());
    }

    private List<TimeInterval> proposedWeeklyIntervals(
        Long staffId,
        DayOfWeek dayOfWeek,
        Long availabilityId,
        AvailabilityChange change,
        UpsertStaffAvailabilityRequest request
    ) {
        return staffAvailabilityRepository.findByStaffIdAndDayOfWeekOrderByStartTimeAsc(staffId, dayOfWeek).stream()
            .filter(availability -> change != AvailabilityChange.DELETE || !availability.getId().equals(availabilityId))
            .map(availability -> {
                if (change == AvailabilityChange.UPDATE && availability.getId().equals(availabilityId)) {
                    DayOfWeek requestDay = parseDayOfWeek(request.dayOfWeek());
                    if (requestDay != dayOfWeek || Boolean.FALSE.equals(request.active())) {
                        return null;
                    }

                    return new TimeInterval(parseTime(request.startTime(), "startTime"), parseTime(request.endTime(), "endTime"));
                }

                return availability.isActive()
                    ? new TimeInterval(availability.getStartTime(), availability.getEndTime())
                    : null;
            })
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(TimeInterval::startTime))
            .toList();
    }

    private List<TimeInterval> weeklyIntervals(Long staffId, DayOfWeek dayOfWeek) {
        return staffAvailabilityRepository.findByStaffIdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(staffId, dayOfWeek).stream()
            .map(availability -> new TimeInterval(availability.getStartTime(), availability.getEndTime()))
            .toList();
    }

    private List<TimeInterval> intervalsForExceptionRequest(UpsertStaffScheduleExceptionRequest request) {
        boolean available = request.available() == null || request.available();
        if (!available) {
            return List.of();
        }

        return List.of(new TimeInterval(
            parseTime(request.customStartTime(), "customStartTime"),
            parseTime(request.customEndTime(), "customEndTime")
        ));
    }

    private void throwIfConflicts(List<ScheduleBookingConflictItemResponse> conflicts) {
        if (!conflicts.isEmpty()) {
            throw new ScheduleBookingConflictException(buildConflictResponse(conflicts));
        }
    }

    private ScheduleBookingConflictResponse buildConflictResponse(List<ScheduleBookingConflictItemResponse> conflicts) {
        return new ScheduleBookingConflictResponse(
            CONFLICT_CODE,
            "This schedule change affects existing appointments.",
            conflicts
        );
    }

    private boolean isActiveBooking(Booking booking) {
        return booking.getStatus() != null && ACTIVE_BOOKING_STATUSES.contains(booking.getStatus().trim().toLowerCase());
    }

    private boolean rolesCompatible(String currentRole, String candidateRole) {
        String currentGroup = clinicalRoleGroup(currentRole);
        String candidateGroup = clinicalRoleGroup(candidateRole);
        return currentGroup != null && currentGroup.equals(candidateGroup);
    }

    private String clinicalRoleGroup(String role) {
        if (!StringUtils.hasText(role)) {
            return null;
        }

        String normalized = role.toLowerCase(Locale.US);
        if (normalized.contains("doctor") || normalized.contains("veterinarian") || normalized.contains("vet")) {
            return "doctor";
        }

        if (normalized.contains("nurse") || normalized.contains("technician") || normalized.contains("tech")) {
            return "nurse";
        }

        return null;
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

    private LocalDate parseBookingDate(Booking booking) {
        try {
            return LocalDate.parse(booking.getDate(), BOOKING_DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private LocalTime parseBookingTime(Booking booking) {
        try {
            return LocalTime.parse(booking.getTime(), BOOKING_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date is invalid");
        }
    }

    private DayOfWeek parseDayOfWeek(String value) {
        try {
            return DayOfWeek.valueOf(value.trim().toUpperCase(Locale.US));
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dayOfWeek is invalid");
        }
    }

    private LocalTime parseTime(String value, String fieldName) {
        try {
            return LocalTime.parse(value.trim());
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is invalid");
        }
    }

    private enum AvailabilityChange {
        UPDATE,
        DELETE
    }

    private record TimeInterval(LocalTime startTime, LocalTime endTime) {
        boolean contains(LocalTime appointmentStart, LocalTime appointmentEnd) {
            return !appointmentStart.isBefore(startTime) && !appointmentEnd.isAfter(endTime);
        }
    }
}
