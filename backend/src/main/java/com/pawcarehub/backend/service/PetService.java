package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.auth.AuthenticatedUser;
import com.pawcarehub.backend.dto.pet.PetResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PetService {

    private final AuthService authService;

    public PetService(AuthService authService) {
        this.authService = authService;
    }

    public List<PetResponse> getCurrentUserPets(String userEmailHeader) {
        AuthenticatedUser user = authService.getAuthenticatedUser(userEmailHeader);
        String ownerName = user.name().trim();
        String ownerFirstName = ownerName.split(" ")[0];
        String ownerSlug = user.email().split("@")[0].toLowerCase();
        int seed = ownerSlug.chars().sum();

        return List.of(
            new PetResponse(
                ownerFirstName + "'s Buddy",
                "Dog",
                seed % 2 == 0 ? "Golden Retriever" : "Labrador Mix",
                "4 years",
                "58 lb",
                ownerName + " has an upcoming wellness visit scheduled and asked for guidance on nutrition and seasonal allergies.",
                "Upcoming visit"
            ),
            new PetResponse(
                buildSecondaryPetName(ownerSlug),
                "Cat",
                "Domestic Shorthair",
                "2 years",
                "10 lb",
                "This profile is tied to " + user.email() + " and reflects a healthy routine with current vaccines and no active care concerns.",
                "Healthy"
            ),
            new PetResponse(
                "Maple",
                "Dog",
                "Cavapoo",
                "8 months",
                "16 lb",
                ownerName + " is tracking a puppy vaccine plan and recent questions about teething, feeding, and energy levels.",
                "Vaccine plan"
            )
        );
    }

    private String buildSecondaryPetName(String ownerSlug) {
        String trimmedSlug = ownerSlug.length() > 1 ? ownerSlug.substring(1, Math.min(ownerSlug.length(), 5)) : "Milo";
        return Character.toUpperCase(ownerSlug.charAt(0)) + trimmedSlug;
    }
}
