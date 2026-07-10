package com.pawcarehub.backend.service;

import com.pawcarehub.backend.entity.Staff;
import com.pawcarehub.backend.entity.StaffAvailability;
import com.pawcarehub.backend.repository.StaffAvailabilityRepository;
import com.pawcarehub.backend.repository.StaffRepository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
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
            if (!staffAvailabilityRepository.findByStaffIdOrderByDayOfWeekAscStartTimeAsc(staff.getId()).isEmpty()) {
                continue;
            }
            defaultSlotsFor(staff).forEach(slot -> createDefaultSlotIfMissing(staff, slot.dayOfWeek(), slot.startTime(), slot.endTime()));
        }
    }

    private List<AvailabilitySlot> defaultSlotsFor(Staff staff) {
        String staffName = staff.getName();
        if ("Dr. Maya Hart".equalsIgnoreCase(staffName) || "Dr. Rivera".equalsIgnoreCase(staffName)) {
            return List.of(
                slot(DayOfWeek.MONDAY, 9, 0, 17, 0),
                slot(DayOfWeek.WEDNESDAY, 9, 0, 17, 0),
                slot(DayOfWeek.FRIDAY, 9, 0, 16, 0)
            );
        }
        if ("Dr. Leo Finch".equalsIgnoreCase(staffName) || "Dr. Chen".equalsIgnoreCase(staffName)) {
            return List.of(
                slot(DayOfWeek.TUESDAY, 9, 30, 17, 30),
                slot(DayOfWeek.THURSDAY, 9, 30, 17, 30)
            );
        }
        if ("Jamie Brooks".equalsIgnoreCase(staffName)) {
            return List.of(
                slot(DayOfWeek.MONDAY, 8, 30, 16, 30),
                slot(DayOfWeek.TUESDAY, 8, 30, 16, 30),
                slot(DayOfWeek.WEDNESDAY, 8, 30, 16, 30),
                slot(DayOfWeek.THURSDAY, 8, 30, 13, 0)
            );
        }
        if ("Anna Reed".equalsIgnoreCase(staffName) || "Nurse Patel".equalsIgnoreCase(staffName)) {
            return List.of(
                slot(DayOfWeek.MONDAY, 8, 0, 16, 0),
                slot(DayOfWeek.TUESDAY, 8, 0, 16, 0),
                slot(DayOfWeek.WEDNESDAY, 8, 0, 16, 0),
                slot(DayOfWeek.THURSDAY, 8, 0, 16, 0),
                slot(DayOfWeek.FRIDAY, 8, 0, 14, 0)
            );
        }
        if ("Noah Kim".equalsIgnoreCase(staffName)) {
            return List.of(
                slot(DayOfWeek.MONDAY, 8, 0, 15, 0),
                slot(DayOfWeek.TUESDAY, 10, 0, 18, 0),
                slot(DayOfWeek.THURSDAY, 10, 0, 18, 0),
                slot(DayOfWeek.FRIDAY, 8, 0, 15, 0)
            );
        }

        return Arrays.stream(DayOfWeek.values())
            .filter(dayOfWeek -> dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY)
            .map(dayOfWeek -> new AvailabilitySlot(dayOfWeek, LocalTime.of(9, 0), LocalTime.of(17, 0)))
            .toList();
    }

    private void createDefaultSlotIfMissing(Staff staff, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        if (staffAvailabilityRepository.existsByStaffIdAndDayOfWeekAndStartTimeAndEndTime(
            staff.getId(),
            dayOfWeek,
            startTime,
            endTime
        )) {
            return;
        }

        staffAvailabilityRepository.save(new StaffAvailability(
            staff,
            dayOfWeek,
            startTime,
            endTime,
            true
        ));
    }

    private AvailabilitySlot slot(DayOfWeek dayOfWeek, int startHour, int startMinute, int endHour, int endMinute) {
        return new AvailabilitySlot(dayOfWeek, LocalTime.of(startHour, startMinute), LocalTime.of(endHour, endMinute));
    }

    private record AvailabilitySlot(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
    }
}
