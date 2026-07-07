package com.pawcarehub.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.repository.BookingRepository;
import com.pawcarehub.backend.repository.PetMedicalNoteRepository;
import com.pawcarehub.backend.repository.PetRepository;
import com.pawcarehub.backend.repository.UserRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserAccountStatusIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@pawcarehub.com";
    private static final String CUSTOMER_EMAIL = "jamie@example.com";
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

    @BeforeEach
    void cleanUserData() {
        bookingRepository.deleteAll();
        petMedicalNoteRepository.deleteAll();
        petRepository.deleteAll();
        userRepository.deleteAll();
        createClinicUser(ADMIN_EMAIL, "Admin Lead", UserRoles.ADMIN);
    }

    @Test
    void adminCanDeactivateNormalUserAndPreserveRelatedRecords() throws Exception {
        registerUser(CUSTOMER_EMAIL);
        User customer = userRepository.findByEmail(CUSTOMER_EMAIL).orElseThrow();
        int petCount = petRepository.findByOwnerEmailOrderByIdAsc(CUSTOMER_EMAIL).size();
        int bookingCount = bookingRepository.findByOwnerEmailOrderByIdAsc(CUSTOMER_EMAIL).size();

        mockMvc.perform(patch("/api/admin/users/{id}/deactivate", customer.getId())
                .header("X-User-Email", ADMIN_EMAIL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(customer.getId()))
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.petCount").value(petCount))
            .andExpect(jsonPath("$.bookingCount").value(bookingCount));

        User deactivatedUser = userRepository.findById(customer.getId()).orElseThrow();
        assertThat(deactivatedUser.isActive()).isFalse();
        assertThat(petRepository.findByOwnerEmailOrderByIdAsc(CUSTOMER_EMAIL)).hasSize(petCount);
        assertThat(bookingRepository.findByOwnerEmailOrderByIdAsc(CUSTOMER_EMAIL)).hasSize(bookingCount);
    }

    @Test
    void deactivatedUserCannotLogIn() throws Exception {
        registerUser(CUSTOMER_EMAIL);
        User customer = userRepository.findByEmail(CUSTOMER_EMAIL).orElseThrow();
        deactivateUser(customer.getId());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(CUSTOMER_EMAIL, PASSWORD)))
            .andExpect(status().isUnauthorized())
            .andExpect(status().reason("This account has been deactivated"));
    }

    @Test
    void oldJwtCannotAccessProtectedEndpointAfterDeactivation() throws Exception {
        registerUser(CUSTOMER_EMAIL);
        String token = loginAndGetToken(CUSTOMER_EMAIL, PASSWORD);
        User customer = userRepository.findByEmail(CUSTOMER_EMAIL).orElseThrow();

        deactivateUser(customer.getId());

        mockMvc.perform(get("/api/auth/profile")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized())
            .andExpect(status().reason("User session is not recognized"));
    }

    @Test
    void adminCanReactivateUserAndUserCanLogInAgain() throws Exception {
        registerUser(CUSTOMER_EMAIL);
        User customer = userRepository.findByEmail(CUSTOMER_EMAIL).orElseThrow();
        deactivateUser(customer.getId());

        mockMvc.perform(patch("/api/admin/users/{id}/reactivate", customer.getId())
                .header("X-User-Email", ADMIN_EMAIL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(customer.getId()))
            .andExpect(jsonPath("$.active").value(true));

        assertThat(userRepository.findById(customer.getId()).orElseThrow().isActive()).isTrue();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(CUSTOMER_EMAIL, PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void nonAdminCannotDeactivateUsers() throws Exception {
        registerUser(CUSTOMER_EMAIL);
        registerUser("other@example.com");
        User otherUser = userRepository.findByEmail("other@example.com").orElseThrow();

        mockMvc.perform(patch("/api/admin/users/{id}/deactivate", otherUser.getId())
                .header("X-User-Email", CUSTOMER_EMAIL))
            .andExpect(status().isForbidden())
            .andExpect(status().reason("You do not have permission to access this action"));
    }

    @Test
    void adminCannotDeactivateOwnAuthenticatedAccount() throws Exception {
        User admin = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();

        mockMvc.perform(patch("/api/admin/users/{id}/deactivate", admin.getId())
                .header("X-User-Email", ADMIN_EMAIL))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Admins cannot deactivate their own account"));

        assertThat(userRepository.findByEmail(ADMIN_EMAIL).orElseThrow().isActive()).isTrue();
    }

    @Test
    void unknownUserReturnsNotFound() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}/deactivate", 999_999L)
                .header("X-User-Email", ADMIN_EMAIL))
            .andExpect(status().isNotFound())
            .andExpect(status().reason("User not found"));
    }

    @Test
    void repeatedDeactivateAndReactivateRequestsAreSafe() throws Exception {
        registerUser(CUSTOMER_EMAIL);
        User customer = userRepository.findByEmail(CUSTOMER_EMAIL).orElseThrow();

        deactivateUser(customer.getId());
        mockMvc.perform(patch("/api/admin/users/{id}/deactivate", customer.getId())
                .header("X-User-Email", ADMIN_EMAIL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        reactivateUser(customer.getId());
        mockMvc.perform(patch("/api/admin/users/{id}/reactivate", customer.getId())
                .header("X-User-Email", ADMIN_EMAIL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true));
    }

    private void createClinicUser(String email, String name, String role) {
        User user = new User(name, email, PASSWORD);
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
                      "password": "%s"
                    }
                    """.formatted(email, PASSWORD)))
            .andExpect(status().isCreated());
    }

    private void deactivateUser(Long userId) throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}/deactivate", userId)
                .header("X-User-Email", ADMIN_EMAIL))
            .andExpect(status().isOk());
    }

    private void reactivateUser(Long userId) throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}/reactivate", userId)
                .header("X-User-Email", ADMIN_EMAIL))
            .andExpect(status().isOk());
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, password)))
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
    }

    private String loginJson(String email, String password) {
        return """
            {
              "email": "%s",
              "password": "%s"
            }
            """.formatted(email, password);
    }
}
