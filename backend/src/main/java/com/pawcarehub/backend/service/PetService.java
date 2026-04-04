package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.auth.AuthenticatedUser;
import com.pawcarehub.backend.dto.pet.CreatePetRequest;
import com.pawcarehub.backend.dto.pet.PetResponse;
import com.pawcarehub.backend.dto.pet.UpdatePetRequest;
import com.pawcarehub.backend.entity.Pet;
import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.repository.BookingRepository;
import com.pawcarehub.backend.repository.PetRepository;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PetService {

    private final AuthService authService;
    private final PetRepository petRepository;
    private final BookingRepository bookingRepository;
    private static final List<String> UPCOMING_BOOKING_STATUSES = Arrays.asList("Upcoming", "Confirmed");

    public PetService(
        AuthService authService,
        PetRepository petRepository,
        BookingRepository bookingRepository
    ) {
        this.authService = authService;
        this.petRepository = petRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<PetResponse> getCurrentUserPets(String userEmailHeader) {
        AuthenticatedUser user = authService.getAuthenticatedUser(userEmailHeader);
        return petRepository.findByOwnerEmailOrderByIdAsc(user.email()).stream()
            .map(pet -> toPetResponse(pet, user.email()))
            .toList();
    }

    public PetResponse getCurrentUserPet(String userEmailHeader, Long petId) {
        AuthenticatedUser user = authService.getAuthenticatedUser(userEmailHeader);
        Pet pet = petRepository.findByIdAndOwnerEmail(petId, user.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));

        return toPetResponse(pet, user.email());
    }

    public PetResponse createPet(String userEmailHeader, CreatePetRequest request) {
        User owner = authService.getAuthenticatedUserEntity(userEmailHeader);

        Pet savedPet = petRepository.save(new Pet(
            normalizeRequiredField(request.name(), "name"),
            normalizeRequiredField(request.species(), "species"),
            normalizeRequiredField(request.breed(), "breed"),
            normalizeRequiredField(request.age(), "age"),
            normalizeRequiredField(request.weight(), "weight"),
            normalizeRequiredField(request.note(), "note"),
            normalizeRequiredField(request.status(), "status"),
            owner
        ));

        return toPetResponse(savedPet, owner.getEmail());
    }

    public PetResponse updatePet(String userEmailHeader, Long petId, UpdatePetRequest request) {
        AuthenticatedUser user = authService.getAuthenticatedUser(userEmailHeader);
        Pet pet = petRepository.findByIdAndOwnerEmail(petId, user.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));

        pet.setName(normalizeRequiredField(request.name(), "name"));
        pet.setSpecies(normalizeRequiredField(request.species(), "species"));
        pet.setBreed(normalizeRequiredField(request.breed(), "breed"));
        pet.setAge(normalizeRequiredField(request.age(), "age"));
        pet.setWeight(normalizeRequiredField(request.weight(), "weight"));
        pet.setNote(normalizeRequiredField(request.note(), "note"));
        pet.setStatus(normalizeRequiredField(request.status(), "status"));

        Pet savedPet = petRepository.save(pet);
        return toPetResponse(savedPet, user.email());
    }

    public void deletePet(String userEmailHeader, Long petId) {
        AuthenticatedUser user = authService.getAuthenticatedUser(userEmailHeader);
        Pet pet = petRepository.findByIdAndOwnerEmail(petId, user.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found"));

        if (bookingRepository.existsByOwnerEmailAndPetName(user.email(), pet.getName())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "This pet cannot be deleted because it is referenced by existing bookings"
            );
        }

        petRepository.delete(pet);
    }

    private PetResponse toPetResponse(Pet pet, String ownerEmail) {
        return new PetResponse(
            pet.getId(),
            pet.getName(),
            pet.getSpecies(),
            pet.getBreed(),
            pet.getAge(),
            pet.getWeight(),
            pet.getNote(),
            pet.getStatus(),
            resolveDisplayStatus(ownerEmail, pet)
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
}
