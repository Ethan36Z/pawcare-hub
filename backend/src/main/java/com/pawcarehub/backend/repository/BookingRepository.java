package com.pawcarehub.backend.repository;

import com.pawcarehub.backend.entity.Booking;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByOrderByIdDesc();

    long countByStatusIgnoreCase(String status);

    List<Booking> findByOwnerEmailOrderByIdAsc(String ownerEmail);

    boolean existsByOwnerEmailAndPetName(String ownerEmail, String petName);

    Optional<Booking> findByIdAndOwnerEmail(Long id, String ownerEmail);
}
