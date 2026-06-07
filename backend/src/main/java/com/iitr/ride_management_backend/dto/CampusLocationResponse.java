package com.iitr.ride_management_backend.dto;

public record CampusLocationResponse(
        Long id,
        String name,
        String category,
        Double latitude,
        Double longitude
) {
}
