package com.iitr.ride_management_backend.controller;

import com.iitr.ride_management_backend.domain.User;
import com.iitr.ride_management_backend.dto.DriverDashboardResponse;
import com.iitr.ride_management_backend.dto.DriverLocationRequest;
import com.iitr.ride_management_backend.dto.DriverLocationResponse;
import com.iitr.ride_management_backend.dto.DriverSummaryResponse;
import com.iitr.ride_management_backend.dto.RideResponse;
import com.iitr.ride_management_backend.service.CurrentUserService;
import com.iitr.ride_management_backend.service.DriverService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final CurrentUserService currentUserService;
    private final DriverService driverService;

    public DriverController(CurrentUserService currentUserService, DriverService driverService) {
        this.currentUserService = currentUserService;
        this.driverService = driverService;
    }

    @PostMapping("/availability/online")
    public DriverSummaryResponse goOnline() {
        return driverService.goOnline(currentUserService.currentUser());
    }

    @PostMapping("/availability/offline")
    public DriverSummaryResponse goOffline() {
        return driverService.goOffline(currentUserService.currentUser());
    }

    @GetMapping("/available")
    public List<DriverSummaryResponse> availableDrivers() {
        return driverService.availableDrivers();
    }

    @PostMapping("/location")
    public DriverLocationResponse updateLocation(@Valid @RequestBody DriverLocationRequest request) {
        User user = currentUserService.currentUser();
        return driverService.updateLocation(user, request);
    }

    @GetMapping("/requests")
    public List<RideResponse> incomingRequests() {
        User user = currentUserService.currentUser();
        return driverService.incomingRequests(user);
    }

    @GetMapping("/dashboard")
    public DriverDashboardResponse dashboard() {
        User user = currentUserService.currentUser();
        return driverService.dashboard(user);
    }

    @GetMapping("/rides/history")
    public List<RideResponse> rideHistory() {
        User user = currentUserService.currentUser();
        return driverService.rideHistory(user);
    }
}
