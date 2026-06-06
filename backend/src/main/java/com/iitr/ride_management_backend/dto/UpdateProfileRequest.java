package com.iitr.ride_management_backend.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @NotBlank String name,
        @NotBlank String phone,
        String campusAddress
) {
}
