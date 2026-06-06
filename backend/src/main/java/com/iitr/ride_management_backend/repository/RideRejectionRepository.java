package com.iitr.ride_management_backend.repository;

import com.iitr.ride_management_backend.domain.RideRejection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RideRejectionRepository extends JpaRepository<RideRejection, Long> {
    boolean existsByRideIdAndDriverId(Long rideId, Long driverId);

    Optional<RideRejection> findTopByRideIdOrderByRejectedAtDesc(Long rideId);
}
