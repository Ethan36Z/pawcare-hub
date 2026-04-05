package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.admin.UpsertStaffAvailabilityRequest;
import com.pawcarehub.backend.dto.staffavailability.ResolvedStaffAvailabilityResponse;
import com.pawcarehub.backend.dto.staffavailability.StaffAvailabilityResponse;
import com.pawcarehub.backend.entity.Staff;
import com.pawcarehub.backend.entity.StaffAvailability;
import com.pawcarehub.backend.entity.StaffScheduleException;
import com.pawcarehub.backend.repository.StaffAvailabilityRepository;
import com.pawcarehub.backend.repository.StaffRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StaffAvailabilityService {

    private final StaffRepository staffRepository;
    private final StaffAvailabilityRepository staffAvailabilityRepository;
    private final StaffScheduleExceptionService staffScheduleExceptionService;

    public StaffAvailabilityService(
        StaffRepository staffRepository,
        StaffAvailabilityRepository staffAvailabilityRepository,
        StaffScheduleExceptionService staffScheduleExceptionService
    ) {
        this.staffRepository = staffRepository;
        this.staffAvailabilityRepository = staffAvailabilityRepository;
        this.staffScheduleExceptionService = staffScheduleExceptionService;
    }

    @Transactional(readOnly = true)
    public List<StaffAvailabilityResponse> getStaffAvailability(Long staffId) {
        getStaff(staffId);

        return staffAvailabilityRepository.findByStaffIdOrderByDayOfWeekAscStartTimeAsc(staffId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ResolvedStaffAvailabilityResponse> getBookableAvailability(Long staffId, LocalDate date) {
        Staff staff = staffRepository.findByIdAndActiveTrue(staffId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff member not found"));

        StaffScheduleException scheduleException = staffScheduleExceptionService.findException(staff.getId(), date);
        if (scheduleException != null) {
            if (!scheduleException.isAvailable()) {
                return List.of();
            }

            return List.of(toResolvedResponse(
                staff.getId(),
                date,
                scheduleException.getCustomStartTime(),
                scheduleException.getCustomEndTime(),
                "exception"
            ));
        }

        return staffAvailabilityRepository.findByStaffIdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(
            staff.getId(),
            date.getDayOfWeek()
        ).stream()
            .map(availability -> toResolvedResponse(
                staff.getId(),
                date,
                availability.getStartTime(),
                availability.getEndTime(),
                "weekly"
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public boolean isWithinActiveAvailability(Long staffId, LocalDate appointmentDate, LocalTime appointmentTime) {
        StaffScheduleException scheduleException = staffScheduleExceptionService.findException(staffId, appointmentDate);
        if (scheduleException != null) {
            if (!scheduleException.isAvailable()) {
                return false;
            }

            return !appointmentTime.isBefore(scheduleException.getCustomStartTime()) &&
                appointmentTime.isBefore(scheduleException.getCustomEndTime());
        }

        return staffAvailabilityRepository.findByStaffIdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(
            staffId,
            appointmentDate.getDayOfWeek()
        ).stream()
            .anyMatch(availability ->
                !appointmentTime.isBefore(availability.getStartTime()) &&
                appointmentTime.isBefore(availability.getEndTime())
            );
    }

    @Transactional
    public StaffAvailabilityResponse createAvailability(Long staffId, UpsertStaffAvailabilityRequest request) {
        Staff staff = getStaff(staffId);
        DayOfWeek dayOfWeek = parseDayOfWeek(request.dayOfWeek());
        LocalTime startTime = parseRequiredTime(request.startTime(), "startTime");
        LocalTime endTime = parseRequiredTime(request.endTime(), "endTime");

        validateTimeRange(startTime, endTime);
        validateOverlaps(staffId, null, dayOfWeek, startTime, endTime);

        StaffAvailability savedAvailability = staffAvailabilityRepository.save(new StaffAvailability(
            staff,
            dayOfWeek,
            startTime,
            endTime,
            request.active() == null || request.active()
        ));

        return toResponse(savedAvailability);
    }

    @Transactional
    public StaffAvailabilityResponse updateAvailability(
        Long staffId,
        Long availabilityId,
        UpsertStaffAvailabilityRequest request
    ) {
        StaffAvailability availability = getAvailability(staffId, availabilityId);
        DayOfWeek dayOfWeek = parseDayOfWeek(request.dayOfWeek());
        LocalTime startTime = parseRequiredTime(request.startTime(), "startTime");
        LocalTime endTime = parseRequiredTime(request.endTime(), "endTime");

        validateTimeRange(startTime, endTime);
        validateOverlaps(staffId, availabilityId, dayOfWeek, startTime, endTime);

        availability.setDayOfWeek(dayOfWeek);
        availability.setStartTime(startTime);
        availability.setEndTime(endTime);
        availability.setActive(request.active() == null || request.active());

        return toResponse(staffAvailabilityRepository.save(availability));
    }

    @Transactional
    public StaffAvailabilityResponse toggleAvailability(Long staffId, Long availabilityId) {
        StaffAvailability availability = getAvailability(staffId, availabilityId);
        availability.setActive(!availability.isActive());
        return toResponse(staffAvailabilityRepository.save(availability));
    }

    @Transactional
    public void deleteAvailability(Long staffId, Long availabilityId) {
        StaffAvailability availability = getAvailability(staffId, availabilityId);
        staffAvailabilityRepository.delete(availability);
    }

    private Staff getStaff(Long staffId) {
        return staffRepository.findById(staffId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff member not found"));
    }

    private StaffAvailability getAvailability(Long staffId, Long availabilityId) {
        return staffAvailabilityRepository.findByIdAndStaffId(availabilityId, staffId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Availability slot not found"));
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startTime must be before endTime");
        }
    }

    private void validateOverlaps(
        Long staffId,
        Long availabilityId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
    ) {
        boolean overlaps = staffAvailabilityRepository.findByStaffIdAndDayOfWeekOrderByStartTimeAsc(staffId, dayOfWeek).stream()
            .filter(existing -> availabilityId == null || !existing.getId().equals(availabilityId))
            .anyMatch(existing ->
                startTime.isBefore(existing.getEndTime()) &&
                endTime.isAfter(existing.getStartTime())
            );

        if (overlaps) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Availability slots cannot overlap for the same staff member and day"
            );
        }
    }

    private DayOfWeek parseDayOfWeek(String value) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dayOfWeek is required");
        }

        try {
            return DayOfWeek.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dayOfWeek is invalid");
        }
    }

    private LocalTime parseRequiredTime(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }

        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is invalid");
        }
    }

    private StaffAvailabilityResponse toResponse(StaffAvailability availability) {
        return new StaffAvailabilityResponse(
            availability.getId(),
            availability.getStaff().getId(),
            availability.getDayOfWeek().name(),
            availability.getStartTime().toString(),
            availability.getEndTime().toString(),
            availability.isActive()
        );
    }

    private ResolvedStaffAvailabilityResponse toResolvedResponse(
        Long staffId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String source
    ) {
        return new ResolvedStaffAvailabilityResponse(
            staffId,
            date.toString(),
            startTime.toString(),
            endTime.toString(),
            source
        );
    }
}
