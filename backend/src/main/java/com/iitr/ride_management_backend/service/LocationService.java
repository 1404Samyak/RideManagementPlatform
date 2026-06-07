package com.iitr.ride_management_backend.service;

import com.iitr.ride_management_backend.dto.CampusLocationResponse;
import com.iitr.ride_management_backend.repository.CampusLocationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationService {

    private final CampusLocationRepository campusLocationRepository;
    private final ResponseMapper mapper;

    public LocationService(CampusLocationRepository campusLocationRepository, ResponseMapper mapper) {
        this.campusLocationRepository = campusLocationRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CampusLocationResponse> campusLocations() {
        return campusLocationRepository.findAllByOrderByNameAsc()
                .stream()
                .map(mapper::campusLocationResponse)
                .toList();
    }
}
