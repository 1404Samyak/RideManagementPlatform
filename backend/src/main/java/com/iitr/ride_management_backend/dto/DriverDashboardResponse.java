package com.iitr.ride_management_backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record DriverDashboardResponse(
        long totalRidesCompleted,
        long activeRides,
        BigDecimal averageRating,
        int ratingCount,
        RideResponse activeRide,
        List<RideResponse> rideHistory,
        List<RatingResponse> ratingsReceived,
        List<StatusCountResponse> rideStatusBreakdown
) {
}
