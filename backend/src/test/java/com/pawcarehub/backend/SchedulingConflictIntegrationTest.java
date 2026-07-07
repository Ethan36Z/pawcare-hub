package com.pawcarehub.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pawcarehub.backend.dto.admin.UpsertStaffAvailabilityRequest;
import com.pawcarehub.backend.dto.scheduling.BookingReassignmentRequest;
import com.pawcarehub.backend.entity.Booking;
import com.pawcarehub.backend.entity.ClinicService;
import com.pawcarehub.backend.entity.Staff;
import com.pawcarehub.backend.entity.StaffAvailability;
import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.repository.BookingRepository;
import com.pawcarehub.backend.repository.ClinicServiceRepository;
import com.pawcarehub.backend.repository.PetMedicalNoteRepository;
import com.pawcarehub.backend.repository.PetRepository;
import com.pawcarehub.backend.repository.StaffAvailabilityRepository;
import com.pawcarehub.backend.repository.StaffRepository;
import com.pawcarehub.backend.repository.UserRepository;
import com.pawcarehub.backend.service.ScheduleBookingConflictException;
import com.pawcarehub.backend.service.StaffAvailabilityService;
import com.pawcarehub.backend.service.UserRoles;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SchedulingConflictIntegrationTest {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.US);

    @Autowired
    private StaffAvailabilityService staffAvailabilityService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ClinicServiceRepository clinicServiceRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private StaffAvailabilityRepository staffAvailabilityRepository;

    @Autowired
    private PetMedicalNoteRepository petMedicalNoteRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private ClinicService service;
    private LocalDate appointmentDate;

    @BeforeEach
    void cleanData() {
        bookingRepository.deleteAll();
        petMedicalNoteRepository.deleteAll();
        petRepository.deleteAll();
        userRepository.deleteAll();

        owner = new User("Jane Smith", "jane@example.com", "Secret123!");
        owner.setRole(UserRoles.USER);
        owner = userRepository.save(owner);
        createClinicUser("frontdesk@pawcarehub.com", "Casey Front Desk", UserRoles.FRONT_DESK);

        service = clinicServiceRepository.findByActiveTrueOrderByCategoryAscNameAsc().stream()
            .findFirst()
            .orElseGet(() -> clinicServiceRepository.save(new ClinicService(
                "Annual wellness exam",
                "Wellness",
                "Routine visit",
                "30 minutes",
                "$95",
                true
            )));

        appointmentDate = nextDate(DayOfWeek.TUESDAY);
    }

    @Test
    void clinicOperationsDateOverrideEndpointRejectsConflictingBookingAssignedByStaffId() throws Exception {
        Staff currentStaff = createStaffWithAvailability("Jamie Brooks", "Veterinarian", true);
        Booking booking = createBooking(currentStaff, "Upcoming");

        mockMvc.perform(post("/api/admin/operations/staff/{staffId}/schedule-exceptions", currentStaff.getId())
                .header("X-User-Email", "frontdesk@pawcarehub.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "date": "%s",
                      "available": true,
                      "customStartTime": "12:00",
                      "customEndTime": "14:00"
                    }
                    """.formatted(appointmentDate)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SCHEDULE_BOOKING_CONFLICT"))
            .andExpect(jsonPath("$.conflicts[0].bookingId").value(booking.getId()));

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStaffRecord().getId())
            .isEqualTo(currentStaff.getId());
    }

    @Test
    void clinicOperationsDateOverrideEndpointRejectsLegacyNameOnlyBookingForSameStaff() throws Exception {
        Staff currentStaff = createStaffWithAvailability("Jamie Brooks", "Veterinarian", true);
        Booking booking = createLegacyNameOnlyBooking(currentStaff, "Upcoming");

        mockMvc.perform(post("/api/admin/operations/staff/{staffId}/schedule-exceptions", currentStaff.getId())
                .header("X-User-Email", "frontdesk@pawcarehub.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "date": "%s",
                      "available": true,
                      "customStartTime": "12:00",
                      "customEndTime": "14:00"
                    }
                    """.formatted(appointmentDate)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SCHEDULE_BOOKING_CONFLICT"))
            .andExpect(jsonPath("$.conflicts[0].bookingId").value(booking.getId()));
    }

    @Test
    void availabilityUpdateWithUnresolvedActiveBookingReturnsStructuredConflict() {
        Staff currentStaff = createStaffWithAvailability("Jamie Brooks", "Veterinarian", true);
        Staff replacement = createStaffWithAvailability("Dr. Rivera", "Veterinarian", true);
        StaffAvailability currentAvailability = firstAvailability(currentStaff, appointmentDate.getDayOfWeek());
        createBooking(currentStaff, "Upcoming");

        assertThatThrownBy(() -> staffAvailabilityService.updateAvailability(
            currentStaff.getId(),
            currentAvailability.getId(),
            new UpsertStaffAvailabilityRequest("TUESDAY", "12:00", "17:00", true)
        ))
            .isInstanceOfSatisfying(ScheduleBookingConflictException.class, exception -> {
                assertThat(exception.getResponse().code()).isEqualTo("SCHEDULE_BOOKING_CONFLICT");
                assertThat(exception.getResponse().conflicts()).hasSize(1);
                assertThat(exception.getResponse().conflicts().get(0).currentStaffId()).isEqualTo(currentStaff.getId());
                assertThat(exception.getResponse().conflicts().get(0).eligibleReplacements())
                    .extracting("staffId")
                    .contains(replacement.getId());
            });
    }

    @Test
    void cancelledBookingsDoNotBlockAvailabilityChanges() {
        Staff currentStaff = createStaffWithAvailability("Jamie Brooks", "Veterinarian", true);
        StaffAvailability currentAvailability = firstAvailability(currentStaff, appointmentDate.getDayOfWeek());
        createBooking(currentStaff, "Cancelled");

        staffAvailabilityService.updateAvailability(
            currentStaff.getId(),
            currentAvailability.getId(),
            new UpsertStaffAvailabilityRequest("TUESDAY", "12:00", "17:00", true)
        );

        StaffAvailability savedAvailability = staffAvailabilityRepository.findById(currentAvailability.getId()).orElseThrow();
        assertThat(savedAvailability.getStartTime()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    void resolvingAvailabilityConflictReassignsBookingAndSavesScheduleAtomically() {
        Staff currentStaff = createStaffWithAvailability("Jamie Brooks", "Veterinarian", true);
        Staff replacement = createStaffWithAvailability("Dr. Rivera", "Veterinarian", true);
        StaffAvailability currentAvailability = firstAvailability(currentStaff, appointmentDate.getDayOfWeek());
        Booking booking = createBooking(currentStaff, "Confirmed");

        staffAvailabilityService.resolveAndUpdateAvailability(
            currentStaff.getId(),
            currentAvailability.getId(),
            new UpsertStaffAvailabilityRequest("TUESDAY", "12:00", "17:00", true),
            List.of(new BookingReassignmentRequest(booking.getId(), replacement.getId()))
        );

        Booking savedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        StaffAvailability savedAvailability = staffAvailabilityRepository.findById(currentAvailability.getId()).orElseThrow();
        assertThat(savedBooking.getStaffRecord().getId()).isEqualTo(replacement.getId());
        assertThat(savedAvailability.getStartTime()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    void staleReplacementWithOverlappingBookingRollsBackScheduleChange() {
        Staff currentStaff = createStaffWithAvailability("Jamie Brooks", "Veterinarian", true);
        Staff replacement = createStaffWithAvailability("Dr. Rivera", "Veterinarian", true);
        StaffAvailability currentAvailability = firstAvailability(currentStaff, appointmentDate.getDayOfWeek());
        Booking booking = createBooking(currentStaff, "Confirmed");
        createBooking(replacement, "Confirmed");

        assertThatThrownBy(() -> staffAvailabilityService.resolveAndUpdateAvailability(
            currentStaff.getId(),
            currentAvailability.getId(),
            new UpsertStaffAvailabilityRequest("TUESDAY", "12:00", "17:00", true),
            List.of(new BookingReassignmentRequest(booking.getId(), replacement.getId()))
        )).isInstanceOf(ScheduleBookingConflictException.class);

        StaffAvailability savedAvailability = staffAvailabilityRepository.findById(currentAvailability.getId()).orElseThrow();
        Booking savedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(savedAvailability.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(savedBooking.getStaffRecord().getId()).isEqualTo(currentStaff.getId());
    }

    private Staff createStaffWithAvailability(String name, String role, boolean active) {
        Staff staff = staffRepository.save(new Staff(name, role, active));
        staffAvailabilityRepository.save(new StaffAvailability(
            staff,
            appointmentDate.getDayOfWeek(),
            LocalTime.of(9, 0),
            LocalTime.of(17, 0),
            true
        ));
        return staff;
    }

    private StaffAvailability firstAvailability(Staff staff, DayOfWeek dayOfWeek) {
        return staffAvailabilityRepository.findByStaffIdAndDayOfWeekOrderByStartTimeAsc(staff.getId(), dayOfWeek)
            .stream()
            .findFirst()
            .orElseThrow();
    }

    private Booking createBooking(Staff staff, String status) {
        return bookingRepository.save(new Booking(
            "Luna",
            service.getName(),
            appointmentDate.format(DATE_FORMATTER),
            "11:00 AM",
            status,
            "PawCare Hub Clinic",
            staff.getName(),
            service,
            staff,
            owner
        ));
    }

    private Booking createLegacyNameOnlyBooking(Staff staff, String status) {
        return bookingRepository.save(new Booking(
            "Luna",
            service.getName(),
            appointmentDate.format(DATE_FORMATTER),
            "11:00 AM",
            status,
            "PawCare Hub Clinic",
            staff.getName(),
            service,
            null,
            owner
        ));
    }

    private void createClinicUser(String email, String name, String role) {
        User user = new User(name, email, "Secret123!");
        user.setActive(true);
        user.setRole(role);
        userRepository.save(user);
    }

    private LocalDate nextDate(DayOfWeek dayOfWeek) {
        LocalDate date = LocalDate.now().plusDays(14);
        while (date.getDayOfWeek() != dayOfWeek) {
            date = date.plusDays(1);
        }
        return date;
    }
}
