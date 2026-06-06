package com.iitr.ride_management_backend.controller;

import com.iitr.ride_management_backend.domain.User;
import com.iitr.ride_management_backend.dto.CreateRideRequest;
import com.iitr.ride_management_backend.dto.RideResponse;
import com.iitr.ride_management_backend.service.CurrentUserService;
import com.iitr.ride_management_backend.service.RideService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final CurrentUserService currentUserService;
    private final RideService rideService;

    public RideController(CurrentUserService currentUserService, RideService rideService) {
        this.currentUserService = currentUserService;
        this.rideService = rideService;
    }

    @PostMapping
    public RideResponse createRide(@Valid @RequestBody CreateRideRequest request) {
        User user = currentUserService.currentUser();
        return rideService.createRide(user, request);
    }

    @GetMapping("/my")
    public List<RideResponse> myRides() {
        User user = currentUserService.currentUser();
        return rideService.myRides(user);
    }

    @GetMapping("/{rideId}")
    public RideResponse getRide(@PathVariable Long rideId) {
        User user = currentUserService.currentUser();
        return rideService.getRide(user, rideId);
    }

    @PostMapping("/{rideId}/accept")
    public RideResponse acceptRide(@PathVariable Long rideId) {
        User user = currentUserService.currentUser();
        return rideService.acceptRide(user, rideId);
    }

    @PostMapping("/{rideId}/reject")
    public RideResponse rejectRide(@PathVariable Long rideId) {
        User user = currentUserService.currentUser();
        return rideService.rejectRide(user, rideId);
    }

    @PostMapping("/{rideId}/start")
    public RideResponse startRide(@PathVariable Long rideId) {
        User user = currentUserService.currentUser();
        return rideService.startRide(user, rideId);
    }

    @PostMapping("/{rideId}/complete")
    public RideResponse completeRide(@PathVariable Long rideId) {
        User user = currentUserService.currentUser();
        return rideService.completeRide(user, rideId);
    }

    @PostMapping("/{rideId}/cancel")
    public RideResponse cancelRide(@PathVariable Long rideId) {
        User user = currentUserService.currentUser();
        return rideService.cancelRide(user, rideId);
    }
}
