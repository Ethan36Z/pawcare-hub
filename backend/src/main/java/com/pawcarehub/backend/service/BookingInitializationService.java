package com.pawcarehub.backend.service;

import com.pawcarehub.backend.entity.Booking;
import com.pawcarehub.backend.entity.ClinicService;
import com.pawcarehub.backend.entity.Staff;
import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.repository.BookingRepository;
import com.pawcarehub.backend.repository.ClinicServiceRepository;
import com.pawcarehub.backend.repository.StaffRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class BookingInitializationService {
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.US);


    private final BookingRepository bookingRepository;
    private final ClinicServiceRepository clinicServiceRepository;
    private final StaffRepository staffRepository;

    public BookingInitializationService(
        BookingRepository bookingRepository,
        ClinicServiceRepository clinicServiceRepository,
        StaffRepository staffRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.clinicServiceRepository = clinicServiceRepository;
        this.staffRepository = staffRepository;
    }

    public void createDefaultBookingsForUser(User user) {
        String ownerName = user.getName().trim();
        String ownerFirstName = ownerName.split(" ")[0];
        ClinicService wellnessService = clinicServiceRepository.findAllByOrderByCategoryAscNameAsc().stream()
            .filter(service -> "Annual wellness exam".equalsIgnoreCase(service.getName()))
            .findFirst()
            .orElse(null);
        ClinicService followUpService = clinicServiceRepository.findAllByOrderByCategoryAscNameAsc().stream()
            .filter(service -> "Core vaccine appointment".equalsIgnoreCase(service.getName()))
            .findFirst()
            .orElse(null);
        Staff primaryDoctor = staffRepository.findFirstByNameIgnoreCaseAndActiveTrue("Dr. Maya Hart")
            .or(() -> staffRepository.findFirstByNameIgnoreCaseAndActiveTrue("Dr. Rivera"))
            .orElse(null);
        Staff supportStaff = staffRepository.findFirstByNameIgnoreCaseAndActiveTrue("Jamie Brooks")
            .or(() -> staffRepository.findFirstByNameIgnoreCaseAndActiveTrue("Nurse Patel"))
            .orElse(primaryDoctor);

        List<Booking> defaultBookings = List.of(
            new Booking(
                ownerFirstName + "'s Buddy",
                "Annual wellness exam",
                nextDate(DayOfWeek.MONDAY, 3),
                "10:30 AM",
                "Confirmed",
                "PawCare Hub Clinic",
                staffName(primaryDoctor, "Care Team"),
                wellnessService,
                primaryDoctor,
                user
            ),
            new Booking(
                "Luna",
                "Core vaccine appointment",
                nextDate(DayOfWeek.THURSDAY, 5),
                "2:15 PM",
                "Upcoming",
                "PawCare Hub Clinic",
                staffName(supportStaff, "Care Team"),
                followUpService,
                supportStaff,
                user
            )
        );

        bookingRepository.saveAll(defaultBookings);
    }

    private String nextDate(DayOfWeek dayOfWeek, int minimumDaysFromToday) {
        LocalDate date = LocalDate.now().plusDays(minimumDaysFromToday);
        while (date.getDayOfWeek() != dayOfWeek) {
            date = date.plusDays(1);
        }
        return date.format(DISPLAY_DATE_FORMATTER);
    }

    private String staffName(Staff staff, String fallback) {
        return staff != null ? staff.getName() : fallback;
    }
}
