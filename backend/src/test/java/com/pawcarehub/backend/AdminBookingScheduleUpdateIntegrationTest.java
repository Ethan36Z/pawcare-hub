package com.pawcarehub.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.pawcarehub.backend.repository.StaffScheduleExceptionRepository;
import com.pawcarehub.backend.repository.UserRepository;
import com.pawcarehub.backend.security.JwtService;
import com.pawcarehub.backend.service.UserRoles;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminBookingScheduleUpdateIntegrationTest {
    private static final DateTimeFormatter BOOKING_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.US);
    private static final String FRONT_DESK_EMAIL = "frontdesk-booking-edit@pawcarehub.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ClinicServiceRepository clinicServiceRepository;

    @Autowired
    private PetMedicalNoteRepository petMedicalNoteRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private StaffAvailabilityRepository staffAvailabilityRepository;

    @Autowired
    private StaffScheduleExceptionRepository staffScheduleExceptionRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private ClinicService service;
    private LocalDate appointmentDate;
    private LocalDate alternateDate;

    @BeforeEach
    void cleanData() {
        bookingRepository.deleteAll();
        petMedicalNoteRepository.deleteAll();
        petRepository.deleteAll();
        staffScheduleExceptionRepository.deleteAll();
        staffAvailabilityRepository.deleteAll();
        staffRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(newUser("Jordan Lee", "jordan-booking-edit@example.com", UserRoles.USER));
        userRepository.save(newUser("Casey Front Desk", FRONT_DESK_EMAIL, UserRoles.FRONT_DESK));

        service = clinicServiceRepository.findByActiveTrueOrderByCategoryAscNameAsc().stream()
            .findFirst()
            .orElseGet(() -> clinicServiceRepository.save(new ClinicService(
                "Wellness check",
                "Wellness",
                "Routine appointment",
                "30 minutes",
                "$95",
                true
            )));

        appointmentDate = nextDate(DayOfWeek.TUESDAY);
        alternateDate = nextDate(DayOfWeek.WEDNESDAY);
    }

    @Test
    void upcomingBookingCanChangeToAvailableStaffMember() throws Exception {
        Staff originalStaff = createStaffWithAvailability("Jamie Brooks", true, appointmentDate, LocalTime.of(9, 0), LocalTime.of(17, 0));
        Staff newStaff = createStaffWithAvailability("Dr. Rivera", true, appointmentDate, LocalTime.of(9, 0), LocalTime.of(17, 0));
        Booking booking = createBooking(originalStaff, "Upcoming", appointmentDate, "11:00 AM");

        updateSchedule(booking, newStaff, appointmentDate, "11:00")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.staffId").value(newStaff.getId()))
            .andExpect(jsonPath("$.staff").value(newStaff.getName()));

        Booking savedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(savedBooking.getStaffRecord().getId()).isEqualTo(newStaff.getId());
    }

    @Test
    void confirmedBookingCanChangeToAvailableStaffMember() throws Exception {
        Staff originalStaff = createStaffWithAvailability("Jamie Brooks", true, appointmentDate, LocalTime.of(9, 0), LocalTime.of(17, 0));
        Staff newStaff = createStaffWithAvailability("Dr. Rivera", true, appointmentDate, LocalTime.of(9, 0), LocalTime.of(17, 0));
        Booking booking = createBooking(originalStaff, "Confirmed", appointmentDate, "11:00 AM");

        updateSchedule(booking, newStaff, appointmentDate, "12:00")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Confirmed"))
            .andExpect(jsonPath("$.staffId").value(newStaff.getId()));
    }

    @Test
    void upcomingBookingCanChangeDateAndTimeWhenSlotIsValid() throws Exception {
        Staff staff = createStaffWithAvailability("Jamie Brooks", true, appointmentDate, LocalTime.of(9, 0), LocalTime.of(17, 0));
        addAvailability(staff, alternateDate, LocalTime.of(9, 0), LocalTime.of(17, 0));
        Booking booking = createBooking(staff, "Upcoming", appointmentDate, "11:00 AM");

        updateSchedule(booking, staff, alternateDate, "14:30")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.date").value(alternateDate.format(BOOKING_DATE_FORMATTER)))
            .andExpect(jsonPath("$.time").value("2:30 PM"));
    }

    @Test
    void completedBookingEditIsRejected() throws Exception {
        Staff staff = createStaffWithAvailability("Jamie Brooks", true, appointmentDate, LocalTime.of(9, 0), LocalTime.of(17, 0));
        Booking booking = createBooking(staff, "Completed", appointmentDate, "11:00 AM");

        updateSchedule(booking, staff, appointmentDate, "12:00")
            .andExpect(status().isBadRequest());
    }

    @Test
    void cancelledBookingEditIsRejected() throws Exception {
        Staff staff = createStaffWithAvailability("Jamie Brooks", true, appointmentDate, LocalTime.of(9, 0), LocalTime.of(17, 0));
        Booking booking = createBooking(staff, "Cancelled", appointmentDate, "11:00 AM");

        updateSchedule(booking, staff, appointmentDate, "12:00")
            .andExpect(status().isBadRequest());
    }

    @Test
    void inactiveStaffIsRejected() throws Exception {
        Staff activeStaff = createStaffWithAvailability("Jamie Brooks", true, appointmentDate, LocalTime.of(9, 0), LocalTime.of(17, 0));
        Staff inactiveStaff = createStaffWithAvailability("Dr. Rivera", false, appointmentDate, LocalTime.of(9, 0), LocalTime.of(17, 0));
        Booking booking = createBooking(activeStaff, "Upcoming", appointmentDate, "11:00 AM");

        updateSchedule(booking, inactiveStaff, appointmentDate, "12:00")
            .andExpect(status().isBadRequest());
    }

    @Test
    void staffOutsideWorkingHoursIsRejected() throws Exception {
        Staff staff = createStaffWithAvailability("Jamie Brooks", true, appointmentDate, LocalTime.of(12, 0), LocalTime.of(17, 0));
        Booking booking = createBooking(staff, "Upcoming", appointmentDate, "12:00 PM");

        updateSchedule(booking, staff, appointmentDate, "11:00")
            .andExpect(status().isBadRequest());
    }

    @Test
    void overlappingActiveBookingIsRejected() throws Exception {
        Staff staff = createStaffWithAvailability("Jamie Brooks", true, appointmentDate, LocalTime.of(9, 0), LocalTime.of(17, 0));
        Booking booking = createBooking(staff, "Upcoming", appointmentDate, "11:00 AM");
        createBooking(staff, "Confirmed", appointmentDate, "12:00 PM");

        updateSchedule(booking, staff, appointmentDate, "12:15")
            .andExpect(status().isBadRequest());
    }

    @Test
    void appointmentEndingAfterStaffHoursIsRejected() throws Exception {
        Staff staff = createStaffWithAvailability("Jamie Brooks", true, appointmentDate, LocalTime.of(9, 0), LocalTime.of(16, 0));
        Booking booking = createBooking(staff, "Upcoming", appointmentDate, "11:00 AM");

        updateSchedule(booking, staff, appointmentDate, "15:45")
            .andExpect(status().isBadRequest());
    }

    @Test
    void appointmentOutsideClinicHoursIsRejected() throws Exception {
        Staff staff = createStaffWithAvailability("Jamie Brooks", true, appointmentDate, LocalTime.of(6, 0), LocalTime.of(17, 0));
        Booking booking = createBooking(staff, "Upcoming", appointmentDate, "11:00 AM");

        updateSchedule(booking, staff, appointmentDate, "08:30")
            .andExpect(status().isBadRequest());
    }

    @Test
    void legacyNameOnlyBookingIsUpdatedToUseValidStaffId() throws Exception {
        Staff staff = createStaffWithAvailability("Jamie Brooks", true, appointmentDate, LocalTime.of(9, 0), LocalTime.of(17, 0));
        Booking booking = createLegacyNameOnlyBooking(staff, "Upcoming", appointmentDate, "11:00 AM");

        updateSchedule(booking, staff, appointmentDate, "12:00")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.staffId").value(staff.getId()));

        Booking savedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(savedBooking.getStaffRecord()).isNotNull();
        assertThat(savedBooking.getStaffRecord().getId()).isEqualTo(staff.getId());
    }

    @Test
    void directApiRequestsCannotBypassOverlapValidation() throws Exception {
        Staff staff = createStaffWithAvailability("Jamie Brooks", true, appointmentDate, LocalTime.of(9, 0), LocalTime.of(17, 0));
        Booking booking = createBooking(staff, "Upcoming", appointmentDate, "11:00 AM");
        createBooking(staff, "Upcoming", appointmentDate, "1:00 PM");

        updateSchedule(booking, staff, appointmentDate, "13:00")
            .andExpect(status().isBadRequest());

        Booking savedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(savedBooking.getTime()).isEqualTo("11:00 AM");
    }

    private org.springframework.test.web.servlet.ResultActions updateSchedule(
        Booking booking,
        Staff staff,
        LocalDate date,
        String time
    ) throws Exception {
        return mockMvc.perform(patch("/api/admin/bookings/{id}/schedule", booking.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(FRONT_DESK_EMAIL))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "staffId": %d,
                  "date": "%s",
                  "time": "%s"
                }
                """.formatted(staff.getId(), date, time)));
    }

    private Booking createBooking(Staff staff, String status, LocalDate date, String time) {
        return bookingRepository.save(new Booking(
            "Milo",
            service.getName(),
            date.format(BOOKING_DATE_FORMATTER),
            time,
            status,
            "PawCare Hub Clinic",
            staff.getName(),
            service,
            staff,
            owner
        ));
    }

    private Booking createLegacyNameOnlyBooking(Staff staff, String status, LocalDate date, String time) {
        return bookingRepository.save(new Booking(
            "Milo",
            service.getName(),
            date.format(BOOKING_DATE_FORMATTER),
            time,
            status,
            "PawCare Hub Clinic",
            staff.getName(),
            service,
            null,
            owner
        ));
    }

    private Staff createStaffWithAvailability(
        String name,
        boolean active,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
    ) {
        Staff staff = staffRepository.save(new Staff(name, "Veterinarian", active));
        addAvailability(staff, date, startTime, endTime);
        return staff;
    }

    private void addAvailability(Staff staff, LocalDate date, LocalTime startTime, LocalTime endTime) {
        staffAvailabilityRepository.save(new StaffAvailability(
            staff,
            date.getDayOfWeek(),
            startTime,
            endTime,
            true
        ));
    }

    private User newUser(String name, String email, String role) {
        User user = new User(name, email, "Secret123!");
        user.setActive(true);
        user.setRole(role);
        return user;
    }

    private LocalDate nextDate(DayOfWeek dayOfWeek) {
        LocalDate date = LocalDate.now().plusDays(14);
        while (date.getDayOfWeek() != dayOfWeek) {
            date = date.plusDays(1);
        }
        return date;
    }

    private String bearerToken(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return "Bearer " + jwtService.generateToken(user);
    }

}
