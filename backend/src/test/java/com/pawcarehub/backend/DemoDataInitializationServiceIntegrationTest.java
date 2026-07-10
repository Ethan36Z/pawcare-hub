package com.pawcarehub.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.pawcarehub.backend.entity.Booking;
import com.pawcarehub.backend.entity.ClinicService;
import com.pawcarehub.backend.entity.Staff;
import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.repository.BookingRepository;
import com.pawcarehub.backend.repository.ClinicServiceRepository;
import com.pawcarehub.backend.repository.PetRepository;
import com.pawcarehub.backend.repository.StaffAvailabilityRepository;
import com.pawcarehub.backend.repository.StaffRepository;
import com.pawcarehub.backend.repository.UserRepository;
import com.pawcarehub.backend.service.DemoDataInitializationService;
import com.pawcarehub.backend.service.UserRoles;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "demo.seed.enabled=true")
@ActiveProfiles("test")
class DemoDataInitializationServiceIntegrationTest {

    @Autowired
    private DemoDataInitializationService demoDataInitializationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClinicServiceRepository clinicServiceRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private StaffAvailabilityRepository staffAvailabilityRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void demoSeedCreatesCleanIdempotentDataset() throws Exception {
        assertDemoDataset();
        DatasetCounts before = counts();

        demoDataInitializationService.run(null);

        assertDemoDataset();
        assertThat(counts()).isEqualTo(before);
    }

    private void assertDemoDataset() {
        assertDemoUser("admin@pawcarehub.demo", UserRoles.ADMIN, true);
        assertDemoUser("doctor@pawcarehub.demo", UserRoles.DOCTOR, true);
        assertDemoUser("frontdesk@pawcarehub.demo", UserRoles.FRONT_DESK, true);
        assertDemoUser("client@pawcarehub.demo", UserRoles.USER, true);
        assertDemoUser("inactive.client@pawcarehub.demo", UserRoles.USER, false);

        assertThat(clinicServiceRepository.findByActiveTrueOrderByCategoryAscNameAsc())
            .extracting(ClinicService::getName)
            .contains(
                "Annual wellness exam",
                "Core vaccine appointment",
                "Dental evaluation",
                "Sick visit consultation",
                "Puppy and kitten wellness visit",
                "Coat and skin consultation"
            )
            .doesNotContain("test", "Vaccination follow-up");

        assertThat(staffRepository.findByActiveTrueOrderByNameAsc())
            .extracting(Staff::getName)
            .containsExactly("Anna Reed", "Dr. Leo Finch", "Dr. Maya Hart", "Jamie Brooks", "Noah Kim");
        assertThat(staffRepository.countByActiveTrue()).isEqualTo(5);
        assertThat(staffRepository.findAll().stream()
            .filter(staff -> List.of("Dr. Chen", "Dr. Rivera", "Nurse Patel").contains(staff.getName())))
            .allSatisfy(staff -> {
                assertThat(staff.isActive()).isFalse();
                assertThat(staff.isShowOnHomepage()).isFalse();
            });

        Staff maya = staffRepository.findFirstByNameIgnoreCaseAndActiveTrue("Dr. Maya Hart").orElseThrow();
        assertThat(staffAvailabilityRepository.findByStaffIdOrderByDayOfWeekAscStartTimeAsc(maya.getId()))
            .extracting(availability -> availability.getDayOfWeek())
            .containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);

        assertThat(petRepository.findByOwnerEmailOrderByIdAsc("client@pawcarehub.demo"))
            .extracting(pet -> pet.getName())
            .containsExactlyInAnyOrder("Luna", "Mochi", "Pumpkin", "Buddy");

        assertThat(bookingRepository.findByOwnerEmailOrderByIdAsc("client@pawcarehub.demo"))
            .hasSize(4)
            .allSatisfy(booking -> {
                assertThat(booking.getStaffRecord()).isNotNull();
                assertThat(booking.getServiceRecord()).isNotNull();
                assertThat(booking.getDate()).doesNotContain("April 18, 2026", "April 24, 2026");
                assertThat(staffRepository.findById(booking.getStaffRecord().getId()).orElseThrow().getName())
                    .isIn("Dr. Maya Hart", "Dr. Leo Finch", "Jamie Brooks", "Anna Reed", "Noah Kim");
            })
            .extracting(Booking::getStatus)
            .containsExactlyInAnyOrder("Upcoming", "Confirmed", "Completed", "Cancelled");
    }

    private void assertDemoUser(String email, String role, boolean active) {
        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getRole()).isEqualTo(role);
        assertThat(user.isActive()).isEqualTo(active);
        assertThat(user.getPassword()).startsWith("$2");
    }

    private DatasetCounts counts() {
        return new DatasetCounts(
            userRepository.findAll().size(),
            clinicServiceRepository.findAll().size(),
            staffRepository.findAll().size(),
            staffAvailabilityRepository.findAll().size(),
            petRepository.findAll().size(),
            bookingRepository.findAll().size(),
            bookingRepository.findByOwnerEmailOrderByIdAsc("client@pawcarehub.demo").stream()
                .map(Booking::getStatus)
                .collect(java.util.stream.Collectors.toSet())
        );
    }

    private record DatasetCounts(
        int users,
        int services,
        int staff,
        int availability,
        int pets,
        int bookings,
        Set<String> demoBookingStatuses
    ) {
    }
}
