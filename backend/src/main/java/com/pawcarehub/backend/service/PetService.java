package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.auth.AuthenticatedUser;
import com.pawcarehub.backend.dto.pet.PetResponse;
import com.pawcarehub.backend.entity.Pet;
import com.pawcarehub.backend.repository.PetRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PetService {

    private final AuthService authService;
    private final PetRepository petRepository;

    public PetService(AuthService authService, PetRepository petRepository) {
        this.authService = authService;
        this.petRepository = petRepository;
    }

    public List<PetResponse> getCurrentUserPets(String userEmailHeader) {
        AuthenticatedUser user = authService.getAuthenticatedUser(userEmailHeader);
        return petRepository.findByOwnerEmailOrderByIdAsc(user.email()).stream()
            .map(this::toPetResponse)
            .toList();
    }

    private PetResponse toPetResponse(Pet pet) {
        return new PetResponse(
            pet.getName(),
            pet.getSpecies(),
            pet.getBreed(),
            pet.getAge(),
            pet.getWeight(),
            pet.getNote(),
            pet.getStatus()
        );
    }
}
