package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.admin.AdminUserResponse;
import com.pawcarehub.backend.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminUserService {

    private static final String ADMIN_EMAIL_PATTERN = "(?i).*(admin|staff|clinic|team|manager).*";

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .map(user -> new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                resolveRole(user.getEmail())
            ))
            .toList();
    }

    private String resolveRole(String email) {
        return email != null && email.matches(ADMIN_EMAIL_PATTERN) ? "admin" : "user";
    }
}
