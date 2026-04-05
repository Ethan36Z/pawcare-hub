package com.pawcarehub.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pawcarehub.backend.entity.Booking;
import com.pawcarehub.backend.entity.ClinicService;
import com.pawcarehub.backend.entity.Staff;
import com.pawcarehub.backend.entity.StaffAvailability;
import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.repository.BookingRepository;
import com.pawcarehub.backend.repository.ClinicServiceRepository;
import com.pawcarehub.backend.repository.PetRepository;
import com.pawcarehub.backend.repository.StaffAvailabilityRepository;
import com.pawcarehub.backend.repository.StaffRepository;
import com.pawcarehub.backend.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendHappyPathIntegrationTest {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.US);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ClinicServiceRepository clinicServiceRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private StaffAvailabilityRepository staffAvailabilityRepository;

    @BeforeEach
    void cleanUserData() {
        bookingRepository.deleteAll();
        petRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerCreatesUserWithHashedPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Jamie Parker",
                      "email": "jamie@example.com",
                      "password": "Secret123!"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.message").value("Registration successful"))
            .andExpect(jsonPath("$.email").value("jamie@example.com"))
            .andExpect(jsonPath("$.name").value("Jamie Parker"));

        User savedUser = userRepository.findByEmail("jamie@example.com").orElseThrow();
        assertThat(savedUser.getPassword()).startsWith("$2");
        assertThat(savedUser.getPassword()).isNotEqualTo("Secret123!");
        assertThat(savedUser.isActive()).isTrue();
    }

    @Test
    void loginReturnsAuthenticatedUserForValidCredentials() throws Exception {
        registerUser("jamie@example.com");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "jamie@example.com",
                      "password": "Secret123!"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Login successful"))
            .andExpect(jsonPath("$.email").value("jamie@example.com"))
            .andExpect(jsonPath("$.name").value("Jamie Parker"));
    }

    @Test
    void createPetAddsPetForCurrentUser() throws Exception {
        registerUser("jamie@example.com");

        mockMvc.perform(post("/api/pets")
                .header("X-User-Email", "jamie@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Milo",
                      "species": "Dog",
                      "breed": "Corgi",
                      "age": "3 years",
                      "weight": "28 lbs",
                      "note": "Friendly during checkups",
                      "status": "Healthy"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Milo"))
            .andExpect(jsonPath("$.species").value("Dog"))
            .andExpect(jsonPath("$.breed").value("Corgi"))
            .andExpect(jsonPath("$.status").value("Healthy"));
    }

    @Test
    void createBookingUsesRealServiceRecord() throws Exception {
        registerUser("jamie@example.com");
        ClinicService service = clinicServiceRepository.findByActiveTrueOrderByCategoryAscNameAsc().stream()
            .findFirst()
            .orElseThrow();
        Staff staff = staffRepository.findAllByOrderByActiveDescNameAsc().stream()
            .findFirst()
            .orElseThrow();

        MvcResult createResult = mockMvc.perform(post("/api/bookings")
                .header("X-User-Email", "jamie@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "petName": "Milo",
                      "serviceId": %d,
                      "service": "%s",
                      "date": "May 10, 2026",
                      "time": "10:30 AM",
                      "status": "Upcoming",
                      "clinic": "PawCare Hub Clinic",
                      "staffId": %d,
                      "staff": "%s"
                    }
                    """.formatted(service.getId(), service.getName(), staff.getId(), staff.getName())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.petName").value("Milo"))
            .andExpect(jsonPath("$.serviceId").value(service.getId()))
            .andExpect(jsonPath("$.service").value(service.getName()))
            .andExpect(jsonPath("$.staffId").value(staff.getId()))
            .andExpect(jsonPath("$.staff").value(staff.getName()))
            .andExpect(jsonPath("$.status").value("Upcoming"))
            .andReturn();

        JsonNode bookingJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long bookingId = bookingJson.get("id").asLong();
        Booking savedBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(savedBooking.getServiceRecord()).isNotNull();
        assertThat(savedBooking.getStaffRecord()).isNotNull();
        assertThat(savedBooking.getStaffRecord().getId()).isEqualTo(staff.getId());
    }

    @Test
    void createBookingRejectsAppointmentsWithinOneHour() throws Exception {
        registerUser("jamie@example.com");
        ClinicService service = clinicServiceRepository.findByActiveTrueOrderByCategoryAscNameAsc().stream()
            .findFirst()
            .orElseThrow();
        Staff staff = staffRepository.findAllByOrderByActiveDescNameAsc().stream()
            .findFirst()
            .orElseThrow();
        LocalDateTime nearFuture = LocalDateTime.now().plusMinutes(30);

        mockMvc.perform(post("/api/bookings")
                .header("X-User-Email", "jamie@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "petName": "Milo",
                      "serviceId": %d,
                      "service": "%s",
                      "date": "%s",
                      "time": "%s",
                      "status": "Upcoming",
                      "clinic": "PawCare Hub Clinic",
                      "staffId": %d,
                      "staff": "%s"
                    }
                    """.formatted(
                    service.getId(),
                    service.getName(),
                    nearFuture.format(DATE_FORMATTER),
                    nearFuture.format(TIME_FORMATTER),
                    staff.getId(),
                    staff.getName()
                )))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Appointments must be scheduled at least 1 hour in advance"));
    }

    @Test
    void createBookingRejectsTimeOutsideSelectedStaffAvailability() throws Exception {
        registerUser("jamie@example.com");
        ClinicService service = clinicServiceRepository.findByActiveTrueOrderByCategoryAscNameAsc().stream()
            .findFirst()
            .orElseThrow();
        Staff staff = staffRepository.findByActiveTrueOrderByNameAsc().stream()
            .findFirst()
            .orElseThrow();

        mockMvc.perform(post("/api/bookings")
                .header("X-User-Email", "jamie@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "petName": "Milo",
                      "serviceId": %d,
                      "service": "%s",
                      "date": "May 12, 2026",
                      "time": "7:30 AM",
                      "status": "Upcoming",
                      "clinic": "PawCare Hub Clinic",
                      "staffId": %d,
                      "staff": "%s"
                    }
                    """.formatted(service.getId(), service.getName(), staff.getId(), staff.getName())))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Selected appointment time is outside this staff member's availability"));
    }

    @Test
    void adminCanManageStaffAvailabilityRecords() throws Exception {
        Staff staff = staffRepository.save(new Staff("Dr. Foster", "Veterinarian", true));

        MvcResult createResult = mockMvc.perform(post("/api/admin/staff/{staffId}/availability", staff.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "dayOfWeek": "MONDAY",
                      "startTime": "17:00",
                      "endTime": "18:00",
                      "active": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.staffId").value(staff.getId()))
            .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
            .andExpect(jsonPath("$.startTime").value("17:00"))
            .andExpect(jsonPath("$.endTime").value("18:00"))
            .andReturn();

        long availabilityId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/admin/staff/{staffId}/availability/{availabilityId}", staff.getId(), availabilityId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "dayOfWeek": "MONDAY",
                      "startTime": "17:30",
                      "endTime": "18:30",
                      "active": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startTime").value("17:30"))
            .andExpect(jsonPath("$.endTime").value("18:30"));

        mockMvc.perform(patch("/api/admin/staff/{staffId}/availability/{availabilityId}/toggle", staff.getId(), availabilityId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/admin/staff/{staffId}/availability", staff.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].staffId").value(staff.getId()));

        mockMvc.perform(delete("/api/admin/staff/{staffId}/availability/{availabilityId}", staff.getId(), availabilityId))
            .andExpect(status().isNoContent());
    }

    @Test
    void adminRejectsOverlappingAvailabilitySlots() throws Exception {
        Staff staff = staffRepository.save(new Staff("Dr. Stone", "Veterinarian", true));

        mockMvc.perform(post("/api/admin/staff/{staffId}/availability", staff.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "dayOfWeek": "TUESDAY",
                      "startTime": "09:00",
                      "endTime": "11:00",
                      "active": true
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/staff/{staffId}/availability", staff.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "dayOfWeek": "TUESDAY",
                      "startTime": "10:30",
                      "endTime": "12:00",
                      "active": true
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Availability slots cannot overlap for the same staff member and day"));
    }

    @Test
    void bookingFlowCanListStaffAvailabilityForSelectedDate() throws Exception {
        Staff staff = staffRepository.findByActiveTrueOrderByNameAsc().stream()
            .findFirst()
            .orElseThrow();

        mockMvc.perform(get("/api/staff/{staffId}/availability", staff.getId())
                .param("date", "2026-05-12"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].staffId").value(staff.getId()))
            .andExpect(jsonPath("$[0].date").value("2026-05-12"))
            .andExpect(jsonPath("$[0].source").value("weekly"));
    }

    @Test
    void adminCanManageScheduleExceptionsAndRejectDuplicates() throws Exception {
        Staff staff = staffRepository.save(new Staff("Dr. Avery", "Veterinarian", true));

        MvcResult createResult = mockMvc.perform(post("/api/admin/operations/staff/{staffId}/schedule-exceptions", staff.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "date": "2026-05-15",
                      "available": false,
                      "customStartTime": null,
                      "customEndTime": null
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.staffId").value(staff.getId()))
            .andExpect(jsonPath("$.date").value("2026-05-15"))
            .andExpect(jsonPath("$.available").value(false))
            .andReturn();

        long exceptionId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/admin/operations/staff/{staffId}/schedule-exceptions", staff.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "date": "2026-05-15",
                      "available": true,
                      "customStartTime": "12:00",
                      "customEndTime": "16:00"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(status().reason("A schedule exception already exists for this staff member and date"));

        mockMvc.perform(patch("/api/admin/operations/staff/{staffId}/schedule-exceptions/{exceptionId}", staff.getId(), exceptionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "date": "2026-05-15",
                      "available": true,
                      "customStartTime": "12:00",
                      "customEndTime": "16:00"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(true))
            .andExpect(jsonPath("$.customStartTime").value("12:00"))
            .andExpect(jsonPath("$.customEndTime").value("16:00"));

        mockMvc.perform(get("/api/admin/operations/staff/{staffId}/schedule-exceptions", staff.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].date").value("2026-05-15"));

        mockMvc.perform(delete("/api/admin/operations/staff/{staffId}/schedule-exceptions/{exceptionId}", staff.getId(), exceptionId))
            .andExpect(status().isNoContent());
    }

    @Test
    void availabilityEndpointUsesInactiveDateOverrideBeforeWeeklyAvailability() throws Exception {
        Staff staff = staffRepository.save(new Staff("Dr. Quinn", "Veterinarian", true));
        createFullWeekAvailability(staff);

        mockMvc.perform(post("/api/admin/operations/staff/{staffId}/schedule-exceptions", staff.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "date": "2026-05-12",
                      "available": false,
                      "customStartTime": null,
                      "customEndTime": null
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/staff/{staffId}/availability", staff.getId())
                .param("date", "2026-05-12"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void availabilityEndpointUsesCustomTimeOverrideBeforeWeeklyAvailability() throws Exception {
        Staff staff = staffRepository.save(new Staff("Dr. Blake", "Veterinarian", true));
        createFullWeekAvailability(staff);

        mockMvc.perform(post("/api/admin/operations/staff/{staffId}/schedule-exceptions", staff.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "date": "2026-05-13",
                      "available": true,
                      "customStartTime": "12:00",
                      "customEndTime": "16:00"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/staff/{staffId}/availability", staff.getId())
                .param("date", "2026-05-13"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].staffId").value(staff.getId()))
            .andExpect(jsonPath("$[0].date").value("2026-05-13"))
            .andExpect(jsonPath("$[0].startTime").value("12:00"))
            .andExpect(jsonPath("$[0].endTime").value("16:00"))
            .andExpect(jsonPath("$[0].source").value("exception"));
    }

    @Test
    void availabilityEndpointFallsBackToWeeklyTemplateWhenNoExceptionExists() throws Exception {
        Staff staff = staffRepository.save(new Staff("Dr. Perez", "Veterinarian", true));
        createFullWeekAvailability(staff);

        mockMvc.perform(get("/api/staff/{staffId}/availability", staff.getId())
                .param("date", "2026-05-14"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].staffId").value(staff.getId()))
            .andExpect(jsonPath("$[0].date").value("2026-05-14"))
            .andExpect(jsonPath("$[0].startTime").value("08:00"))
            .andExpect(jsonPath("$[0].endTime").value("17:00"))
            .andExpect(jsonPath("$[0].source").value("weekly"));
    }

    @Test
    void bookingValidationUsesScheduleExceptionsBeforeWeeklyAvailability() throws Exception {
        registerUser("jamie@example.com");
        ClinicService service = clinicServiceRepository.findByActiveTrueOrderByCategoryAscNameAsc().stream()
            .findFirst()
            .orElseThrow();
        Staff staff = staffRepository.save(new Staff("Dr. Reese", "Veterinarian", true));
        createFullWeekAvailability(staff);

        mockMvc.perform(post("/api/admin/operations/staff/{staffId}/schedule-exceptions", staff.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "date": "2026-05-14",
                      "available": true,
                      "customStartTime": "12:00",
                      "customEndTime": "16:00"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/bookings")
                .header("X-User-Email", "jamie@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "petName": "Milo",
                      "serviceId": %d,
                      "service": "%s",
                      "date": "May 14, 2026",
                      "time": "1:00 PM",
                      "status": "Upcoming",
                      "clinic": "PawCare Hub Clinic",
                      "staffId": %d,
                      "staff": "%s"
                    }
                    """.formatted(service.getId(), service.getName(), staff.getId(), staff.getName())))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/bookings")
                .header("X-User-Email", "jamie@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "petName": "Milo",
                      "serviceId": %d,
                      "service": "%s",
                      "date": "May 14, 2026",
                      "time": "10:00 AM",
                      "status": "Upcoming",
                      "clinic": "PawCare Hub Clinic",
                      "staffId": %d,
                      "staff": "%s"
                    }
                    """.formatted(service.getId(), service.getName(), staff.getId(), staff.getName())))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Selected appointment time is outside this staff member's availability"));
    }

    @Test
    void adminCanListRealStaffRecords() throws Exception {
        mockMvc.perform(get("/api/admin/staff"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").isNumber())
            .andExpect(jsonPath("$[0].name").isNotEmpty())
            .andExpect(jsonPath("$[0].role").isNotEmpty())
            .andExpect(jsonPath("$[0].active").isBoolean());
    }

    @Test
    void adminCanToggleStaffActiveState() throws Exception {
        Staff staff = staffRepository.findByActiveTrueOrderByNameAsc().stream()
            .findFirst()
            .orElseThrow();
        boolean initialActive = staff.isActive();

        mockMvc.perform(patch("/api/admin/staff/{id}/toggle", staff.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(staff.getId()))
            .andExpect(jsonPath("$.active").value(!initialActive));

        Staff updatedStaff = staffRepository.findById(staff.getId()).orElseThrow();
        assertThat(updatedStaff.isActive()).isEqualTo(!initialActive);
    }

    @Test
    void adminCanCreateStaffRecord() throws Exception {
        mockMvc.perform(post("/api/admin/staff")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Dr. Lopez",
                      "role": "Veterinarian",
                      "active": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Dr. Lopez"))
            .andExpect(jsonPath("$.role").value("Veterinarian"))
            .andExpect(jsonPath("$.active").value(true));

        assertThat(staffRepository.existsByNameIgnoreCaseAndRoleIgnoreCase("Dr. Lopez", "Veterinarian")).isTrue();
    }

    @Test
    void adminCanEditStaffRecord() throws Exception {
        Staff staff = staffRepository.findAllByOrderByActiveDescNameAsc().stream()
            .findFirst()
            .orElseThrow();

        mockMvc.perform(patch("/api/admin/staff/{id}", staff.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Dr. Rivera Updated",
                      "role": "Lead Veterinarian",
                      "active": false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(staff.getId()))
            .andExpect(jsonPath("$.name").value("Dr. Rivera Updated"))
            .andExpect(jsonPath("$.role").value("Lead Veterinarian"))
            .andExpect(jsonPath("$.active").value(false));

        Staff updatedStaff = staffRepository.findById(staff.getId()).orElseThrow();
        assertThat(updatedStaff.getName()).isEqualTo("Dr. Rivera Updated");
        assertThat(updatedStaff.getRole()).isEqualTo("Lead Veterinarian");
        assertThat(updatedStaff.isActive()).isFalse();
    }

    @Test
    void bookingFlowCanListActiveStaffRecords() throws Exception {
        mockMvc.perform(get("/api/staff"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").isNumber())
            .andExpect(jsonPath("$[0].name").isNotEmpty())
            .andExpect(jsonPath("$[0].role").isNotEmpty())
            .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void adminCanConfirmBooking() throws Exception {
        registerUser("jamie@example.com");
        ClinicService service = clinicServiceRepository.findByActiveTrueOrderByCategoryAscNameAsc().stream()
            .findFirst()
            .orElseThrow();

        MvcResult createResult = mockMvc.perform(post("/api/bookings")
                .header("X-User-Email", "jamie@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "petName": "Milo",
                      "serviceId": %d,
                      "service": "%s",
                      "date": "May 10, 2026",
                      "time": "10:30 AM",
                      "status": "Upcoming",
                      "clinic": "PawCare Hub Clinic",
                      "staff": "Dr. Rivera"
                    }
                    """.formatted(service.getId(), service.getName())))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode bookingJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long bookingId = bookingJson.get("id").asLong();

        mockMvc.perform(patch("/api/admin/bookings/{id}/confirm", bookingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(bookingId))
            .andExpect(jsonPath("$.status").value("Confirmed"))
            .andExpect(jsonPath("$.service").value(service.getName()))
            .andExpect(jsonPath("$.ownerEmail").value("jamie@example.com"));

        Booking savedBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(savedBooking.getStatus()).isEqualTo("Confirmed");
    }

    @Test
    void rescheduleBookingCanUseRealStaffSelection() throws Exception {
        registerUser("jamie@example.com");
        ClinicService service = clinicServiceRepository.findByActiveTrueOrderByCategoryAscNameAsc().stream()
            .findFirst()
            .orElseThrow();
        Staff staff = staffRepository.save(new Staff("Dr. Morris", "Veterinarian", true));
        Staff nextStaff = staffRepository.save(new Staff("Dr. Campbell", "Veterinarian", true));
        createFullWeekAvailability(staff);
        createFullWeekAvailability(nextStaff);

        MvcResult createResult = mockMvc.perform(post("/api/bookings")
                .header("X-User-Email", "jamie@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "petName": "Milo",
                      "serviceId": %d,
                      "service": "%s",
                      "date": "May 10, 2026",
                      "time": "10:30 AM",
                      "status": "Upcoming",
                      "clinic": "PawCare Hub Clinic",
                      "staffId": %d,
                      "staff": "%s"
                    }
                    """.formatted(service.getId(), service.getName(), staff.getId(), staff.getName())))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode bookingJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long bookingId = bookingJson.get("id").asLong();

        mockMvc.perform(patch("/api/bookings/{id}/reschedule", bookingId)
                .header("X-User-Email", "jamie@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "date": "May 12, 2026",
                      "time": "1:15 PM",
                      "staffId": %d,
                      "staff": "%s"
                    }
                    """.formatted(nextStaff.getId(), nextStaff.getName())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(bookingId))
            .andExpect(jsonPath("$.date").value("May 12, 2026"))
            .andExpect(jsonPath("$.time").value("1:15 PM"))
            .andExpect(jsonPath("$.staffId").value(nextStaff.getId()))
            .andExpect(jsonPath("$.staff").value(nextStaff.getName()));

        Booking updatedBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(updatedBooking.getStaffRecord()).isNotNull();
        assertThat(updatedBooking.getStaffRecord().getId()).isEqualTo(nextStaff.getId());
    }

    private void createFullWeekAvailability(Staff staff) {
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            staffAvailabilityRepository.save(new StaffAvailability(
                staff,
                dayOfWeek,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                true
            ));
        }
    }

    private void registerUser(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Jamie Parker",
                      "email": "%s",
                      "password": "Secret123!"
                    }
                    """.formatted(email)))
            .andExpect(status().isCreated());
    }
}
