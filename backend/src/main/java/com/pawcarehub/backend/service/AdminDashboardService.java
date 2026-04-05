package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.admin.AdminDashboardStatsResponse;
import com.pawcarehub.backend.entity.Booking;
import com.pawcarehub.backend.repository.BookingRepository;
import com.pawcarehub.backend.repository.ClinicServiceRepository;
import com.pawcarehub.backend.repository.PetRepository;
import com.pawcarehub.backend.repository.StaffRepository;
import com.pawcarehub.backend.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final BookingRepository bookingRepository;
    private final ClinicServiceRepository clinicServiceRepository;
    private final StaffRepository staffRepository;

    public AdminDashboardService(
        UserRepository userRepository,
        PetRepository petRepository,
        BookingRepository bookingRepository,
        ClinicServiceRepository clinicServiceRepository,
        StaffRepository staffRepository
    ) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.bookingRepository = bookingRepository;
        this.clinicServiceRepository = clinicServiceRepository;
        this.staffRepository = staffRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getStats() {
        List<Booking> latestBookings = bookingRepository.findAllByOrderByIdDesc();

        return new AdminDashboardStatsResponse(
            userRepository.count(),
            petRepository.count(),
            staffRepository.countByActiveTrue(),
            clinicServiceRepository.countByActiveTrue(),
            bookingRepository.count(),
            buildBookingStatusStats(),
            latestBookings.stream()
                .filter(booking -> isUpcomingStatus(booking.getStatus()))
                .limit(5)
                .map(this::toSnapshotItem)
                .toList(),
            latestBookings.stream()
                .filter(booking -> "Completed".equalsIgnoreCase(booking.getStatus()))
                .limit(5)
                .map(this::toSnapshotItem)
                .toList()
        );
    }

    private List<AdminDashboardStatsResponse.BookingStatusStat> buildBookingStatusStats() {
        return List.of(
            new AdminDashboardStatsResponse.BookingStatusStat("Upcoming", bookingRepository.countByStatusIgnoreCase("Upcoming")),
            new AdminDashboardStatsResponse.BookingStatusStat("Confirmed", bookingRepository.countByStatusIgnoreCase("Confirmed")),
            new AdminDashboardStatsResponse.BookingStatusStat("Completed", bookingRepository.countByStatusIgnoreCase("Completed")),
            new AdminDashboardStatsResponse.BookingStatusStat("Cancelled", bookingRepository.countByStatusIgnoreCase("Cancelled"))
        );
    }

    private AdminDashboardStatsResponse.DashboardBookingSnapshotItem toSnapshotItem(Booking booking) {
        return new AdminDashboardStatsResponse.DashboardBookingSnapshotItem(
            booking.getId(),
            booking.getPetName(),
            booking.getResolvedServiceName(),
            booking.getDate(),
            booking.getTime(),
            booking.getResolvedStaffName(),
            booking.getOwner().getName(),
            booking.getStatus()
        );
    }

    private boolean isUpcomingStatus(String status) {
        return "Upcoming".equalsIgnoreCase(status) || "Confirmed".equalsIgnoreCase(status);
    }
}
