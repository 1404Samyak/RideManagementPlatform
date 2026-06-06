package com.iitr.ride_management_backend.controller;

import com.iitr.ride_management_backend.dto.RatingRequest;
import com.iitr.ride_management_backend.dto.RatingResponse;
import com.iitr.ride_management_backend.service.CurrentUserService;
import com.iitr.ride_management_backend.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final CurrentUserService currentUserService;
    private final RatingService ratingService;

    public RatingController(CurrentUserService currentUserService, RatingService ratingService) {
        this.currentUserService = currentUserService;
        this.ratingService = ratingService;
    }

    @PostMapping
    public RatingResponse rateRide(@Valid @RequestBody RatingRequest request) {
        return ratingService.rateRide(currentUserService.currentUser(), request);
    }
}
