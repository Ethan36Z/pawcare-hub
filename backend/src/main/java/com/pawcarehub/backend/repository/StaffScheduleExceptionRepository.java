package com.pawcarehub.backend.repository;

import com.pawcarehub.backend.entity.StaffScheduleException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffScheduleExceptionRepository extends JpaRepository<StaffScheduleException, Long> {

    List<StaffScheduleException> findByStaffIdOrderByDateAsc(Long staffId);

    Optional<StaffScheduleException> findByStaffIdAndDate(Long staffId, LocalDate date);

    Optional<StaffScheduleException> findByIdAndStaffId(Long id, Long staffId);

    boolean existsByStaffIdAndDate(Long staffId, LocalDate date);

    boolean existsByStaffIdAndDateAndIdNot(Long staffId, LocalDate date, Long id);
}
