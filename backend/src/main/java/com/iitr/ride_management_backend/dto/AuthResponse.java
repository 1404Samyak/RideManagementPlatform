package com.iitr.ride_management_backend.dto;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
