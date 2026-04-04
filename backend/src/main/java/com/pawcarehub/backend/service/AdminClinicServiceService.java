package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.admin.AdminClinicServiceResponse;
import com.pawcarehub.backend.entity.ClinicService;
import com.pawcarehub.backend.repository.ClinicServiceRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
                service.getPrice(),
                service.isActive()
            ))
            .toList();
    }

    public AdminClinicServiceResponse toggleServiceAvailability(Long serviceId) {
        ClinicService service = clinicServiceRepository.findById(serviceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));

        service.setActive(!service.isActive());
        ClinicService savedService = clinicServiceRepository.save(service);

        return new AdminClinicServiceResponse(
            savedService.getId(),
            savedService.getName(),
            savedService.getCategory(),
            savedService.getDescription(),
            savedService.getDuration(),
            savedService.getPrice(),
            savedService.isActive()
        );
    }
}
