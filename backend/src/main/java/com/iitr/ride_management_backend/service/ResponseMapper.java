package com.iitr.ride_management_backend.service;

import com.iitr.ride_management_backend.domain.CampusLocation;
import com.iitr.ride_management_backend.domain.DriverProfile;
import com.iitr.ride_management_backend.domain.PassengerProfile;
import com.iitr.ride_management_backend.domain.Rating;
import com.iitr.ride_management_backend.domain.Ride;
import com.iitr.ride_management_backend.domain.RideRejection;
import com.iitr.ride_management_backend.domain.User;
import com.iitr.ride_management_backend.domain.Vehicle;
import com.iitr.ride_management_backend.dto.BasicUserResponse;
import com.iitr.ride_management_backend.dto.CampusLocationResponse;
import com.iitr.ride_management_backend.dto.DriverDetailsResponse;
import com.iitr.ride_management_backend.dto.DriverLocationResponse;
import com.iitr.ride_management_backend.dto.DriverSummaryResponse;
import com.iitr.ride_management_backend.dto.PassengerDetailsResponse;
import com.iitr.ride_management_backend.dto.RatingResponse;
import com.iitr.ride_management_backend.dto.RideResponse;
import com.iitr.ride_management_backend.dto.UserResponse;
import com.iitr.ride_management_backend.dto.VehicleResponse;
import com.iitr.ride_management_backend.repository.CampusLocationRepository;
import com.iitr.ride_management_backend.repository.DriverProfileRepository;
import com.iitr.ride_management_backend.repository.PassengerProfileRepository;
import com.iitr.ride_management_backend.repository.RatingRepository;
import com.iitr.ride_management_backend.repository.RideRejectionRepository;
import com.iitr.ride_management_backend.repository.VehicleRepository;
import org.springframework.stereotype.Component;

@Component
public class ResponseMapper {

    private final CampusLocationRepository campusLocationRepository;
    private final PassengerProfileRepository passengerProfileRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final RatingRepository ratingRepository;
    private final RideRejectionRepository rideRejectionRepository;

    public ResponseMapper(
            CampusLocationRepository campusLocationRepository,
            PassengerProfileRepository passengerProfileRepository,
            DriverProfileRepository driverProfileRepository,
            VehicleRepository vehicleRepository,
            RatingRepository ratingRepository,
            RideRejectionRepository rideRejectionRepository
    ) {
        this.campusLocationRepository = campusLocationRepository;
        this.passengerProfileRepository = passengerProfileRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.ratingRepository = ratingRepository;
        this.rideRejectionRepository = rideRejectionRepository;
    }

    public BasicUserResponse basicUser(User user) {
        if (user == null) {
            return null;
        }
        return new BasicUserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole());
    }

    public UserResponse userResponse(User user) {
        PassengerDetailsResponse passengerDetails = passengerProfileRepository.findByUserId(user.getId())
                .map(this::passengerDetails)
                .orElse(null);
        DriverDetailsResponse driverDetails = driverProfileRepository.findByUserId(user.getId())
                .map(this::driverDetails)
                .orElse(null);
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getCreatedAt(),
                passengerDetails,
                driverDetails
        );
    }

    public DriverSummaryResponse driverSummary(DriverProfile profile) {
        User driver = profile.getUser();
        return new DriverSummaryResponse(
                driver.getId(),
                driver.getName(),
                driver.getPhone(),
                profile.getAvailabilityStatus(),
                profile.getAverageRating(),
                profile.getRatingCount(),
                vehicleRepository.findByDriverId(driver.getId()).map(this::vehicleResponse).orElse(null),
                driverLocation(profile)
        );
    }

    public RideResponse rideResponse(Ride ride) {
        RatingResponse rating = ratingRepository.findByRideId(ride.getId()).map(this::ratingResponse).orElse(null);
        RideRejection latestRejection = rideRejectionRepository.findTopByRideIdOrderByRejectedAtDesc(ride.getId()).orElse(null);
        DriverLocationResponse driverLocation = ride.getDriver() == null
                ? null
                : driverProfileRepository.findByUserId(ride.getDriver().getId())
                .map(this::driverLocation)
                .orElse(null);
        ResolvedMapPoint pickup = resolveMapPoint(
                ride.getPickupLocation(),
                ride.getPickupLatitude(),
                ride.getPickupLongitude()
        );
        ResolvedMapPoint destination = resolveMapPoint(
                ride.getDestination(),
                ride.getDestinationLatitude(),
                ride.getDestinationLongitude()
        );
        return new RideResponse(
                ride.getId(),
                basicUser(ride.getPassenger()),
                basicUser(ride.getDriver()),
                ride.getPickupLocation(),
                ride.getDestination(),
                pickup.latitude(),
                pickup.longitude(),
                destination.latitude(),
                destination.longitude(),
                ride.getStatus(),
                ride.getRequestedAt(),
                ride.getAcceptedAt(),
                ride.getStartedAt(),
                ride.getCompletedAt(),
                ride.getCancelledAt(),
                latestRejection == null ? null : basicUser(latestRejection.getDriver()),
                latestRejection == null ? null : latestRejection.getRejectedAt(),
                rating,
                driverLocation
        );
    }

    public CampusLocationResponse campusLocationResponse(CampusLocation location) {
        return new CampusLocationResponse(
                location.getId(),
                location.getName(),
                location.getCategory(),
                location.getLatitude(),
                location.getLongitude()
        );
    }

    public DriverLocationResponse driverLocation(DriverProfile profile) {
        if (profile.getCurrentLatitude() == null || profile.getCurrentLongitude() == null) {
            return null;
        }
        return new DriverLocationResponse(
                profile.getUser().getId(),
                profile.getUser().getName(),
                profile.getCurrentLatitude(),
                profile.getCurrentLongitude(),
                profile.getLocationAccuracy(),
                profile.getLastLocationUpdatedAt()
        );
    }

    public RatingResponse ratingResponse(Rating rating) {
        return new RatingResponse(
                rating.getId(),
                rating.getRide().getId(),
                rating.getPassenger().getId(),
                rating.getDriver().getId(),
                rating.getScore(),
                rating.getFeedback(),
                rating.getCreatedAt()
        );
    }

    private PassengerDetailsResponse passengerDetails(PassengerProfile profile) {
        return new PassengerDetailsResponse(profile.getId(), profile.getCampusAddress());
    }

    private DriverDetailsResponse driverDetails(DriverProfile profile) {
        return new DriverDetailsResponse(
                profile.getId(),
                profile.getLicenseNumber(),
                profile.getVerificationDocument(),
                profile.getVerificationStatus(),
                profile.getAvailabilityStatus(),
                profile.getAverageRating(),
                profile.getRatingCount(),
                vehicleRepository.findByDriverId(profile.getUser().getId()).map(this::vehicleResponse).orElse(null),
                driverLocation(profile)
        );
    }

    private VehicleResponse vehicleResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getVehicleNumber(),
                vehicle.getVehicleType(),
                vehicle.getCapacity()
        );
    }

    private ResolvedMapPoint resolveMapPoint(String locationName, Double fallbackLatitude, Double fallbackLongitude) {
        if (fallbackLatitude != null && fallbackLongitude != null) {
            return new ResolvedMapPoint(fallbackLatitude, fallbackLongitude);
        }
        if (locationName != null) {
            return campusLocationRepository.findByNameIgnoreCase(locationName)
                    .map(location -> new ResolvedMapPoint(location.getLatitude(), location.getLongitude()))
                    .orElseGet(() -> new ResolvedMapPoint(fallbackLatitude, fallbackLongitude));
        }
        return new ResolvedMapPoint(fallbackLatitude, fallbackLongitude);
    }

    private record ResolvedMapPoint(Double latitude, Double longitude) {
    }
}
