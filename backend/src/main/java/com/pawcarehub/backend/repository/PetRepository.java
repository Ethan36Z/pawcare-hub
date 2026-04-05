package com.pawcarehub.backend.repository;

import com.pawcarehub.backend.entity.Pet;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, Long> {

    List<Pet> findByOwnerEmailOrderByIdAsc(String ownerEmail);

    Optional<Pet> findByIdAndOwnerEmail(Long id, String ownerEmail);

    Optional<Pet> findFirstByOwnerIdAndNameIgnoreCase(Long ownerId, String name);
}
