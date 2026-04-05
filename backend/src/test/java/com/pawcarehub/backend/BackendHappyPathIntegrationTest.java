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
import com.pawcarehub.backend.entity.PetMedicalNote;
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
import com.pawcarehub.backend.service.UserRoles;
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
    private static final String ADMIN_EMAIL = "admin@pawcarehub.com";
    private static final String FRONT_DESK_EMAIL = "frontdesk@pawcarehub.com";
    private static final String DOCTOR_EMAIL = "doctor@pawcarehub.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private PetMedicalNoteRepository petMedicalNoteRepository;

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
        petMedicalNoteRepository.deleteAll();
        petRepository.deleteAll();
        userRepository.deleteAll();
        createClinicUser(ADMIN_EMAIL, "Admin Lead", UserRoles.ADMIN);
        createClinicUser(FRONT_DESK_EMAIL, "Casey Front Desk", UserRoles.FRONT_DESK);
        createClinicUser(DOCTOR_EMAIL, "Dr. Rivera", UserRoles.DOCTOR);
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
            .andExpect(jsonPath("$.name").value("Jamie Parker"))
            .andExpect(jsonPath("$.role").value(UserRoles.USER));

        User savedUser = userRepository.findByEmail("jamie@example.com").orElseThrow();
        assertThat(savedUser.getPassword()).startsWith("$2");
        assertThat(savedUser.getPassword()).isNotEqualTo("Secret123!");
        assertThat(savedUser.isActive()).isTrue();
    }

    @Test
    void loginReturnsAuthenticatedUserForValidCredentials() throws Exception {
        registerUser("jamie@example.com");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
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
            .andExpect(jsonPath("$.name").value("Jamie Parker"))
            .andExpect(jsonPath("$.role").value(UserRoles.USER))
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn();

        String jwtToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(get("/api/auth/profile")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
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
                      "sex": "Male",
                      "dateOfBirth": "2023-01-05",
                      "color": "Golden and white",
                      "microchipNumber": "MC-1001",
                      "allergies": "None reported",
                      "chronicConditions": "",
                      "medications": "",
                      "vaccinationNotes": "Core vaccines up to date",
                      "generalMedicalNotes": "Enjoys treats after exams",
                      "status": "Healthy"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Milo"))
            .andExpect(jsonPath("$.species").value("Dog"))
            .andExpect(jsonPath("$.breed").value("Corgi"))
            .andExpect(jsonPath("$.sex").value("Male"))
            .andExpect(jsonPath("$.dateOfBirth").value("2023-01-05"))
            .andExpect(jsonPath("$.microchipNumber").value("MC-1001"))
            .andExpect(jsonPath("$.status").value("Healthy"));
    }

    @Test
    void updatePetPersistsMedicalRecordFieldsAndNotesCanBeAdded() throws Exception {
        registerUser("jamie@example.com");

        MvcResult createPetResult = mockMvc.perform(post("/api/pets")
                .header("X-User-Email", "jamie@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Luna",
                      "species": "Cat",
                      "breed": "Siamese",
                      "age": "4 years",
                      "weight": "9 lbs",
                      "note": "Calm during visits",
                      "sex": "Female",
                      "dateOfBirth": "2022-03-12",
                      "color": "Cream",
                      "microchipNumber": "MC-2222",
                      "allergies": "Chicken",
                      "chronicConditions": "Mild asthma",
                      "medications": "Inhaler as needed",
                      "vaccinationNotes": "Rabies current through 2026",
                      "generalMedicalNotes": "Monitor breathing after exercise",
                      "status": "Healthy"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.generalMedicalNotes").value("Monitor breathing after exercise"))
            .andReturn();

        long petId = objectMapper.readTree(createPetResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/pets/{id}", petId)
                .header("X-User-Email", "jamie@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Luna",
                      "species": "Cat",
                      "breed": "Siamese",
                      "age": "4 years",
                      "weight": "9.5 lbs",
                      "note": "Prefers a quiet exam room",
                      "sex": "Female",
                      "dateOfBirth": "2022-03-12",
                      "color": "Cream and gray",
                      "microchipNumber": "MC-2222",
                      "allergies": "Chicken",
                      "chronicConditions": "Mild asthma",
                      "medications": "Inhaler as needed",
                      "vaccinationNotes": "Rabies current through 2026",
                      "generalMedicalNotes": "Monitor breathing after exercise and grooming",
                      "status": "Needs attention"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weight").value("9.5 lbs"))
            .andExpect(jsonPath("$.color").value("Cream and gray"))
            .andExpect(jsonPath("$.status").value("Needs attention"));

        mockMvc.perform(post("/api/pets/{id}/medical-notes", petId)
                .header("X-User-Email", "jamie@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "date": "2026-04-04",
                      "author": "Dr. Rivera",
                      "noteText": "Follow-up exam showed stable breathing."
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.date").value("2026-04-04"))
            .andExpect(jsonPath("$.author").value("Dr. Rivera"))
            .andExpect(jsonPath("$.noteText").value("Follow-up exam showed stable breathing."))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        mockMvc.perform(get("/api/pets/{id}", petId)
                .header("X-User-Email", "jamie@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.microchipNumber").value("MC-2222"))
            .andExpect(jsonPath("$.generalMedicalNotes").value("Monitor breathing after exercise and grooming"))
            .andExpect(jsonPath("$.medicalNotes[0].author").value("Dr. Rivera"))
            .andExpect(jsonPath("$.medicalNotes[0].noteText").value("Follow-up exam showed stable breathing."))
            .andExpect(jsonPath("$.medicalNotes[0].relatedVisit").doesNotExist())
            .andExpect(jsonPath("$.medicalNotes[0].createdAt").isNotEmpty())
            .andExpect(jsonPath("$.medicalNotes[0].updatedAt").isNotEmpty());
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
                      "ownerNote": "Please be gentle around the front paw.",
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
            .andExpect(jsonPath("$.ownerNote").value("Please be gentle around the front paw."))
            .andReturn();

        JsonNode bookingJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long bookingId = bookingJson.get("id").asLong();
        Booking savedBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(savedBooking.getServiceRecord()).isNotNull();
        assertThat(savedBooking.getStaffRecord()).isNotNull();
        assertThat(savedBooking.getStaffRecord().getId()).isEqualTo(staff.getId());
        assertThat(savedBooking.getOwnerNote()).isEqualTo("Please be gentle around the front paw.");
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
                .header("X-User-Email", FRONT_DESK_EMAIL)
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
                .header("X-User-Email", FRONT_DESK_EMAIL)
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

        mockMvc.perform(patch("/api/admin/staff/{staffId}/availability/{availabilityId}/toggle", staff.getId(), availabilityId)
                .header("X-User-Email", FRONT_DESK_EMAIL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/admin/staff/{staffId}/availability", staff.getId())
                .header("X-User-Email", FRONT_DESK_EMAIL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].staffId").value(staff.getId()));

        mockMvc.perform(delete("/api/admin/staff/{staffId}/availability/{availabilityId}", staff.getId(), availabilityId)
                .header("X-User-Email", FRONT_DESK_EMAIL))
            .andExpect(status().isNoContent());
    }

    @Test
    void adminRejectsOverlappingAvailabilitySlots() throws Exception {
        Staff staff = staffRepository.save(new Staff("Dr. Stone", "Veterinarian", true));

        mockMvc.perform(post("/api/admin/staff/{staffId}/availability", staff.getId())
                .header("X-User-Email", FRONT_DESK_EMAIL)
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
                .header("X-User-Email", FRONT_DESK_EMAIL)
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
                .header("X-User-Email", FRONT_DESK_EMAIL)
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
                .header("X-User-Email", FRONT_DESK_EMAIL)
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
                .header("X-User-Email", FRONT_DESK_EMAIL)
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

        mockMvc.perform(get("/api/admin/operations/staff/{staffId}/schedule-exceptions", staff.getId())
                .header("X-User-Email", FRONT_DESK_EMAIL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].date").value("2026-05-15"));

        mockMvc.perform(delete("/api/admin/operations/staff/{staffId}/schedule-exceptions/{exceptionId}", staff.getId(), exceptionId)
                .header("X-User-Email", FRONT_DESK_EMAIL))
            .andExpect(status().isNoContent());
    }

    @Test
    void availabilityEndpointUsesInactiveDateOverrideBeforeWeeklyAvailability() throws Exception {
        Staff staff = staffRepository.save(new Staff("Dr. Quinn", "Veterinarian", true));
        createFullWeekAvailability(staff);

        mockMvc.perform(post("/api/admin/operations/staff/{staffId}/schedule-exceptions", staff.getId())
                .header("X-User-Email", FRONT_DESK_EMAIL)
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
                .header("X-User-Email", FRONT_DESK_EMAIL)
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
                .header("X-User-Email", FRONT_DESK_EMAIL)
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
        mockMvc.perform(get("/api/admin/staff")
                .header("X-User-Email", ADMIN_EMAIL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").isNumber())
            .andExpect(jsonPath("$[0].name").isNotEmpty())
            .andExpect(jsonPath("$[0].role").isNotEmpty())
            .andExpect(jsonPath("$[0].active").isBoolean());
    }

    @Test
    void frontDeskCanLoadReadOnlyOperationsStaffList() throws Exception {
        mockMvc.perform(get("/api/admin/staff/operations-list")
                .header("X-User-Email", FRONT_DESK_EMAIL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").isNumber())
            .andExpect(jsonPath("$[0].name").isNotEmpty())
            .andExpect(jsonPath("$[0].role").isNotEmpty())
            .andExpect(jsonPath("$[0].active").isBoolean());
    }

    @Test
    void adminDashboardReturnsRicherOperationalStats() throws Exception {
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
                      "sex": "Male",
                      "color": "Golden and white",
                      "microchipNumber": "MC-1001",
                      "allergies": "None reported",
                      "status": "Healthy"
                    }
                    """))
            .andExpect(status().isCreated());

        ClinicService service = clinicServiceRepository.findByActiveTrueOrderByCategoryAscNameAsc().stream()
            .findFirst()
            .orElseThrow();
        Staff staff = staffRepository.findByActiveTrueOrderByNameAsc().stream()
            .findFirst()
            .orElseThrow();

        MvcResult bookingResult = mockMvc.perform(post("/api/bookings")
                .header("X-User-Email", "jamie@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "petName": "Milo",
                      "serviceId": %d,
                      "service": "%s",
                      "date": "May 10, 2026",
                      "time": "10:30 AM",
                      "status": "Confirmed",
                      "clinic": "PawCare Hub Clinic",
                      "staffId": %d,
                      "staff": "%s"
                    }
                    """.formatted(service.getId(), service.getName(), staff.getId(), staff.getName())))
            .andExpect(status().isCreated())
            .andReturn();

        long bookingId = objectMapper.readTree(bookingResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/admin/bookings/{id}/complete", bookingId)
                .header("X-User-Email", DOCTOR_EMAIL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "visitSummary": "Routine check completed successfully.",
                      "diagnosisAssessment": "Stable condition.",
                      "treatmentRecommendation": "Continue current care plan.",
                      "followUpNote": "Book another wellness exam in six months."
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/dashboard/stats")
                .header("X-User-Email", FRONT_DESK_EMAIL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalUsers").value(4))
            .andExpect(jsonPath("$.petRecords").value(3))
            .andExpect(jsonPath("$.activeStaff").isNumber())
            .andExpect(jsonPath("$.activeServices").isNumber())
            .andExpect(jsonPath("$.bookingsByStatus[?(@.status=='Completed')].count").value(org.hamcrest.Matchers.hasItem(1)))
            .andExpect(jsonPath("$.topServices").isArray())
            .andExpect(jsonPath("$.topServices[0].label").isNotEmpty())
            .andExpect(jsonPath("$.staffWorkload").isArray())
            .andExpect(jsonPath("$.staffWorkload[0].label").isNotEmpty())
            .andExpect(jsonPath("$.recentCompletedVisits[0].id").value(bookingId))
            .andExpect(jsonPath("$.upcomingBookings").isArray());
    }

    @Test
    void adminCanToggleStaffActiveState() throws Exception {
        Staff staff = staffRepository.findByActiveTrueOrderByNameAsc().stream()
            .findFirst()
            .orElseThrow();
        boolean initialActive = staff.isActive();

        mockMvc.perform(patch("/api/admin/staff/{id}/toggle", staff.getId())
                .header("X-User-Email", ADMIN_EMAIL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(staff.getId()))
            .andExpect(jsonPath("$.active").value(!initialActive));

        Staff updatedStaff = staffRepository.findById(staff.getId()).orElseThrow();
        assertThat(updatedStaff.isActive()).isEqualTo(!initialActive);
    }

    @Test
    void adminCanCreateStaffRecord() throws Exception {
        mockMvc.perform(post("/api/admin/staff")
                .header("X-User-Email", ADMIN_EMAIL)
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
                .header("X-User-Email", ADMIN_EMAIL)
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

        mockMvc.perform(patch("/api/admin/bookings/{id}/confirm", bookingId)
                .header("X-User-Email", FRONT_DESK_EMAIL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(bookingId))
            .andExpect(jsonPath("$.status").value("Confirmed"))
            .andExpect(jsonPath("$.service").value(service.getName()))
            .andExpect(jsonPath("$.ownerEmail").value("jamie@example.com"));

        Booking savedBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(savedBooking.getStatus()).isEqualTo("Confirmed");
    }

    @Test
    void adminCanCompleteBookingAndCreatePetMedicalNote() throws Exception {
        registerUser("jamie@example.com");
        ClinicService service = clinicServiceRepository.findByActiveTrueOrderByCategoryAscNameAsc().stream()
            .findFirst()
            .orElseThrow();

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
                      "sex": "Male",
                      "color": "Golden and white",
                      "microchipNumber": "MC-1001",
                      "allergies": "None reported",
                      "status": "Healthy"
                    }
                    """))
            .andExpect(status().isCreated());

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
                      "status": "Confirmed",
                      "clinic": "PawCare Hub Clinic",
                      "staff": "Dr. Rivera"
                    }
                    """.formatted(service.getId(), service.getName())))
            .andExpect(status().isCreated())
            .andReturn();

        long bookingId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/admin/bookings/{id}/complete", bookingId)
                .header("X-User-Email", DOCTOR_EMAIL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "visitSummary": "Wellness exam completed with no urgent concerns.",
                      "diagnosisAssessment": "Mild seasonal skin irritation.",
                      "treatmentRecommendation": "Use the prescribed medicated shampoo twice weekly.",
                      "followUpNote": "Recheck in 4 weeks if itching continues."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Completed"))
            .andExpect(jsonPath("$.visitSummary").value("Wellness exam completed with no urgent concerns."))
            .andExpect(jsonPath("$.diagnosisAssessment").value("Mild seasonal skin irritation."))
            .andExpect(jsonPath("$.treatmentRecommendation").value("Use the prescribed medicated shampoo twice weekly."))
            .andExpect(jsonPath("$.followUpNote").value("Recheck in 4 weeks if itching continues."));

        mockMvc.perform(get("/api/bookings/{id}", bookingId)
                .header("X-User-Email", "jamie@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Completed"))
            .andExpect(jsonPath("$.visitSummary").value("Wellness exam completed with no urgent concerns."))
            .andExpect(jsonPath("$.diagnosisAssessment").value("Mild seasonal skin irritation."));

        Booking completedBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(completedBooking.getStatus()).isEqualTo("Completed");
        assertThat(completedBooking.getFollowUpNote()).isEqualTo("Recheck in 4 weeks if itching continues.");

        PetMedicalNote savedNote = petMedicalNoteRepository.findFirstByRelatedBookingId(bookingId).orElse(null);
        assertThat(savedNote).isNotNull();
        assertThat(savedNote.getAuthor()).isEqualTo("Dr. Rivera");
        assertThat(savedNote.getNoteText()).contains("Visit summary: Wellness exam completed with no urgent concerns.");
        assertThat(savedNote.getNoteText()).contains("Assessment: Mild seasonal skin irritation.");
        long petId = petRepository.findByOwnerEmailOrderByIdAsc("jamie@example.com").stream()
            .filter(pet -> "Milo".equalsIgnoreCase(pet.getName()))
            .findFirst()
            .orElseThrow()
            .getId();

        mockMvc.perform(get("/api/pets/{id}", petId)
                .header("X-User-Email", "jamie@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.medicalNotes[?(@.relatedBookingId==%s)].relatedVisit.bookingId".formatted(bookingId))
                .value(org.hamcrest.Matchers.hasItem((int) bookingId)))
            .andExpect(jsonPath("$.medicalNotes[?(@.relatedBookingId==%s)].relatedVisit.service".formatted(bookingId))
                .value(org.hamcrest.Matchers.hasItem(service.getName())))
            .andExpect(jsonPath("$.medicalNotes[?(@.relatedBookingId==%s)].relatedVisit.staff".formatted(bookingId))
                .value(org.hamcrest.Matchers.hasItem(completedBooking.getStaff())))
            .andExpect(jsonPath("$.medicalNotes[?(@.relatedBookingId==%s)].relatedVisit.status".formatted(bookingId))
                .value(org.hamcrest.Matchers.hasItem("Completed")))
            .andExpect(jsonPath("$.medicalNotes[?(@.relatedBookingId==%s)].relatedVisit.visitSummary".formatted(bookingId))
                .value(org.hamcrest.Matchers.hasItem("Wellness exam completed with no urgent concerns.")))
            .andExpect(jsonPath("$.medicalNotes[?(@.relatedBookingId==%s)].relatedVisit.diagnosisAssessment".formatted(bookingId))
                .value(org.hamcrest.Matchers.hasItem("Mild seasonal skin irritation.")))
            .andExpect(jsonPath("$.medicalNotes[?(@.relatedBookingId==%s)].relatedVisit.treatmentRecommendation".formatted(bookingId))
                .value(org.hamcrest.Matchers.hasItem("Use the prescribed medicated shampoo twice weekly.")))
            .andExpect(jsonPath("$.medicalNotes[?(@.relatedBookingId==%s)].relatedVisit.followUpNote".formatted(bookingId))
                .value(org.hamcrest.Matchers.hasItem("Recheck in 4 weeks if itching continues.")));
    }

    @Test
    void doctorCannotPerformFrontDeskBookingConfirmation() throws Exception {
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
                      "date": "May 11, 2026",
                      "time": "11:30 AM",
                      "status": "Upcoming",
                      "clinic": "PawCare Hub Clinic",
                      "staff": "Dr. Rivera"
                    }
                    """.formatted(service.getId(), service.getName())))
            .andExpect(status().isCreated())
            .andReturn();

        long bookingId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/admin/bookings/{id}/confirm", bookingId)
                .header("X-User-Email", DOCTOR_EMAIL))
            .andExpect(status().isForbidden())
            .andExpect(status().reason("You do not have permission to access this action"));
    }

    @Test
    void frontDeskCannotAccessAdminOnlyStaffDirectory() throws Exception {
        mockMvc.perform(get("/api/admin/staff")
                .header("X-User-Email", FRONT_DESK_EMAIL))
            .andExpect(status().isForbidden())
            .andExpect(status().reason("You do not have permission to access this action"));
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

    private void createClinicUser(String email, String name, String role) {
        User user = new User(name, email, "Secret123!");
        user.setActive(true);
        user.setRole(role);
        userRepository.save(user);
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
