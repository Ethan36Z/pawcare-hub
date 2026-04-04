package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.service.ClinicServiceResponse;
import com.pawcarehub.backend.repository.ClinicServiceRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ClinicServiceService {

    private final ClinicServiceRepository clinicServiceRepository;

    public ClinicServiceService(ClinicServiceRepository clinicServiceRepository) {
        this.clinicServiceRepository = clinicServiceRepository;
    }

    public List<ClinicServiceResponse> getActiveServices() {
        return clinicServiceRepository.findByActiveTrueOrderByCategoryAscNameAsc().stream()
            .map(service -> new ClinicServiceResponse(
                service.getId(),
                service.getName(),
                service.getCategory(),
                service.getDescription(),
                service.getDuration(),
                service.getPrice()
            ))
            .toList();
    }
}
