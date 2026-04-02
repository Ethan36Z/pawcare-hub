package com.pawcarehub.backend.service;

import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public Map<String, String> getHealthStatus() {
        return Map.of(
            "status", "UP",
            "service", "pawcarehub-backend"
        );
    }
}
