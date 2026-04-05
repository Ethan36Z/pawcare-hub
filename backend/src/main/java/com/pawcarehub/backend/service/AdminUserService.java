package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.admin.AdminUserBookingSummaryResponse;
import com.pawcarehub.backend.dto.admin.AdminUserDetailResponse;
import com.pawcarehub.backend.dto.admin.AdminUserPetSummaryResponse;
import com.pawcarehub.backend.dto.admin.AdminUserResponse;
import com.pawcarehub.backend.entity.Booking;
import com.pawcarehub.backend.entity.Pet;
import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.repository.BookingRepository;
import com.pawcarehub.backend.repository.PetRepository;
import com.pawcarehub.backend.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUserService {
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final BookingRepository bookingRepository;

    public AdminUserService(
        UserRepository userRepository,
        PetRepository petRepository,
        BookingRepository bookingRepository
    ) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers(String search, String role, Boolean active) {
        return userRepository.findAll().stream()
            .filter(user -> matchesSearch(user, search))
            .filter(user -> matchesRole(user, role))
            .filter(user -> active == null || user.isActive() == active)
            .map(user -> new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                UserRoles.normalize(user.getRole()),
                user.isActive()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<Pet> pets = petRepository.findByOwnerEmailOrderByIdAsc(user.getEmail());
        List<Booking> bookings = bookingRepository.findByOwnerEmailOrderByIdAsc(user.getEmail());

        List<AdminUserPetSummaryResponse> petSummaries = pets.stream()
            .map(pet -> new AdminUserPetSummaryResponse(
                pet.getId(),
                pet.getName(),
                pet.getSpecies(),
                pet.getBreed(),
                pet.getStatus()
            ))
            .toList();

        List<AdminUserBookingSummaryResponse> recentBookings = bookings.stream()
            .sorted((left, right) -> Long.compare(right.getId(), left.getId()))
            .limit(5)
            .map(this::toBookingSummary)
            .toList();

        return new AdminUserDetailResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            UserRoles.normalize(user.getRole()),
            user.isActive(),
            user.getPhone(),
            user.getAddress(),
            user.getPreferredContactMethod(),
            user.isEmailRemindersEnabled(),
            user.isTextRemindersEnabled(),
            petSummaries.size(),
            bookings.size(),
            petSummaries,
            recentBookings
        );
    }

    private AdminUserBookingSummaryResponse toBookingSummary(Booking booking) {
        return new AdminUserBookingSummaryResponse(
            booking.getId(),
            booking.getPetName(),
            booking.getService(),
            booking.getDate(),
            booking.getTime(),
            booking.getStatus(),
            booking.getClinic(),
            booking.getStaff()
        );
    }

    private boolean matchesSearch(User user, String search) {
        if (!StringUtils.hasText(search)) {
            return true;
        }

        String normalizedSearch = search.trim().toLowerCase();
        return containsIgnoreCase(user.getName(), normalizedSearch)
            || containsIgnoreCase(user.getEmail(), normalizedSearch);
    }

    private boolean matchesRole(User user, String role) {
        if (!StringUtils.hasText(role)) {
            return true;
        }

        return UserRoles.normalize(user.getRole()).equalsIgnoreCase(role.trim());
    }

    private boolean containsIgnoreCase(String value, String normalizedSearch) {
        return value != null && value.toLowerCase().contains(normalizedSearch);
    }
}
