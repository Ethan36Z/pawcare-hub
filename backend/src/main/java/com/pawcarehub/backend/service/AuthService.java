package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.auth.AuthenticatedUser;
import com.pawcarehub.backend.dto.auth.AuthResponse;
import com.pawcarehub.backend.dto.auth.LoginRequest;
import com.pawcarehub.backend.dto.auth.RegisterRequest;
import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PetInitializationService petInitializationService;

    public AuthService(
        UserRepository userRepository,
        PetInitializationService petInitializationService
    ) {
        this.userRepository = userRepository;
        this.petInitializationService = petInitializationService;
    }

    public AuthResponse register(RegisterRequest request) {
        String name = normalizeRequiredField(request.name(), "name");
        String email = normalizeEmail(request.email());
        String password = normalizeRequiredField(request.password(), "password");

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already exists");
        }

        User savedUser = userRepository.save(new User(name, email, password));
        petInitializationService.createDefaultPetsForUser(savedUser);
        return new AuthResponse("Registration successful", savedUser.getEmail(), savedUser.getName());
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        String password = normalizeRequiredField(request.password(), "password");

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!user.getPassword().equals(password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return new AuthResponse("Login successful", user.getEmail(), user.getName());
    }

    public AuthenticatedUser getAuthenticatedUser(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User session is not recognized"));

        return new AuthenticatedUser(user.getName(), user.getEmail());
    }

    private String normalizeEmail(String email) {
        String normalizedEmail = normalizeRequiredField(email, "email").toLowerCase();
        if (!normalizedEmail.contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid email is required");
        }
        return normalizedEmail;
    }

    private String normalizeRequiredField(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                fieldName + " is required"
            );
        }
        return value.trim();
    }
}
