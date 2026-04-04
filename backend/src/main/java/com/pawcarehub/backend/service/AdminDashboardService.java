package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.admin.AdminDashboardStatsResponse;
import com.pawcarehub.backend.repository.BookingRepository;
import com.pawcarehub.backend.repository.ClinicServiceRepository;
import com.pawcarehub.backend.repository.PetRepository;
import com.pawcarehub.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final BookingRepository bookingRepository;
    private final ClinicServiceRepository clinicServiceRepository;

    public AdminDashboardService(
        UserRepository userRepository,
        PetRepository petRepository,
        BookingRepository bookingRepository,
        ClinicServiceRepository clinicServiceRepository
    ) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.bookingRepository = bookingRepository;
        this.clinicServiceRepository = clinicServiceRepository;
    }

    public AdminDashboardStatsResponse getStats() {
        return new AdminDashboardStatsResponse(
            userRepository.count(),
            petRepository.count(),
            bookingRepository.count(),
            clinicServiceRepository.count(),
            bookingRepository.countByStatusIgnoreCase("Confirmed"),
            bookingRepository.countByStatusIgnoreCase("Cancelled")
        );
    }
}
