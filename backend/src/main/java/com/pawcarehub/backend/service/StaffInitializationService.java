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
            createDefaultStaff(
                "Dr. Rivera",
                "Veterinarian",
                "Dr. Elena Rivera",
                "Small Animal Veterinarian",
                "Focused on preventive care, wellness visits, and clear follow-up guidance for families."
            ),
            createDefaultStaff(
                "Nurse Patel",
                "Veterinary Nurse",
                "Asha Patel",
                "Veterinary Nurse",
                "Supports calm check-ins, patient comfort, and practical day-of-visit care coordination."
            ),
            createDefaultStaff(
                "Dr. Chen",
                "Veterinarian",
                "Dr. Marcus Chen",
                "Companion Animal Veterinarian",
                "Enjoys helping pet owners understand treatment plans and next steps with confidence."
            )
        );

        List<Staff> staffToCreate = defaultStaff.stream()
            .filter(staff -> !staffRepository.existsByNameIgnoreCaseAndRoleIgnoreCase(staff.getName(), staff.getRole()))
            .toList();

        if (!staffToCreate.isEmpty()) {
            staffRepository.saveAll(staffToCreate);
        }
    }

    private Staff createDefaultStaff(
        String name,
        String role,
        String displayName,
        String specialty,
        String bio
    ) {
        Staff staff = new Staff(name, role, true);
        staff.setDisplayName(displayName);
        staff.setSpecialty(specialty);
        staff.setBio(bio);
        staff.setShowOnHomepage(true);
        return staff;
    }
}
