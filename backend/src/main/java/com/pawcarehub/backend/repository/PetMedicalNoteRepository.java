package com.pawcarehub.backend.repository;

import com.pawcarehub.backend.entity.PetMedicalNote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetMedicalNoteRepository extends JpaRepository<PetMedicalNote, Long> {

    List<PetMedicalNote> findByPetIdOrderByNoteDateDescIdDesc(Long petId);

    void deleteByPetId(Long petId);
}
