package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.auth.AuthenticatedUser;
import com.pawcarehub.backend.dto.booking.BookingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    private final AuthService authService;

    public BookingService(AuthService authService) {
        this.authService = authService;
    }

    public List<BookingResponse> getCurrentUserBookings(String userEmailHeader) {
        AuthenticatedUser user = authService.getAuthenticatedUser(userEmailHeader);
        String ownerName = user.name().trim();
        String ownerSlug = user.email().split("@")[0].toLowerCase();
        String ownerFirstName = ownerName.split(" ")[0];
        int seed = ownerSlug.chars().sum();

        List<String> petNames = List.of(
            ownerFirstName + "'s Companion",
            buildSecondaryPetName(ownerSlug),
            "Maple"
        );
        List<String> services = List.of(
            "Annual wellness exam",
            "Vaccination follow-up",
            "Dental evaluation"
        );
        List<String> dates = List.of("April 18, 2026", "April 24, 2026", "March 12, 2026");
        List<String> times = List.of("10:30 AM", "2:15 PM", "9:00 AM");
        List<String> statuses = List.of("Confirmed", "Upcoming", "Completed");
        List<String> staffRotation = List.of("Dr. Rivera", "Nurse Patel", "Dr. Chen");

        return services.stream()
            .map(service -> createBooking(
                service,
                services.indexOf(service),
                petNames,
                dates,
                times,
                statuses,
                staffRotation,
                seed,
                user.email()
            ))
            .toList();
    }

    private BookingResponse createBooking(
        String service,
        int index,
        List<String> petNames,
        List<String> dates,
        List<String> times,
        List<String> statuses,
        List<String> staffRotation,
        int seed,
        String ownerEmail
    ) {
        return new BookingResponse(
            petNames.get(index),
            service,
            dates.get(index),
            times.get(index),
            statuses.get(index),
            "PawCare Hub Clinic",
            staffRotation.get((seed + index) % staffRotation.size()),
            ownerEmail
        );
    }

    private String buildSecondaryPetName(String ownerSlug) {
        String trimmedSlug = ownerSlug.length() > 1 ? ownerSlug.substring(1, Math.min(ownerSlug.length(), 6)) : "Buddy";
        return Character.toUpperCase(ownerSlug.charAt(0)) + trimmedSlug;
    }
}
