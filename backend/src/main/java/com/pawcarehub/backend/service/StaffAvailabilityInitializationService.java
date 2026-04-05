package com.pawcarehub.backend.service;

import com.pawcarehub.backend.entity.Staff;
import com.pawcarehub.backend.entity.StaffAvailability;
import com.pawcarehub.backend.repository.StaffAvailabilityRepository;
import com.pawcarehub.backend.repository.StaffRepository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class StaffAvailabilityInitializationService {

    private final StaffRepository staffRepository;
    private final StaffAvailabilityRepository staffAvailabilityRepository;

    public StaffAvailabilityInitializationService(
        StaffRepository staffRepository,
        StaffAvailabilityRepository staffAvailabilityRepository
    ) {
        this.staffRepository = staffRepository;
        this.staffAvailabilityRepository = staffAvailabilityRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeDefaults() {
        for (Staff staff : staffRepository.findAll()) {
            Arrays.stream(DayOfWeek.values()).forEach(dayOfWeek -> createDefaultSlotIfMissing(staff, dayOfWeek));
        }
    }

    private void createDefaultSlotIfMissing(Staff staff, DayOfWeek dayOfWeek) {
        if (staffAvailabilityRepository.existsByStaffIdAndDayOfWeekAndStartTimeAndEndTime(
            staff.getId(),
            dayOfWeek,
            LocalTime.of(8, 0),
            LocalTime.of(17, 0)
        )) {
            return;
        }

        staffAvailabilityRepository.save(new StaffAvailability(
            staff,
            dayOfWeek,
            LocalTime.of(8, 0),
            LocalTime.of(17, 0),
            true
        ));
    }
}
