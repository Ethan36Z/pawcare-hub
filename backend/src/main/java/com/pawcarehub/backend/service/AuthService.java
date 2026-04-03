package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.auth.AuthenticatedUser;
import com.pawcarehub.backend.dto.auth.AuthResponse;
import com.pawcarehub.backend.dto.auth.LoginRequest;
import com.pawcarehub.backend.dto.auth.RegisterRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final Map<String, InMemoryAuthUser> usersByEmail = new ConcurrentHashMap<>();

    public AuthResponse register(RegisterRequest request) {
        String name = normalizeRequiredField(request.name(), "name");
        String email = normalizeEmail(request.email());
        String password = normalizeRequiredField(request.password(), "password");

        InMemoryAuthUser newUser = new InMemoryAuthUser(name, email, password);
        InMemoryAuthUser existingUser = usersByEmail.putIfAbsent(email, newUser);
        if (existingUser != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already exists");
        }

        return new AuthResponse("Registration successful", newUser.email(), newUser.name());
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        String password = normalizeRequiredField(request.password(), "password");

        InMemoryAuthUser user = usersByEmail.get(email);
        if (user == null || !user.password().equals(password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return new AuthResponse("Login successful", user.email(), user.name());
    }

    public AuthenticatedUser getAuthenticatedUser(String email) {
        String normalizedEmail = normalizeEmail(email);
        InMemoryAuthUser user = usersByEmail.get(normalizedEmail);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User session is not recognized");
        }

        return new AuthenticatedUser(user.name(), user.email());
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

    private record InMemoryAuthUser(String name, String email, String password) {
    }
}
