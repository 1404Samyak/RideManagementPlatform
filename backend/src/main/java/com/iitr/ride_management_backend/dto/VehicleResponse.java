package com.iitr.ride_management_backend.dto;

public record VehicleResponse(
        Long id,
        String vehicleNumber,
        String vehicleType,
        int capacity
) {
}
