package com.pawcarehub.backend;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.repository.BookingRepository;
import com.pawcarehub.backend.repository.ClinicServiceRepository;
import com.pawcarehub.backend.repository.PetRepository;
import com.pawcarehub.backend.repository.StaffRepository;
import com.pawcarehub.backend.repository.UserRepository;
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
        Staff staff = staffRepository.findAllByOrderByActiveDescNameAsc().stream()
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
