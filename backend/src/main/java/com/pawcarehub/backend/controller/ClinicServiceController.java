package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.service.ClinicServiceResponse;
import com.pawcarehub.backend.service.ClinicServiceService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/services")
public class ClinicServiceController {

    private final ClinicServiceService clinicServiceService;

    public ClinicServiceController(ClinicServiceService clinicServiceService) {
        this.clinicServiceService = clinicServiceService;
    }

    @GetMapping
    public List<ClinicServiceResponse> getServices() {
        return clinicServiceService.getActiveServices();
    }
}
