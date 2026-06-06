package com.iitr.ride_management_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterDriverRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        @NotBlank String phone,
        @NotBlank String licenseNumber,
        @NotBlank String verificationDocument,
        @NotBlank String vehicleNumber,
        @NotBlank String vehicleType,
        @NotNull @Min(1) Integer vehicleCapacity
) {
}
