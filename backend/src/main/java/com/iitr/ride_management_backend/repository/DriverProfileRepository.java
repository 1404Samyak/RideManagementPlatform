package com.iitr.ride_management_backend.repository;

import com.iitr.ride_management_backend.domain.AvailabilityStatus;
import com.iitr.ride_management_backend.domain.DriverProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {
    Optional<DriverProfile> findByUserId(Long userId);

    List<DriverProfile> findByAvailabilityStatusOrderByUserNameAsc(AvailabilityStatus availabilityStatus);
}
