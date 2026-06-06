package com.iitr.ride_management_backend.service;

import com.iitr.ride_management_backend.domain.DriverProfile;
import com.iitr.ride_management_backend.domain.Rating;
import com.iitr.ride_management_backend.domain.Ride;
import com.iitr.ride_management_backend.domain.RideStatus;
import com.iitr.ride_management_backend.domain.Role;
import com.iitr.ride_management_backend.domain.User;
import com.iitr.ride_management_backend.dto.RatingRequest;
import com.iitr.ride_management_backend.dto.RatingResponse;
import com.iitr.ride_management_backend.exception.BadRequestException;
import com.iitr.ride_management_backend.exception.ForbiddenException;
import com.iitr.ride_management_backend.exception.NotFoundException;
import com.iitr.ride_management_backend.repository.DriverProfileRepository;
import com.iitr.ride_management_backend.repository.RatingRepository;
import com.iitr.ride_management_backend.repository.RideRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final RideRepository rideRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final ResponseMapper mapper;
    private final RealtimeService realtimeService;

    public RatingService(
            RatingRepository ratingRepository,
            RideRepository rideRepository,
            DriverProfileRepository driverProfileRepository,
            ResponseMapper mapper,
            RealtimeService realtimeService
    ) {
        this.ratingRepository = ratingRepository;
        this.rideRepository = rideRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.mapper = mapper;
        this.realtimeService = realtimeService;
    }

    @Transactional
    public RatingResponse rateRide(User user, RatingRequest request) {
        if (user.getRole() != Role.PASSENGER) {
            throw new ForbiddenException("Passenger access required");
        }
        Ride ride = rideRepository.findById(request.rideId())
                .orElseThrow(() -> new NotFoundException("Ride not found"));
        if (!ride.getPassenger().getId().equals(user.getId())) {
            throw new ForbiddenException("You can only rate your own rides");
        }
        if (ride.getStatus() != RideStatus.COMPLETED || ride.getDriver() == null) {
            throw new BadRequestException("Only completed rides can be rated");
        }
        if (ratingRepository.existsByRideId(ride.getId())) {
            throw new BadRequestException("Ride has already been rated");
        }

        Rating rating = new Rating(
                ride,
                user,
                ride.getDriver(),
                request.rating(),
                blankToNull(request.feedback())
        );
        Rating savedRating = ratingRepository.save(rating);
        updateDriverAverage(ride.getDriver().getId(), request.rating());

        RatingResponse response = mapper.ratingResponse(savedRating);
        realtimeService.rideChanged("RATING_SUBMITTED", "Passenger submitted ride feedback", mapper.rideResponse(ride));
        realtimeService.driverDashboardChanged(ride.getDriver().getId(), "RATING_SUBMITTED", response);
        return response;
    }

    private void updateDriverAverage(Long driverId, int score) {
        DriverProfile profile = driverProfileRepository.findByUserId(driverId)
                .orElseThrow(() -> new NotFoundException("Driver profile not found"));
        BigDecimal total = profile.getAverageRating()
                .multiply(BigDecimal.valueOf(profile.getRatingCount()))
                .add(BigDecimal.valueOf(score));
        int newCount = profile.getRatingCount() + 1;
        profile.setRatingCount(newCount);
        profile.setAverageRating(total.divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP));
        driverProfileRepository.save(profile);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
