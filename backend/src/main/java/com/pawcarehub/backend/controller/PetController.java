package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.pet.CreatePetRequest;
import com.pawcarehub.backend.dto.pet.PetMedicalNoteRequest;
import com.pawcarehub.backend.dto.pet.PetMedicalNoteResponse;
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
    public List<PetResponse> getCurrentUserPets() {
        return petService.getCurrentUserPets();
    }

    @GetMapping("/{id}")
    public PetResponse getCurrentUserPet(@PathVariable Long id) {
        return petService.getCurrentUserPet(id);
    }

    @PostMapping
    public ResponseEntity<PetResponse> createPet(@RequestBody CreatePetRequest request) {
        PetResponse response = petService.createPet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PetResponse> updatePet(
        @PathVariable Long id,
        @RequestBody UpdatePetRequest request
    ) {
        PetResponse response = petService.updatePet(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/medical-notes")
    public ResponseEntity<PetMedicalNoteResponse> addMedicalNote(
        @PathVariable Long id,
        @RequestBody PetMedicalNoteRequest request
    ) {
        PetMedicalNoteResponse response = petService.addMedicalNote(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePet(@PathVariable Long id) {
        petService.deletePet(id);
        return ResponseEntity.noContent().build();
    }
}
