package com.iitr.ride_management_backend.repository;

import com.iitr.ride_management_backend.domain.CampusLocation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampusLocationRepository extends JpaRepository<CampusLocation, Long> {

    List<CampusLocation> findAllByOrderByNameAsc();

    Optional<CampusLocation> findByNameIgnoreCase(String name);
}
