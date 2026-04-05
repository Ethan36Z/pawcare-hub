package com.pawcarehub.backend.repository;

import com.pawcarehub.backend.entity.StaffAvailability;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffAvailabilityRepository extends JpaRepository<StaffAvailability, Long> {

    List<StaffAvailability> findByStaffIdOrderByDayOfWeekAscStartTimeAsc(Long staffId);

    List<StaffAvailability> findByStaffIdAndDayOfWeekOrderByStartTimeAsc(Long staffId, DayOfWeek dayOfWeek);

    List<StaffAvailability> findByStaffIdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(Long staffId, DayOfWeek dayOfWeek);

    Optional<StaffAvailability> findByIdAndStaffId(Long id, Long staffId);

    boolean existsByStaffIdAndDayOfWeekAndStartTimeAndEndTime(
        Long staffId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
    );
}
