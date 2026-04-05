package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.auth.AuthenticatedUser;
import com.pawcarehub.backend.dto.auth.AuthResponse;
import com.pawcarehub.backend.dto.auth.ChangePasswordRequest;
import com.pawcarehub.backend.dto.auth.LoginRequest;
import com.pawcarehub.backend.dto.auth.RegisterRequest;
import com.pawcarehub.backend.dto.auth.UpdateUserProfileRequest;
import com.pawcarehub.backend.dto.auth.UserProfileResponse;
import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.security.JwtService;
import com.pawcarehub.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final String LEGACY_ADMIN_EMAIL_PATTERN = "(?i).*(admin|staff|clinic|team|manager).*";
    private final UserRepository userRepository;
    private final PetInitializationService petInitializationService;
    private final BookingInitializationService bookingInitializationService;
    private final JwtService jwtService;

    public AuthService(
        UserRepository userRepository,
        PetInitializationService petInitializationService,
        BookingInitializationService bookingInitializationService,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.petInitializationService = petInitializationService;
        this.bookingInitializationService = bookingInitializationService;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String name = normalizeRequiredField(request.name(), "name");
        String email = normalizeEmail(request.email());
        String password = normalizeRequiredField(request.password(), "password");

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already exists");
        }

        User savedUser = new User(name, email, passwordEncoder.encode(password));
        savedUser.setActive(true);
        savedUser.setRole(UserRoles.USER);
        savedUser = userRepository.save(savedUser);
        petInitializationService.createDefaultPetsForUser(savedUser);
        bookingInitializationService.createDefaultBookingsForUser(savedUser);
        return new AuthResponse(
            "Registration successful",
            savedUser.getEmail(),
            savedUser.getName(),
            savedUser.getRole(),
            jwtService.generateToken(savedUser)
        );
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        String password = normalizeRequiredField(request.password(), "password");

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This account has been deactivated");
        }

        if (!passwordMatches(user, password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        user = ensureRole(user);
        return new AuthResponse(
            "Login successful",
            user.getEmail(),
            user.getName(),
            user.getRole(),
            jwtService.generateToken(user)
        );
    }

    public AuthenticatedUser getAuthenticatedUser(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = getAuthenticatedUserEntity(normalizedEmail);

        return new AuthenticatedUser(user.getName(), user.getEmail(), user.getRole());
    }

    public AuthenticatedUser getAuthenticatedUser() {
        User user = getAuthenticatedUserEntity();
        return new AuthenticatedUser(user.getName(), user.getEmail(), user.getRole());
    }

    public UserProfileResponse getProfile() {
        User user = getAuthenticatedUserEntity();
        return toUserProfileResponse(user);
    }

    public UserProfileResponse updateProfile(UpdateUserProfileRequest request) {
        User user = getAuthenticatedUserEntity();

        user.setPhone(normalizeOptionalField(request.phone()));
        user.setAddress(normalizeOptionalField(request.address()));
        user.setPreferredContactMethod(normalizeOptionalField(request.preferredContactMethod()));
        user.setEmailReminders(Boolean.TRUE.equals(request.emailReminders()));
        user.setTextReminders(Boolean.TRUE.equals(request.textReminders()));

        User savedUser = userRepository.save(user);
        return toUserProfileResponse(savedUser);
    }

    public void changePassword(ChangePasswordRequest request) {
        User user = getAuthenticatedUserEntity();
        String currentPassword = normalizeRequiredField(request.currentPassword(), "currentPassword");
        String newPassword = normalizeRequiredField(request.newPassword(), "newPassword");

        if (!passwordMatches(user, currentPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        if (currentPassword.equals(newPassword)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "New password must be different from the current password"
            );
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void deactivateCurrentUser() {
        User user = getAuthenticatedUserEntity();
        user.setActive(false);
        userRepository.save(user);
    }

    public User getAuthenticatedUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User session is not recognized");
        }

        return getAuthenticatedUserEntity(authentication.getName());
    }

    public User getAuthenticatedUserEntity(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User session is not recognized"));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User session is not recognized");
        }

        return ensureRole(user);
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
            user.isActive(),
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

    private User ensureRole(User user) {
        String normalizedRole = UserRoles.normalize(user.getRole());
        if (user.getRole() == null || !normalizedRole.equals(user.getRole())) {
            user.setRole(resolveInitialRole(user.getEmail(), normalizedRole));
            return userRepository.save(user);
        }

        return user;
    }

    private String resolveInitialRole(String email, String currentRole) {
        if (currentRole != null && !UserRoles.USER.equals(currentRole)) {
            return currentRole;
        }

        if (email != null && email.matches(LEGACY_ADMIN_EMAIL_PATTERN)) {
            return UserRoles.ADMIN;
        }

        return UserRoles.USER;
    }
}
