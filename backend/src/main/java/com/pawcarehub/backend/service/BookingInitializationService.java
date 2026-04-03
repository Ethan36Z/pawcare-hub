package com.pawcarehub.backend.service;

import com.pawcarehub.backend.entity.Booking;
import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.repository.BookingRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BookingInitializationService {

    private final BookingRepository bookingRepository;

    public BookingInitializationService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public void createDefaultBookingsForUser(User user) {
        String ownerName = user.getName().trim();
        String ownerFirstName = ownerName.split(" ")[0];

        List<Booking> defaultBookings = List.of(
            new Booking(
                ownerFirstName + "'s Buddy",
                "Annual wellness exam",
                "April 18, 2026",
                "10:30 AM",
                "Confirmed",
                "PawCare Hub Clinic",
                "Dr. Rivera",
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
                user
            )
        );

        bookingRepository.saveAll(defaultBookings);
    }
}
