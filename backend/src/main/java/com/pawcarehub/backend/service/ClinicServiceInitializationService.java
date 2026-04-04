package com.pawcarehub.backend.service;

import com.pawcarehub.backend.entity.ClinicService;
import com.pawcarehub.backend.repository.ClinicServiceRepository;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ClinicServiceInitializationService {

    private final ClinicServiceRepository clinicServiceRepository;

    public ClinicServiceInitializationService(ClinicServiceRepository clinicServiceRepository) {
        this.clinicServiceRepository = clinicServiceRepository;
    }

    @PostConstruct
    public void initializeDefaults() {
        List<ClinicService> defaultServices = List.of(
            new ClinicService(
                "Annual wellness exam",
                "Wellness",
                "A routine checkup with preventive guidance and a general health review.",
                "30 min",
                "$85"
            ),
            new ClinicService(
                "Puppy and kitten wellness visit",
                "Wellness",
                "Early-life exam focused on growth, nutrition, and preventive care planning.",
                "30 min",
                "$78"
            ),
            new ClinicService(
                "Core vaccine appointment",
                "Vaccines",
                "A visit for recommended core vaccines with timing based on your pet's age.",
                "20 min",
                "From $35"
            ),
            new ClinicService(
                "Dental evaluation",
                "Dental",
                "An oral health assessment to review tartar, gum condition, and next steps.",
                "25 min",
                "$65"
            ),
            new ClinicService(
                "Sick visit consultation",
                "Sick Visits",
                "A prompt appointment for concerns like itching, vomiting, coughing, or low energy.",
                "30 min",
                "From $95"
            )
        );

        List<ClinicService> servicesToCreate = defaultServices.stream()
            .filter(service -> !clinicServiceRepository.existsByName(service.getName()))
            .toList();

        if (!servicesToCreate.isEmpty()) {
            clinicServiceRepository.saveAll(servicesToCreate);
        }
    }
}
