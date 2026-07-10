package com.pawcarehub.backend.service;

import com.pawcarehub.backend.entity.Booking;
import com.pawcarehub.backend.entity.ClinicService;
import com.pawcarehub.backend.entity.Pet;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Order(50)
public class DemoDataInitializationService implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoDataInitializationService.class);
    private static final String DEMO_PASSWORD = "PawCareDemo123!";
    private static final String CLINIC = "PawCare Hub Clinic";
    private static final String DEMO_CLIENT_EMAIL = "client@pawcarehub.demo";
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.US);

    private final boolean seedEnabled;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserRepository userRepository;
    private final ClinicServiceRepository clinicServiceRepository;
    private final StaffRepository staffRepository;
    private final StaffAvailabilityRepository staffAvailabilityRepository;
    private final PetRepository petRepository;
    private final PetMedicalNoteRepository petMedicalNoteRepository;
    private final BookingRepository bookingRepository;

    public DemoDataInitializationService(
        @Value("${demo.seed.enabled:false}") boolean seedEnabled,
        UserRepository userRepository,
        ClinicServiceRepository clinicServiceRepository,
        StaffRepository staffRepository,
        StaffAvailabilityRepository staffAvailabilityRepository,
        PetRepository petRepository,
        PetMedicalNoteRepository petMedicalNoteRepository,
        BookingRepository bookingRepository
    ) {
        this.seedEnabled = seedEnabled;
        this.userRepository = userRepository;
        this.clinicServiceRepository = clinicServiceRepository;
        this.staffRepository = staffRepository;
        this.staffAvailabilityRepository = staffAvailabilityRepository;
        this.petRepository = petRepository;
        this.petMedicalNoteRepository = petMedicalNoteRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }

        CleanupStats cleanupStats = cleanupKnownDevelopmentArtifacts();
        seedUsers();
        seedServices();
        Map<String, Staff> staffByName = seedStaff();
        seedStaffAvailability(staffByName);
        User client = userRepository.findByEmail(DEMO_CLIENT_EMAIL).orElseThrow();
        seedClientPets(client);
        seedClientBookings(client, staffByName);
        int backfilledBookings = backfillExactStaffReferences();

        LOGGER.info(
            "Demo seed complete. Removed {} junk users, deactivated {} services, backfilled {} booking staff references.",
            cleanupStats.removedUsers(),
            cleanupStats.deactivatedServices(),
            backfilledBookings
        );
    }

    private CleanupStats cleanupKnownDevelopmentArtifacts() {
        int removedUsers = 0;
        int deactivatedServices = 0;

        for (User user : userRepository.findAll()) {
            if (!isKnownJunkUser(user)) {
                continue;
            }
            bookingRepository.findByOwnerEmailOrderByIdAsc(user.getEmail()).forEach(bookingRepository::delete);
            for (Pet pet : petRepository.findByOwnerEmailOrderByIdAsc(user.getEmail())) {
                petMedicalNoteRepository.deleteByPetId(pet.getId());
                petRepository.delete(pet);
            }
            userRepository.delete(user);
            removedUsers++;
        }

        for (ClinicService service : clinicServiceRepository.findAll()) {
            String name = normalized(service.getName());
            String description = normalized(service.getDescription());
            if ("test".equals(name)
                || "test".equals(description)
                || "vaccination follow-up".equals(name)
                || "vaccine follow-up".equals(name)) {
                if (service.isActive()) {
                    service.setActive(false);
                    clinicServiceRepository.save(service);
                    deactivatedServices++;
                }
            }
        }

        return new CleanupStats(removedUsers, deactivatedServices);
    }

    private boolean isKnownJunkUser(User user) {
        String name = normalized(user.getName()).replace(" ", "");
        String email = normalized(user.getEmail());
        return name.matches("test\\d*")
            || name.matches("testhash\\d*")
            || email.matches("test\\d*@.*")
            || email.matches("testhash\\d*@.*");
    }

    private void seedUsers() {
        upsertUser("admin@pawcarehub.demo", "Demo Admin", UserRoles.ADMIN, true);
        upsertUser("doctor@pawcarehub.demo", "Dr. Demo User", UserRoles.DOCTOR, true);
        upsertUser("frontdesk@pawcarehub.demo", "Front Desk Demo", UserRoles.FRONT_DESK, true);
        upsertUser(DEMO_CLIENT_EMAIL, "Taylor Morgan", UserRoles.USER, true);
        upsertUser("inactive.client@pawcarehub.demo", "Inactive Demo Client", UserRoles.USER, false);
    }

    private void upsertUser(String email, String name, String role, boolean active) {
        User user = userRepository.findByEmail(email)
            .orElseGet(() -> new User(name, email, passwordEncoder.encode(DEMO_PASSWORD)));
        user.setName(name);
        user.setRole(role);
        user.setActive(active);
        if (!passwordMatches(user.getPassword(), DEMO_PASSWORD)) {
            user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        }
        userRepository.save(user);
    }

    private boolean passwordMatches(String encodedPassword, String rawPassword) {
        return encodedPassword != null
            && encodedPassword.startsWith("$2")
            && passwordEncoder.matches(rawPassword, encodedPassword);
    }

    private void seedServices() {
        upsertService(
            "Annual wellness exam",
            "Wellness",
            "Comprehensive nose-to-tail exam with preventive care guidance and time for owner questions.",
            "30 min",
            "$85",
            true
        );
        upsertService(
            "Core vaccine appointment",
            "Vaccines",
            "Age-appropriate core vaccines with a brief wellness check and vaccine schedule review.",
            "20 min",
            "From $35",
            true
        );
        upsertService(
            "Dental evaluation",
            "Dental",
            "Oral health assessment for tartar, gum condition, breath concerns, and dental care planning.",
            "25 min",
            "$65",
            true
        );
        upsertService(
            "Sick visit consultation",
            "Sick Visits",
            "Focused visit for concerns like itching, vomiting, coughing, low energy, or appetite changes.",
            "30 min",
            "From $95",
            true
        );
        upsertService(
            "Puppy and kitten wellness visit",
            "Wellness",
            "Early-life care visit covering growth, nutrition, vaccines, parasite prevention, and home care.",
            "30 min",
            "$78",
            true
        );
        upsertService(
            "Coat and skin consultation",
            "Dermatology",
            "Evaluation for itching, shedding, coat changes, hot spots, and allergy-related skin concerns.",
            "30 min",
            "$72",
            true
        );
    }

    private void upsertService(String name, String category, String description, String duration, String price, boolean active) {
        ClinicService service = clinicServiceRepository.findAll().stream()
            .filter(existing -> existing.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElseGet(() -> new ClinicService(name, category, description, duration, price, active));
        service.setName(name);
        service.setCategory(category);
        service.setDescription(description);
        service.setDuration(duration);
        service.setPrice(price);
        service.setActive(active);
        clinicServiceRepository.save(service);
    }

    private Map<String, Staff> seedStaff() {
        List<Staff> staff = List.of(
            upsertStaff(
                "Dr. Maya Hart",
                "Veterinarian",
                "Dr. Maya Hart",
                "Preventive Care Veterinarian",
                "Guides wellness visits, vaccine planning, and long-term care for dogs and cats."
            ),
            upsertStaff(
                "Dr. Leo Finch",
                "Veterinarian",
                "Dr. Leo Finch",
                "Internal Medicine Veterinarian",
                "Helps families navigate sick visits, diagnostics, and follow-up care plans."
            ),
            upsertStaff(
                "Jamie Brooks",
                "Associate Veterinarian",
                "Jamie Brooks, DVM",
                "Associate Veterinarian",
                "Supports routine care, recheck visits, and practical treatment planning."
            ),
            upsertStaff(
                "Anna Reed",
                "Veterinary Nurse",
                "Anna Reed",
                "Veterinary Nurse",
                "Keeps visits calm and organized with hands-on patient support."
            ),
            upsertStaff(
                "Noah Kim",
                "Front Desk Coordinator",
                "Noah Kim",
                "Front Desk Coordinator",
                "Coordinates appointment flow, check-ins, and clinic operations."
            )
        );

        deactivateLegacyDefaultStaff(staff.stream().map(Staff::getName).collect(Collectors.toSet()));

        return staff.stream().collect(Collectors.toMap(Staff::getName, Function.identity()));
    }

    private void deactivateLegacyDefaultStaff(Set<String> currentDemoStaffNames) {
        Set<String> legacyDefaultStaffNames = Set.of("Dr. Chen", "Dr. Rivera", "Nurse Patel");
        staffRepository.findAll().stream()
            .filter(staff -> legacyDefaultStaffNames.contains(staff.getName()))
            .filter(staff -> !currentDemoStaffNames.contains(staff.getName()))
            .forEach(staff -> {
                staff.setActive(false);
                staff.setShowOnHomepage(false);
                staffRepository.save(staff);
            });
    }

    private Staff upsertStaff(String name, String role, String displayName, String specialty, String bio) {
        Staff staff = staffRepository.findAll().stream()
            .filter(existing -> existing.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElseGet(() -> new Staff(name, role, true));
        staff.setName(name);
        staff.setRole(role);
        staff.setDisplayName(displayName);
        staff.setSpecialty(specialty);
        staff.setBio(bio);
        staff.setActive(true);
        staff.setShowOnHomepage(true);
        return staffRepository.save(staff);
    }

    private void seedStaffAvailability(Map<String, Staff> staffByName) {
        replaceAvailability(staffByName.get("Dr. Maya Hart"), List.of(
            slot(DayOfWeek.MONDAY, 9, 0, 17, 0),
            slot(DayOfWeek.WEDNESDAY, 9, 0, 17, 0),
            slot(DayOfWeek.FRIDAY, 9, 0, 16, 0)
        ));
        replaceAvailability(staffByName.get("Dr. Leo Finch"), List.of(
            slot(DayOfWeek.TUESDAY, 9, 30, 17, 30),
            slot(DayOfWeek.THURSDAY, 9, 30, 17, 30)
        ));
        replaceAvailability(staffByName.get("Jamie Brooks"), List.of(
            slot(DayOfWeek.MONDAY, 8, 30, 16, 30),
            slot(DayOfWeek.TUESDAY, 8, 30, 16, 30),
            slot(DayOfWeek.WEDNESDAY, 8, 30, 16, 30),
            slot(DayOfWeek.THURSDAY, 8, 30, 13, 0)
        ));
        replaceAvailability(staffByName.get("Anna Reed"), List.of(
            slot(DayOfWeek.MONDAY, 8, 0, 16, 0),
            slot(DayOfWeek.TUESDAY, 8, 0, 16, 0),
            slot(DayOfWeek.WEDNESDAY, 8, 0, 16, 0),
            slot(DayOfWeek.THURSDAY, 8, 0, 16, 0),
            slot(DayOfWeek.FRIDAY, 8, 0, 14, 0)
        ));
        replaceAvailability(staffByName.get("Noah Kim"), List.of(
            slot(DayOfWeek.MONDAY, 8, 0, 15, 0),
            slot(DayOfWeek.TUESDAY, 10, 0, 18, 0),
            slot(DayOfWeek.THURSDAY, 10, 0, 18, 0),
            slot(DayOfWeek.FRIDAY, 8, 0, 15, 0)
        ));
    }

    private void replaceAvailability(Staff staff, List<AvailabilitySlot> slots) {
        if (staff == null) {
            return;
        }
        List<StaffAvailability> existingAvailability =
            staffAvailabilityRepository.findByStaffIdOrderByDayOfWeekAscStartTimeAsc(staff.getId());
        Set<AvailabilitySlot> existingSlots = existingAvailability.stream()
            .map(availability -> new AvailabilitySlot(
                availability.getDayOfWeek(),
                availability.getStartTime(),
                availability.getEndTime()
            ))
            .collect(Collectors.toSet());
        Set<AvailabilitySlot> targetSlots = Set.copyOf(slots);

        if (existingSlots.equals(targetSlots)) {
            return;
        }

        staffAvailabilityRepository.deleteAll(existingAvailability);
        slots.forEach(slot -> staffAvailabilityRepository.save(new StaffAvailability(
            staff,
            slot.dayOfWeek(),
            slot.startTime(),
            slot.endTime(),
            true
        )));
    }

    private void seedClientPets(User client) {
        petRepository.findByOwnerEmailOrderByIdAsc(client.getEmail()).stream()
            .filter(pet -> "Taylor's Buddy".equalsIgnoreCase(pet.getName()) || isJunkText(pet.getName()))
            .forEach(pet -> {
                petMedicalNoteRepository.deleteByPetId(pet.getId());
                petRepository.delete(pet);
            });

        upsertPet(
            client,
                "Luna",
                "Dog",
                "Golden Retriever",
                "5 years",
                "62 lb",
                "Friendly, food-motivated, and due for a routine wellness review.",
                "Female",
                null,
                "Golden",
                null,
                "Mild seasonal allergies",
                null,
                null,
                "Core vaccines current",
                "Monitor spring itching and weight trend.",
                "Healthy"
        );
        upsertPet(
            client,
                "Mochi",
                "Cat",
                "Domestic Shorthair",
                "3 years",
                "10 lb",
                "Indoor cat with good appetite and normal activity at home.",
                "Male",
                null,
                "Gray tabby",
                null,
                null,
                null,
                null,
                "Rabies current",
                "Prefers quiet handling during exams.",
                "Healthy"
        );
        upsertPet(
            client,
                "Pumpkin",
                "Cat",
                "Orange Tabby",
                "7 years",
                "13 lb",
                "Senior wellness monitoring with dental tartar noted at last visit.",
                "Male",
                null,
                "Orange",
                null,
                null,
                "Early dental disease",
                null,
                "Vaccines reviewed",
                "Dental evaluation recommended.",
                "Needs follow-up"
        );
        upsertPet(
            client,
                "Buddy",
                "Dog",
                "Corgi",
                "2 years",
                "27 lb",
                "Active young dog with occasional skin irritation after park visits.",
                "Male",
                null,
                "Sable",
                null,
                "Possible grass sensitivity",
                null,
                null,
                "Core vaccines current",
                "Track skin flare timing and bathing response.",
                "Active care"
        );
    }

    private void upsertPet(
        User owner,
        String name,
        String species,
        String breed,
        String age,
        String weight,
        String note,
        String sex,
        LocalDate dateOfBirth,
        String color,
        String microchipNumber,
        String allergies,
        String chronicConditions,
        String medications,
        String vaccinationNotes,
        String generalMedicalNotes,
        String status
    ) {
        Pet pet = petRepository.findFirstByOwnerIdAndNameIgnoreCase(owner.getId(), name)
            .orElseGet(() -> new Pet(
                name,
                species,
                breed,
                age,
                weight,
                note,
                sex,
                dateOfBirth,
                color,
                microchipNumber,
                allergies,
                chronicConditions,
                medications,
                vaccinationNotes,
                generalMedicalNotes,
                status,
                owner
            ));

        pet.setName(name);
        pet.setSpecies(species);
        pet.setBreed(breed);
        pet.setAge(age);
        pet.setWeight(weight);
        pet.setNote(note);
        pet.setSex(sex);
        pet.setDateOfBirth(dateOfBirth);
        pet.setColor(color);
        pet.setMicrochipNumber(microchipNumber);
        pet.setAllergies(allergies);
        pet.setChronicConditions(chronicConditions);
        pet.setMedications(medications);
        pet.setVaccinationNotes(vaccinationNotes);
        pet.setGeneralMedicalNotes(generalMedicalNotes);
        pet.setStatus(status);
        petRepository.save(pet);
    }

    private void seedClientBookings(User client, Map<String, Staff> staffByName) {
        bookingRepository.findByOwnerEmailOrderByIdAsc(client.getEmail()).stream()
            .filter(this::isKnownStaleDemoBooking)
            .forEach(bookingRepository::delete);

        Staff maya = staffByName.get("Dr. Maya Hart");
        Staff leo = staffByName.get("Dr. Leo Finch");
        Staff jamie = staffByName.get("Jamie Brooks");
        Staff anna = staffByName.get("Anna Reed");

        Booking upcoming = upsertBooking(
            client,
            "Luna",
            "Annual wellness exam",
            nextDate(DayOfWeek.MONDAY, 2),
            "10:30 AM",
            "Upcoming",
            maya
        );
        upcoming.setOwnerNote("Luna has been scratching more after hikes.");
        bookingRepository.save(upcoming);

        Booking confirmed = upsertBooking(
            client,
            "Buddy",
            "Coat and skin consultation",
            nextDate(DayOfWeek.THURSDAY, 5),
            "10:30 AM",
            "Confirmed",
            leo
        );
        confirmed.setOwnerNote("Please check Buddy's paws and belly for irritation.");
        bookingRepository.save(confirmed);

        Booking completed = upsertBooking(
            client,
            "Mochi",
            "Core vaccine appointment",
            previousDate(DayOfWeek.WEDNESDAY, 3),
            "11:00 AM",
            "Completed",
            jamie
        );
        completed.setVisitSummary("Mochi was bright, alert, and handled the vaccine well.");
        completed.setDiagnosisAssessment("No abnormalities found on brief exam.");
        completed.setTreatmentRecommendation("Continue normal diet and monitor for mild vaccine soreness.");
        completed.setFollowUpNote("Next wellness reminder in 12 months.");
        bookingRepository.save(completed);

        Booking cancelled = upsertBooking(
            client,
            "Pumpkin",
            "Dental evaluation",
            previousDate(DayOfWeek.FRIDAY, 7),
            "9:30 AM",
            "Cancelled",
            anna
        );
        cancelled.setOwnerNote("Owner called to reschedule after a travel conflict.");
        bookingRepository.save(cancelled);
    }

    private boolean isKnownStaleDemoBooking(Booking booking) {
        return ("April 18, 2026".equals(booking.getDate()) || "April 24, 2026".equals(booking.getDate()))
            || "Vaccination follow-up".equalsIgnoreCase(booking.getService())
            || isJunkText(booking.getPetName());
    }

    private Booking upsertBooking(
        User owner,
        String petName,
        String serviceName,
        String date,
        String time,
        String status,
        Staff staff
    ) {
        ClinicService service = clinicServiceRepository.findAll().stream()
            .filter(existing -> existing.getName().equalsIgnoreCase(serviceName))
            .findFirst()
            .orElseThrow();

        Booking booking = bookingRepository.findByOwnerEmailOrderByIdAsc(owner.getEmail()).stream()
            .filter(existing -> existing.getPetName().equalsIgnoreCase(petName))
            .filter(existing -> existing.getStatus().equalsIgnoreCase(status))
            .findFirst()
            .orElseGet(() -> new Booking(
                petName,
                service.getName(),
                date,
                time,
                status,
                CLINIC,
                staff.getName(),
                service,
                staff,
                owner
            ));

        booking.setPetName(petName);
        booking.setServiceRecord(service);
        booking.setDate(date);
        booking.setTime(time);
        booking.setStatus(status);
        booking.setClinic(CLINIC);
        booking.setStaffRecord(staff);
        booking.setVisitSummary(null);
        booking.setDiagnosisAssessment(null);
        booking.setTreatmentRecommendation(null);
        booking.setFollowUpNote(null);
        booking.setOwnerNote(null);
        return booking;
    }

    private int backfillExactStaffReferences() {
        Map<String, List<Staff>> staffByName = staffRepository.findAll().stream()
            .collect(Collectors.groupingBy(staff -> normalized(staff.getName())));
        int updated = 0;

        for (Booking booking : bookingRepository.findAll()) {
            if (booking.getStaffRecord() != null) {
                continue;
            }
            List<Staff> matches = staffByName.get(normalized(booking.getStaff()));
            if (matches == null || matches.size() != 1) {
                continue;
            }
            booking.setStaffRecord(matches.get(0));
            bookingRepository.save(booking);
            updated++;
        }

        return updated;
    }

    private String nextDate(DayOfWeek dayOfWeek, int minimumDaysFromToday) {
        LocalDate date = LocalDate.now().plusDays(minimumDaysFromToday);
        while (date.getDayOfWeek() != dayOfWeek) {
            date = date.plusDays(1);
        }
        return date.format(DISPLAY_DATE_FORMATTER);
    }

    private String previousDate(DayOfWeek dayOfWeek, int minimumDaysBeforeToday) {
        LocalDate date = LocalDate.now().minusDays(minimumDaysBeforeToday);
        while (date.getDayOfWeek() != dayOfWeek) {
            date = date.minusDays(1);
        }
        return date.format(DISPLAY_DATE_FORMATTER);
    }

    private AvailabilitySlot slot(DayOfWeek dayOfWeek, int startHour, int startMinute, int endHour, int endMinute) {
        return new AvailabilitySlot(dayOfWeek, LocalTime.of(startHour, startMinute), LocalTime.of(endHour, endMinute));
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isJunkText(String value) {
        String normalized = normalized(value).replace(" ", "");
        return normalized.matches("test\\d*") || normalized.matches("testhash\\d*");
    }

    private record AvailabilitySlot(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
    }

    private record CleanupStats(int removedUsers, int deactivatedServices) {
    }
}
