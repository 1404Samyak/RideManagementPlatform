package com.iitr.ride_management_backend.dto;

import com.iitr.ride_management_backend.domain.Role;

public record BasicUserResponse(
        Long id,
        String name,
        String email,
        String phone,
        Role role
) {
}
