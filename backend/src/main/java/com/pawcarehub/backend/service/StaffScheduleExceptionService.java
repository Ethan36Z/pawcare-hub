package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.admin.UpsertStaffScheduleExceptionRequest;
import com.pawcarehub.backend.dto.staffavailability.StaffScheduleExceptionResponse;
import com.pawcarehub.backend.entity.Staff;
import com.pawcarehub.backend.entity.StaffScheduleException;
import com.pawcarehub.backend.repository.StaffRepository;
import com.pawcarehub.backend.repository.StaffScheduleExceptionRepository;
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
public class StaffScheduleExceptionService {

    private final StaffRepository staffRepository;
    private final StaffScheduleExceptionRepository staffScheduleExceptionRepository;

    public StaffScheduleExceptionService(
        StaffRepository staffRepository,
        StaffScheduleExceptionRepository staffScheduleExceptionRepository
    ) {
        this.staffRepository = staffRepository;
        this.staffScheduleExceptionRepository = staffScheduleExceptionRepository;
    }

    @Transactional(readOnly = true)
    public List<StaffScheduleExceptionResponse> getScheduleExceptions(Long staffId) {
        getStaff(staffId);

        return staffScheduleExceptionRepository.findByStaffIdOrderByDateAsc(staffId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public StaffScheduleException findException(Long staffId, LocalDate date) {
        return staffScheduleExceptionRepository.findByStaffIdAndDate(staffId, date).orElse(null);
    }

    @Transactional
    public StaffScheduleExceptionResponse createScheduleException(
        Long staffId,
        UpsertStaffScheduleExceptionRequest request
    ) {
        Staff staff = getStaff(staffId);
        LocalDate date = parseRequiredDate(request.date());

        if (staffScheduleExceptionRepository.existsByStaffIdAndDate(staffId, date)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A schedule exception already exists for this staff member and date");
        }

        ScheduleExceptionValues values = validateAndNormalize(request);
        StaffScheduleException savedException = staffScheduleExceptionRepository.save(new StaffScheduleException(
            staff,
            date,
            values.available(),
            values.customStartTime(),
            values.customEndTime()
        ));

        return toResponse(savedException);
    }

    @Transactional
    public StaffScheduleExceptionResponse updateScheduleException(
        Long staffId,
        Long exceptionId,
        UpsertStaffScheduleExceptionRequest request
    ) {
        StaffScheduleException scheduleException = getScheduleException(staffId, exceptionId);
        LocalDate date = parseRequiredDate(request.date());

        if (staffScheduleExceptionRepository.existsByStaffIdAndDateAndIdNot(staffId, date, exceptionId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A schedule exception already exists for this staff member and date");
        }

        ScheduleExceptionValues values = validateAndNormalize(request);
        scheduleException.setDate(date);
        scheduleException.setAvailable(values.available());
        scheduleException.setCustomStartTime(values.customStartTime());
        scheduleException.setCustomEndTime(values.customEndTime());

        return toResponse(staffScheduleExceptionRepository.save(scheduleException));
    }

    @Transactional
    public void deleteScheduleException(Long staffId, Long exceptionId) {
        staffScheduleExceptionRepository.delete(getScheduleException(staffId, exceptionId));
    }

    private ScheduleExceptionValues validateAndNormalize(UpsertStaffScheduleExceptionRequest request) {
        boolean available = request.available() == null || request.available();
        LocalTime customStartTime = parseOptionalTime(request.customStartTime(), "customStartTime");
        LocalTime customEndTime = parseOptionalTime(request.customEndTime(), "customEndTime");

        if (!available) {
            if (customStartTime != null || customEndTime != null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unavailable day overrides cannot include custom hours"
                );
            }

            return new ScheduleExceptionValues(false, null, null);
        }

        if (customStartTime == null || customEndTime == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Available date overrides require both customStartTime and customEndTime"
            );
        }

        if (!customStartTime.isBefore(customEndTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "customStartTime must be before customEndTime");
        }

        return new ScheduleExceptionValues(true, customStartTime, customEndTime);
    }

    private Staff getStaff(Long staffId) {
        return staffRepository.findById(staffId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff member not found"));
    }

    private StaffScheduleException getScheduleException(Long staffId, Long exceptionId) {
        return staffScheduleExceptionRepository.findByIdAndStaffId(exceptionId, staffId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule exception not found"));
    }

    private LocalDate parseRequiredDate(String value) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date is required");
        }

        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date is invalid");
        }
    }

    private LocalTime parseOptionalTime(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is invalid");
        }
    }

    private StaffScheduleExceptionResponse toResponse(StaffScheduleException scheduleException) {
        return new StaffScheduleExceptionResponse(
            scheduleException.getId(),
            scheduleException.getStaff().getId(),
            scheduleException.getDate().toString(),
            scheduleException.isAvailable(),
            scheduleException.getCustomStartTime() != null ? scheduleException.getCustomStartTime().toString() : null,
            scheduleException.getCustomEndTime() != null ? scheduleException.getCustomEndTime().toString() : null
        );
    }

    private record ScheduleExceptionValues(
        boolean available,
        LocalTime customStartTime,
        LocalTime customEndTime
    ) {
    }
}
