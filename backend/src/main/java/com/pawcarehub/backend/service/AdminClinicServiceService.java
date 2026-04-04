package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.admin.AdminClinicServiceResponse;
import com.pawcarehub.backend.repository.ClinicServiceRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminClinicServiceService {

    private final ClinicServiceRepository clinicServiceRepository;

    public AdminClinicServiceService(ClinicServiceRepository clinicServiceRepository) {
        this.clinicServiceRepository = clinicServiceRepository;
    }

    public List<AdminClinicServiceResponse> getAllServices() {
        return clinicServiceRepository.findAllByOrderByCategoryAscNameAsc().stream()
            .map(service -> new AdminClinicServiceResponse(
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
