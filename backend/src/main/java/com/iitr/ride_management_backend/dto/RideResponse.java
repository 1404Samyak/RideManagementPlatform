package com.iitr.ride_management_backend.dto;

import com.iitr.ride_management_backend.domain.RideStatus;
import java.time.Instant;

public record RideResponse(
        Long id,
        BasicUserResponse passenger,
        BasicUserResponse driver,
        String pickupLocation,
        String destination,
        RideStatus status,
        Instant requestedAt,
        Instant acceptedAt,
        Instant startedAt,
        Instant completedAt,
        Instant cancelledAt,
        RatingResponse rating
) {
}
