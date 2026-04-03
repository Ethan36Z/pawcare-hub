package com.pawcarehub.backend.repository;

import com.pawcarehub.backend.entity.Pet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, Long> {

    List<Pet> findByOwnerEmailOrderByIdAsc(String ownerEmail);
}
