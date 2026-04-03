package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.pet.PetResponse;
import com.pawcarehub.backend.service.PetService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping("/me")
    public List<PetResponse> getCurrentUserPets(
        @RequestHeader("X-User-Email") String userEmailHeader
    ) {
        return petService.getCurrentUserPets(userEmailHeader);
    }
}
