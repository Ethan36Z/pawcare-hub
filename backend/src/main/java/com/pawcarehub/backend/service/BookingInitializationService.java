package com.pawcarehub.backend.service;

import com.pawcarehub.backend.entity.Booking;
import com.pawcarehub.backend.entity.ClinicService;
import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.repository.BookingRepository;
import com.pawcarehub.backend.repository.ClinicServiceRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BookingInitializationService {

    private final BookingRepository bookingRepository;
    private final ClinicServiceRepository clinicServiceRepository;

    public BookingInitializationService(
        BookingRepository bookingRepository,
        ClinicServiceRepository clinicServiceRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.clinicServiceRepository = clinicServiceRepository;
    }

    public void createDefaultBookingsForUser(User user) {
        String ownerName = user.getName().trim();
        String ownerFirstName = ownerName.split(" ")[0];
        ClinicService wellnessService = clinicServiceRepository.findAllByOrderByCategoryAscNameAsc().stream()
            .filter(service -> "Annual wellness exam".equalsIgnoreCase(service.getName()))
            .findFirst()
            .orElse(null);
        ClinicService followUpService = clinicServiceRepository.findAllByOrderByCategoryAscNameAsc().stream()
            .filter(service -> "Vaccination follow-up".equalsIgnoreCase(service.getName()))
            .findFirst()
            .orElse(null);

        List<Booking> defaultBookings = List.of(
            new Booking(
                ownerFirstName + "'s Buddy",
                "Annual wellness exam",
                "April 18, 2026",
                "10:30 AM",
                "Confirmed",
                "PawCare Hub Clinic",
                "Dr. Rivera",
                wellnessService,
                user
            ),
            new Booking(
                "Luna",
                "Vaccination follow-up",
                "April 24, 2026",
                "2:15 PM",
                "Upcoming",
                "PawCare Hub Clinic",
                "Nurse Patel",
                followUpService,
                user
            )
        );

        bookingRepository.saveAll(defaultBookings);
    }
}
