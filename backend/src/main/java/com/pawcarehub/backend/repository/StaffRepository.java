package com.pawcarehub.backend.repository;

import com.pawcarehub.backend.entity.Staff;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    List<Staff> findAllByOrderByActiveDescNameAsc();

    List<Staff> findByActiveTrueOrderByNameAsc();

    List<Staff> findByActiveTrueAndShowOnHomepageTrueOrderByNameAsc();

    Optional<Staff> findByIdAndActiveTrue(Long id);

    Optional<Staff> findFirstByNameIgnoreCaseAndActiveTrue(String name);

    boolean existsByNameIgnoreCaseAndRoleIgnoreCase(String name, String role);

    boolean existsByNameIgnoreCaseAndRoleIgnoreCaseAndIdNot(String name, String role, Long id);

    long countByActiveTrue();
}
