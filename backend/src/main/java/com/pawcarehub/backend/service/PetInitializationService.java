package com.pawcarehub.backend.service;

import com.pawcarehub.backend.entity.Pet;
import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.repository.PetRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PetInitializationService {

    private final PetRepository petRepository;

    public PetInitializationService(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    public void createDefaultPetsForUser(User user) {
        String ownerName = user.getName().trim();
        String ownerFirstName = ownerName.split(" ")[0];

        List<Pet> defaultPets = List.of(
            new Pet(
                ownerFirstName + "'s Buddy",
                "Dog",
                "Golden Retriever",
                "4 years",
                "58 lb",
                ownerName + " has an upcoming wellness visit scheduled and asked for guidance on nutrition and seasonal allergies.",
                "Upcoming visit",
                user
            ),
            new Pet(
                "Luna",
                "Cat",
                "Domestic Shorthair",
                "2 years",
                "10 lb",
                "Healthy and current on vaccines, with no active care concerns at the last visit.",
                "Healthy",
                user
            )
        );

        petRepository.saveAll(defaultPets);
    }
}
