package com.pawcarehub.backend.repository;

import com.pawcarehub.backend.entity.ClinicService;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicServiceRepository extends JpaRepository<ClinicService, Long> {

    boolean existsByName(String name);

    List<ClinicService> findAllByOrderByCategoryAscNameAsc();
}
