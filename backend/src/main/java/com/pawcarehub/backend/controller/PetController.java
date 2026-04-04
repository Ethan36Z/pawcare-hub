package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.pet.CreatePetRequest;
import com.pawcarehub.backend.dto.pet.PetResponse;
import com.pawcarehub.backend.dto.pet.UpdatePetRequest;
import com.pawcarehub.backend.service.PetService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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

    @GetMapping("/{id}")
    public PetResponse getCurrentUserPet(
        @RequestHeader("X-User-Email") String userEmailHeader,
        @PathVariable Long id
    ) {
        return petService.getCurrentUserPet(userEmailHeader, id);
    }

    @PostMapping
    public ResponseEntity<PetResponse> createPet(
        @RequestHeader("X-User-Email") String userEmailHeader,
        @RequestBody CreatePetRequest request
    ) {
        PetResponse response = petService.createPet(userEmailHeader, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PetResponse> updatePet(
        @RequestHeader("X-User-Email") String userEmailHeader,
        @PathVariable Long id,
        @RequestBody UpdatePetRequest request
    ) {
        PetResponse response = petService.updatePet(userEmailHeader, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePet(
        @RequestHeader("X-User-Email") String userEmailHeader,
        @PathVariable Long id
    ) {
        petService.deletePet(userEmailHeader, id);
        return ResponseEntity.noContent().build();
    }
}
