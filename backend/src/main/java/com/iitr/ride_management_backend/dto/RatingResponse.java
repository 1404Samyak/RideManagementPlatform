package com.iitr.ride_management_backend.dto;

import java.time.Instant;

public record RatingResponse(
        Long id,
        Long rideId,
        Long passengerId,
        Long driverId,
        int score,
        String feedback,
        Instant createdAt
) {
}
