package com.pawcarehub.backend.repository;

import com.pawcarehub.backend.entity.ClinicService;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicServiceRepository extends JpaRepository<ClinicService, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<ClinicService> findByActiveTrueOrderByCategoryAscNameAsc();

    List<ClinicService> findAllByOrderByCategoryAscNameAsc();

    Optional<ClinicService> findByIdAndActiveTrue(Long id);

    long countByActiveTrue();
}
