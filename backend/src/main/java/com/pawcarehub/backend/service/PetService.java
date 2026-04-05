package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.auth.AuthenticatedUser;
import com.pawcarehub.backend.dto.pet.CreatePetRequest;
import com.pawcarehub.backend.dto.pet.PetMedicalNoteRequest;
import com.pawcarehub.backend.dto.pet.PetMedicalNoteResponse;
import com.pawcarehub.backend.dto.pet.PetResponse;
import com.pawcarehub.backend.dto.pet.UpdatePetRequest;
import com.pawcarehub.backend.entity.Booking;
import com.pawcarehub.backend.entity.Pet;
import com.pawcarehub.backend.entity.PetMedicalNote;
import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.repository.BookingRepository;
import com.pawcarehub.backend.repository.PetMedicalNoteRepository;
import com.pawcarehub.backend.repository.PetRepository;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PetService {

    private final AuthService authService;
    private final PetRepository petRepository;
    private final BookingRepository bookingRepository;
    private final PetMedicalNoteRepository petMedicalNoteRepository;
    private static final List<String> UPCOMING_BOOKING_STATUSES = Arrays.asList("Upcoming", "Confirmed");

    public PetService(
        AuthService authService,
        PetRepository petRepository,
        BookingRepository bookingRepository,
        PetMedicalNoteRepository petMedicalNoteRepository
    ) {
        this.authService = authService;
        this.petRepository = petRepository;
        this.bookingRepository = bookingRepository;
        this.petMedicalNoteRepository = petMedicalNoteRepository;
    }

    public List<PetResponse> getCurrentUserPets() {
        AuthenticatedUser user = authService.getAuthenticatedUser();
        return petRepository.findByOwnerEmailOrderByIdAsc(user.email()).stream()
            .map(pet -> toPetResponse(pet, user.email(), false))
            .toList();
    }

    public PetResponse getCurrentUserPet(Long petId) {
        AuthenticatedUser user = authService.getAuthenticatedUser();
        Pet pet = petRepository.findByIdAndOwnerEmail(petId, user.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));

        return toPetResponse(pet, user.email(), true);
    }

    public PetResponse createPet(CreatePetRequest request) {
        User owner = authService.getAuthenticatedUserEntity();

        Pet savedPet = petRepository.save(new Pet(
            normalizeRequiredField(request.name(), "name"),
            normalizeRequiredField(request.species(), "species"),
            normalizeRequiredField(request.breed(), "breed"),
            normalizeRequiredField(request.age(), "age"),
            normalizeRequiredField(request.weight(), "weight"),
            normalizeRequiredField(request.note(), "note"),
            normalizeOptionalField(request.sex()),
            parseOptionalDate(request.dateOfBirth(), "dateOfBirth"),
            normalizeOptionalField(request.color()),
            normalizeOptionalField(request.microchipNumber()),
            normalizeOptionalField(request.allergies()),
            normalizeOptionalField(request.chronicConditions()),
            normalizeOptionalField(request.medications()),
            normalizeOptionalField(request.vaccinationNotes()),
            normalizeOptionalField(request.generalMedicalNotes()),
            normalizeRequiredField(request.status(), "status"),
            owner
        ));

        return toPetResponse(savedPet, owner.getEmail(), true);
    }

    public PetResponse updatePet(Long petId, UpdatePetRequest request) {
        AuthenticatedUser user = authService.getAuthenticatedUser();
        Pet pet = petRepository.findByIdAndOwnerEmail(petId, user.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));

        pet.setName(normalizeRequiredField(request.name(), "name"));
        pet.setSpecies(normalizeRequiredField(request.species(), "species"));
        pet.setBreed(normalizeRequiredField(request.breed(), "breed"));
        pet.setAge(normalizeRequiredField(request.age(), "age"));
        pet.setWeight(normalizeRequiredField(request.weight(), "weight"));
        pet.setNote(normalizeRequiredField(request.note(), "note"));
        pet.setSex(normalizeOptionalField(request.sex()));
        pet.setDateOfBirth(parseOptionalDate(request.dateOfBirth(), "dateOfBirth"));
        pet.setColor(normalizeOptionalField(request.color()));
        pet.setMicrochipNumber(normalizeOptionalField(request.microchipNumber()));
        pet.setAllergies(normalizeOptionalField(request.allergies()));
        pet.setChronicConditions(normalizeOptionalField(request.chronicConditions()));
        pet.setMedications(normalizeOptionalField(request.medications()));
        pet.setVaccinationNotes(normalizeOptionalField(request.vaccinationNotes()));
        pet.setGeneralMedicalNotes(normalizeOptionalField(request.generalMedicalNotes()));
        pet.setStatus(normalizeRequiredField(request.status(), "status"));

        Pet savedPet = petRepository.save(pet);
        return toPetResponse(savedPet, user.email(), true);
    }

    public PetMedicalNoteResponse addMedicalNote(
        Long petId,
        PetMedicalNoteRequest request
    ) {
        AuthenticatedUser user = authService.getAuthenticatedUser();
        Pet pet = petRepository.findByIdAndOwnerEmail(petId, user.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));

        Long relatedBookingId = request.relatedBookingId();
        if (relatedBookingId != null) {
            Booking booking = bookingRepository.findByIdAndOwnerEmail(relatedBookingId, user.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Related booking not found"));

            if (!booking.getPetName().equalsIgnoreCase(pet.getName())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Related booking must belong to the same pet"
                );
            }
        }

        PetMedicalNote savedNote = petMedicalNoteRepository.save(new PetMedicalNote(
            pet,
            parseRequiredDate(request.date(), "date"),
            normalizeOptionalField(request.author()),
            relatedBookingId,
            normalizeRequiredField(request.noteText(), "noteText")
        ));

        return toMedicalNoteResponse(savedNote);
    }

    public void deletePet(Long petId) {
        AuthenticatedUser user = authService.getAuthenticatedUser();
        Pet pet = petRepository.findByIdAndOwnerEmail(petId, user.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));

        if (bookingRepository.existsByOwnerEmailAndPetName(user.email(), pet.getName())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "This pet cannot be deleted because it is referenced by existing bookings"
            );
        }

        petMedicalNoteRepository.deleteByPetId(petId);
        petRepository.delete(pet);
    }

    private PetResponse toPetResponse(Pet pet, String ownerEmail, boolean includeMedicalNotes) {
        return new PetResponse(
            pet.getId(),
            pet.getName(),
            pet.getSpecies(),
            pet.getBreed(),
            pet.getAge(),
            pet.getWeight(),
            pet.getNote(),
            pet.getSex(),
            pet.getDateOfBirth() != null ? pet.getDateOfBirth().toString() : null,
            pet.getColor(),
            pet.getMicrochipNumber(),
            pet.getAllergies(),
            pet.getChronicConditions(),
            pet.getMedications(),
            pet.getVaccinationNotes(),
            pet.getGeneralMedicalNotes(),
            pet.getStatus(),
            resolveDisplayStatus(ownerEmail, pet),
            includeMedicalNotes ? getMedicalNotes(pet.getId()) : List.of()
        );
    }

    private List<PetMedicalNoteResponse> getMedicalNotes(Long petId) {
        List<PetMedicalNote> notes = petMedicalNoteRepository.findByPetIdOrderByNoteDateDescIdDesc(petId);
        Map<Long, Booking> bookingsById = bookingRepository.findAllById(
            notes.stream()
                .map(PetMedicalNote::getRelatedBookingId)
                .filter(relatedBookingId -> relatedBookingId != null)
                .distinct()
                .toList()
        ).stream().collect(Collectors.toMap(Booking::getId, Function.identity()));

        return notes.stream()
            .map(note -> toMedicalNoteResponse(note, bookingsById.get(note.getRelatedBookingId())))
            .toList();
    }

    private PetMedicalNoteResponse toMedicalNoteResponse(PetMedicalNote note) {
        return toMedicalNoteResponse(note, null);
    }

    private PetMedicalNoteResponse toMedicalNoteResponse(PetMedicalNote note, Booking relatedBooking) {
        return new PetMedicalNoteResponse(
            note.getId(),
            note.getNoteDate().toString(),
            note.getAuthor(),
            note.getRelatedBookingId(),
            note.getNoteText(),
            note.getCreatedAt() != null ? note.getCreatedAt().toString() : null,
            note.getUpdatedAt() != null ? note.getUpdatedAt().toString() : null,
            toRelatedVisitSummary(relatedBooking)
        );
    }

    private PetMedicalNoteResponse.RelatedVisitSummary toRelatedVisitSummary(Booking booking) {
        if (booking == null) {
            return null;
        }

        return new PetMedicalNoteResponse.RelatedVisitSummary(
            booking.getId(),
            booking.getService(),
            booking.getStaff(),
            booking.getDate(),
            booking.getTime(),
            booking.getStatus(),
            booking.getVisitSummary(),
            booking.getDiagnosisAssessment(),
            booking.getTreatmentRecommendation(),
            booking.getFollowUpNote()
        );
    }

    private String resolveDisplayStatus(String ownerEmail, Pet pet) {
        boolean hasUpcomingBooking = bookingRepository.existsByOwnerEmailAndPetNameAndStatusIn(
            ownerEmail,
            pet.getName(),
            UPCOMING_BOOKING_STATUSES
        );

        if (hasUpcomingBooking) {
            return "Upcoming visit";
        }

        return pet.getStatus();
    }

    private String normalizeRequiredField(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }

        return value.trim();
    }

    private String normalizeOptionalField(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private LocalDate parseRequiredDate(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }

        return parseDate(value.trim(), fieldName);
    }

    private LocalDate parseOptionalDate(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return parseDate(value.trim(), fieldName);
    }

    private LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must use YYYY-MM-DD format");
        }
    }
}
