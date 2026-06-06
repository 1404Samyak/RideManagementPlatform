package com.iitr.ride_management_backend.dto;

import com.iitr.ride_management_backend.domain.Role;
import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        Instant createdAt,
        PassengerDetailsResponse passenger,
        DriverDetailsResponse driver
) {
}
