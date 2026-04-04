package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.auth.AuthenticatedUser;
import com.pawcarehub.backend.dto.auth.AuthResponse;
import com.pawcarehub.backend.dto.auth.LoginRequest;
import com.pawcarehub.backend.dto.auth.RegisterRequest;
import com.pawcarehub.backend.dto.auth.UpdateUserProfileRequest;
import com.pawcarehub.backend.dto.auth.UserProfileResponse;
import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserRepository userRepository;
    private final PetInitializationService petInitializationService;
    private final BookingInitializationService bookingInitializationService;

    public AuthService(
        UserRepository userRepository,
        PetInitializationService petInitializationService,
        BookingInitializationService bookingInitializationService
    ) {
        this.userRepository = userRepository;
        this.petInitializationService = petInitializationService;
        this.bookingInitializationService = bookingInitializationService;
    }

    public AuthResponse register(RegisterRequest request) {
        String name = normalizeRequiredField(request.name(), "name");
        String email = normalizeEmail(request.email());
        String password = normalizeRequiredField(request.password(), "password");

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already exists");
        }

        User savedUser = userRepository.save(new User(name, email, passwordEncoder.encode(password)));
        petInitializationService.createDefaultPetsForUser(savedUser);
        bookingInitializationService.createDefaultBookingsForUser(savedUser);
        return new AuthResponse("Registration successful", savedUser.getEmail(), savedUser.getName());
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        String password = normalizeRequiredField(request.password(), "password");

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordMatches(user, password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return new AuthResponse("Login successful", user.getEmail(), user.getName());
    }

    public AuthenticatedUser getAuthenticatedUser(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = getAuthenticatedUserEntity(normalizedEmail);

        return new AuthenticatedUser(user.getName(), user.getEmail());
    }

    public UserProfileResponse getProfile(String userEmailHeader) {
        User user = getAuthenticatedUserEntity(userEmailHeader);
        return toUserProfileResponse(user);
    }

    public UserProfileResponse updateProfile(String userEmailHeader, UpdateUserProfileRequest request) {
        User user = getAuthenticatedUserEntity(userEmailHeader);

        user.setPhone(normalizeOptionalField(request.phone()));
        user.setAddress(normalizeOptionalField(request.address()));
        user.setPreferredContactMethod(normalizeOptionalField(request.preferredContactMethod()));
        user.setEmailReminders(Boolean.TRUE.equals(request.emailReminders()));
        user.setTextReminders(Boolean.TRUE.equals(request.textReminders()));

        User savedUser = userRepository.save(user);
        return toUserProfileResponse(savedUser);
    }

    public User getAuthenticatedUserEntity(String email) {
        String normalizedEmail = normalizeEmail(email);
        return userRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User session is not recognized"));
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

    private String normalizeOptionalField(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private UserProfileResponse toUserProfileResponse(User user) {
        return new UserProfileResponse(
            user.getName(),
            user.getEmail(),
            user.getPhone(),
            user.getAddress(),
            user.getPreferredContactMethod(),
            user.isEmailRemindersEnabled(),
            user.isTextRemindersEnabled()
        );
    }

    private boolean passwordMatches(User user, String rawPassword) {
        String storedPassword = user.getPassword();

        if (isBcryptHash(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }

        if (!storedPassword.equals(rawPassword)) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
        return true;
    }

    private boolean isBcryptHash(String password) {
        return password != null
            && (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }
}
