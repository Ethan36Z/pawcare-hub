package com.pawcarehub.backend.service;

import com.pawcarehub.backend.entity.Staff;
import com.pawcarehub.backend.repository.StaffRepository;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StaffInitializationService {

    private final StaffRepository staffRepository;

    public StaffInitializationService(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    @PostConstruct
    public void initializeDefaults() {
        List<Staff> defaultStaff = List.of(
            new Staff("Dr. Rivera", "Veterinarian", true),
            new Staff("Nurse Patel", "Veterinary Nurse", true),
            new Staff("Dr. Chen", "Veterinarian", true)
        );

        List<Staff> staffToCreate = defaultStaff.stream()
            .filter(staff -> !staffRepository.existsByNameIgnoreCaseAndRoleIgnoreCase(staff.getName(), staff.getRole()))
            .toList();

        if (!staffToCreate.isEmpty()) {
            staffRepository.saveAll(staffToCreate);
        }
    }
}
