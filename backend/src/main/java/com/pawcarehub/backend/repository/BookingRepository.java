package com.pawcarehub.backend.repository;

import com.pawcarehub.backend.entity.Booking;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByOwnerEmailOrderByIdAsc(String ownerEmail);

    boolean existsByOwnerEmailAndPetName(String ownerEmail, String petName);
}
