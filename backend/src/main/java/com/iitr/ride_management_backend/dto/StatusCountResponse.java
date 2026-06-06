package com.iitr.ride_management_backend.dto;

import com.iitr.ride_management_backend.domain.RideStatus;

public record StatusCountResponse(
        RideStatus status,
        long count
) {
}
