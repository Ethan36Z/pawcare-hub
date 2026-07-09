package com.pawcarehub.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pawcarehub.backend.entity.Staff;
import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.repository.BookingRepository;
import com.pawcarehub.backend.repository.PetMedicalNoteRepository;
import com.pawcarehub.backend.repository.PetRepository;
import com.pawcarehub.backend.repository.StaffRepository;
import com.pawcarehub.backend.repository.StaffAvailabilityRepository;
import com.pawcarehub.backend.repository.StaffScheduleExceptionRepository;
import com.pawcarehub.backend.repository.UserRepository;
import com.pawcarehub.backend.service.AdminUserService;
import com.pawcarehub.backend.service.UserRoles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserRoleAccessIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@pawcarehub.com";
    private static final String USER_EMAIL = "jamie@example.com";
    private static final String PASSWORD = "Secret123!";

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
    private StaffRepository staffRepository;

    @Autowired
    private StaffAvailabilityRepository staffAvailabilityRepository;

    @Autowired
    private StaffScheduleExceptionRepository staffScheduleExceptionRepository;

    @Autowired
    private AdminUserService adminUserService;

    @BeforeEach
    void cleanData() {
        bookingRepository.deleteAll();
        petMedicalNoteRepository.deleteAll();
        petRepository.deleteAll();
        staffScheduleExceptionRepository.deleteAll();
        staffAvailabilityRepository.deleteAll();
        staffRepository.deleteAll();
        userRepository.deleteAll();
        createUser(ADMIN_EMAIL, "Admin Lead", UserRoles.ADMIN, true);
    }

    @Test
    void publicRegistrationAlwaysCreatesUserRole() throws Exception {
        register("admin.person@example.com", "Admin Person", """
            {
              "name": "Admin Person",
              "email": "admin.person@example.com",
              "password": "%s",
              "role": "admin"
            }
            """.formatted(PASSWORD));

        assertThat(userRepository.findByEmail("admin.person@example.com").orElseThrow().getRole())
            .isEqualTo(UserRoles.USER);

        register("doctor.person@example.com", "Doctor Person", """
            {
              "name": "Doctor Person",
              "email": "doctor.person@example.com",
              "password": "%s"
            }
            """.formatted(PASSWORD));

        assertThat(userRepository.findByEmail("doctor.person@example.com").orElseThrow().getRole())
            .isEqualTo(UserRoles.USER);
    }

    @Test
    void adminCanChangeRolesAcrossAllowedValuesAndBackToUser() throws Exception {
        registerUser(USER_EMAIL);
        User user = userRepository.findByEmail(USER_EMAIL).orElseThrow();

        changeRole(user.getId(), UserRoles.DOCTOR)
            .andExpect(jsonPath("$.role").value(UserRoles.DOCTOR));
        assertThat(userRepository.findById(user.getId()).orElseThrow().getRole()).isEqualTo(UserRoles.DOCTOR);

        changeRole(user.getId(), UserRoles.FRONT_DESK)
            .andExpect(jsonPath("$.role").value(UserRoles.FRONT_DESK));
        assertThat(userRepository.findById(user.getId()).orElseThrow().getRole()).isEqualTo(UserRoles.FRONT_DESK);

        changeRole(user.getId(), UserRoles.ADMIN)
            .andExpect(jsonPath("$.role").value(UserRoles.ADMIN));
        assertThat(userRepository.findById(user.getId()).orElseThrow().getRole()).isEqualTo(UserRoles.ADMIN);

        changeRole(user.getId(), UserRoles.USER)
            .andExpect(jsonPath("$.role").value(UserRoles.USER));
        assertThat(userRepository.findById(user.getId()).orElseThrow().getRole()).isEqualTo(UserRoles.USER);
    }

    @Test
    void invalidAndUnauthorizedRoleChangesAreRejected() throws Exception {
        registerUser(USER_EMAIL);
        User user = userRepository.findByEmail(USER_EMAIL).orElseThrow();

        mockMvc.perform(patch("/api/admin/users/{id}/role", user.getId())
                .header("X-User-Email", ADMIN_EMAIL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "role": "manager" }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Invalid role. Allowed roles are user, doctor, front_desk, admin"));

        mockMvc.perform(patch("/api/admin/users/{id}/role", user.getId())
                .header("X-User-Email", USER_EMAIL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "role": "admin" }
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotChangeOwnRoleOrDemoteFinalActiveAdmin() throws Exception {
        User admin = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();

        mockMvc.perform(patch("/api/admin/users/{id}/role", admin.getId())
                .header("X-User-Email", ADMIN_EMAIL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "role": "user" }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Admins cannot change their own role"));

        assertThatThrownBy(() -> adminUserService.updateUserRole(admin.getId(), UserRoles.USER, null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Cannot demote the final active admin account");
    }

    @Test
    void changingRoleDoesNotReactivateInactiveUserOrTouchStaffRecords() throws Exception {
        registerUser(USER_EMAIL);
        User user = userRepository.findByEmail(USER_EMAIL).orElseThrow();
        user.setActive(false);
        userRepository.save(user);
        Staff staff = staffRepository.save(new Staff("Dr. Avery", "Veterinarian", true));

        changeRole(user.getId(), UserRoles.DOCTOR)
            .andExpect(jsonPath("$.role").value(UserRoles.DOCTOR))
            .andExpect(jsonPath("$.active").value(false));

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.isActive()).isFalse();
        assertThat(staffRepository.findAll()).hasSize(1);
        assertThat(staffRepository.findById(staff.getId()).orElseThrow().getRole()).isEqualTo("Veterinarian");
    }

    @Test
    void roleMatrixIsEnforcedByBackendApis() throws Exception {
        createUser("doctor@example.com", "Dr. Rivera", UserRoles.DOCTOR, true);
        createUser("front.desk@example.com", "Front Desk", UserRoles.FRONT_DESK, true);
        registerUser(USER_EMAIL);

        mockMvc.perform(get("/api/admin/dashboard/stats").header("X-User-Email", USER_EMAIL))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/dashboard/stats").header("X-User-Email", "doctor@example.com"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/bookings").header("X-User-Email", "doctor@example.com"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/services").header("X-User-Email", "doctor@example.com"))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users").header("X-User-Email", "doctor@example.com"))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/staff/operations-list").header("X-User-Email", "doctor@example.com"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/dashboard/stats").header("X-User-Email", "front.desk@example.com"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/bookings").header("X-User-Email", "front.desk@example.com"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/staff/operations-list").header("X-User-Email", "front.desk@example.com"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/users").header("X-User-Email", "front.desk@example.com"))
            .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/admin/bookings/{id}/complete", 1L)
                .header("X-User-Email", "front.desk@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "outcome": "Completed" }
                    """))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users").header("X-User-Email", ADMIN_EMAIL))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/services").header("X-User-Email", ADMIN_EMAIL))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/staff").header("X-User-Email", ADMIN_EMAIL))
            .andExpect(status().isOk());
    }

    @Test
    void oldJwtUsesCurrentDatabaseRole() throws Exception {
        registerUser(USER_EMAIL);
        String token = loginAndGetToken(USER_EMAIL);
        User user = userRepository.findByEmail(USER_EMAIL).orElseThrow();

        mockMvc.perform(get("/api/admin/dashboard/stats").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());

        changeRole(user.getId(), UserRoles.DOCTOR);

        mockMvc.perform(get("/api/admin/dashboard/stats").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void changingEmailDoesNotChangePermissions() throws Exception {
        createUser("casey@example.com", "Casey", UserRoles.DOCTOR, true);
        String token = loginAndGetToken("casey@example.com");

        mockMvc.perform(patch("/api/auth/change-email")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "currentPassword": "%s",
                      "newEmail": "admin.doctor.manager@example.com"
                    }
                    """.formatted(PASSWORD)))
            .andExpect(status().isOk());

        assertThat(userRepository.findByEmail("admin.doctor.manager@example.com").orElseThrow().getRole())
            .isEqualTo(UserRoles.DOCTOR);
    }

    private void register(String email, String name, String payload) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.name").value(name))
            .andExpect(jsonPath("$.role").value(UserRoles.USER));
    }

    private void registerUser(String email) throws Exception {
        register(email, "Jamie Parker", """
            {
              "name": "Jamie Parker",
              "email": "%s",
              "password": "%s"
            }
            """.formatted(email, PASSWORD));
    }

    private org.springframework.test.web.servlet.ResultActions changeRole(Long userId, String role) throws Exception {
        return mockMvc.perform(patch("/api/admin/users/{id}/role", userId)
                .header("X-User-Email", ADMIN_EMAIL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "role": "%s" }
                    """.formatted(role)))
            .andExpect(status().isOk());
    }

    private String loginAndGetToken(String email) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "%s"
                    }
                    """.formatted(email, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
    }

    private void createUser(String email, String name, String role, boolean active) {
        User user = new User(name, email, PASSWORD);
        user.setRole(role);
        user.setActive(active);
        userRepository.save(user);
    }
}
