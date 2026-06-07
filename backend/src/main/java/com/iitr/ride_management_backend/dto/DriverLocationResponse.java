package com.iitr.ride_management_backend.dto;

import java.time.Instant;

public record DriverLocationResponse(
        Long driverId,
        String driverName,
        Double latitude,
        Double longitude,
        Double accuracy,
        Instant updatedAt
) {
}
