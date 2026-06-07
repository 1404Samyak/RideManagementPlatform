package com.iitr.ride_management_backend.controller;

import com.iitr.ride_management_backend.dto.CampusLocationResponse;
import com.iitr.ride_management_backend.service.LocationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public List<CampusLocationResponse> campusLocations() {
        return locationService.campusLocations();
    }
}
