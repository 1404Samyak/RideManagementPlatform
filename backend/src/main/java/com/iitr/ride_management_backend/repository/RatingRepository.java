package com.iitr.ride_management_backend.repository;

import com.iitr.ride_management_backend.domain.Rating;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByRideId(Long rideId);

    boolean existsByRideId(Long rideId);

    List<Rating> findByDriverIdOrderByCreatedAtDesc(Long driverId);
}
