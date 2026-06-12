package com.iitr.ride_management_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Future;
import java.time.Instant;

public record CreateRideRequest(
        @NotBlank String pickupLocation,
        @NotBlank String destination,
        Long pickupLocationId,
        Long destinationLocationId,
        @Future Instant scheduledFor
) {
}
