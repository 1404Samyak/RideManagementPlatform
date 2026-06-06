package com.iitr.ride_management_backend.repository;

import com.iitr.ride_management_backend.domain.PassengerProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassengerProfileRepository extends JpaRepository<PassengerProfile, Long> {
    Optional<PassengerProfile> findByUserId(Long userId);
}
