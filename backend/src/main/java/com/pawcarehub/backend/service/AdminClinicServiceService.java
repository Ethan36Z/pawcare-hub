package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.admin.CreateClinicServiceRequest;
import com.pawcarehub.backend.dto.admin.AdminClinicServiceResponse;
import com.pawcarehub.backend.entity.ClinicService;
import com.pawcarehub.backend.repository.ClinicServiceRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminClinicServiceService {

    private final ClinicServiceRepository clinicServiceRepository;

    public AdminClinicServiceService(ClinicServiceRepository clinicServiceRepository) {
        this.clinicServiceRepository = clinicServiceRepository;
    }

    public List<AdminClinicServiceResponse> getAllServices() {
        return clinicServiceRepository.findAllByOrderByCategoryAscNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public AdminClinicServiceResponse createService(CreateClinicServiceRequest request) {
        String name = normalizeRequiredField(request.name(), "name");

        if (clinicServiceRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A service with this name already exists");
        }

        ClinicService savedService = clinicServiceRepository.save(new ClinicService(
            name,
            normalizeRequiredField(request.category(), "category"),
            normalizeRequiredField(request.description(), "description"),
            normalizeRequiredField(request.duration(), "duration"),
            normalizeRequiredField(request.price(), "price"),
            request.active() == null || request.active()
        ));

        return toResponse(savedService);
    }

    public AdminClinicServiceResponse updateService(Long serviceId, CreateClinicServiceRequest request) {
        ClinicService service = clinicServiceRepository.findById(serviceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));

        String name = normalizeRequiredField(request.name(), "name");
        if (clinicServiceRepository.existsByNameAndIdNot(name, serviceId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A service with this name already exists");
        }

        service.setName(name);
        service.setCategory(normalizeRequiredField(request.category(), "category"));
        service.setDescription(normalizeRequiredField(request.description(), "description"));
        service.setDuration(normalizeRequiredField(request.duration(), "duration"));
        service.setPrice(normalizeRequiredField(request.price(), "price"));
        service.setActive(request.active() == null || request.active());

        ClinicService savedService = clinicServiceRepository.save(service);
        return toResponse(savedService);
    }

    public AdminClinicServiceResponse toggleServiceAvailability(Long serviceId) {
        ClinicService service = clinicServiceRepository.findById(serviceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));

        service.setActive(!service.isActive());
        ClinicService savedService = clinicServiceRepository.save(service);

        return toResponse(savedService);
    }

    private AdminClinicServiceResponse toResponse(ClinicService service) {
        return new AdminClinicServiceResponse(
            service.getId(),
            service.getName(),
            service.getCategory(),
            service.getDescription(),
            service.getDuration(),
            service.getPrice(),
            service.isActive()
        );
    }

    private String normalizeRequiredField(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }

        return value.trim();
    }
}
