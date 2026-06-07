package com.iitr.ride_management_backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRideRequest(
        @NotBlank String pickupLocation,
        @NotBlank String destination,
        Long pickupLocationId,
        Long destinationLocationId
) {
}
