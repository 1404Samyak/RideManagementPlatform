package com.iitr.ride_management_backend.service;

import com.iitr.ride_management_backend.domain.AvailabilityStatus;
import com.iitr.ride_management_backend.domain.CampusLocation;
import com.iitr.ride_management_backend.domain.DriverProfile;
import com.iitr.ride_management_backend.domain.Ride;
import com.iitr.ride_management_backend.domain.RideRejection;
import com.iitr.ride_management_backend.domain.RideStatus;
import com.iitr.ride_management_backend.domain.Role;
import com.iitr.ride_management_backend.domain.User;
import com.iitr.ride_management_backend.dto.CreateRideRequest;
import com.iitr.ride_management_backend.dto.RideResponse;
import com.iitr.ride_management_backend.exception.BadRequestException;
import com.iitr.ride_management_backend.exception.ForbiddenException;
import com.iitr.ride_management_backend.exception.NotFoundException;
import com.iitr.ride_management_backend.repository.CampusLocationRepository;
import com.iitr.ride_management_backend.repository.DriverProfileRepository;
import com.iitr.ride_management_backend.repository.RideRejectionRepository;
import com.iitr.ride_management_backend.repository.RideRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RideService {

    private static final List<RideStatus> PASSENGER_ACTIVE_STATUSES = List.of(
            RideStatus.REQUESTED,
            RideStatus.ACCEPTED,
            RideStatus.IN_PROGRESS
    );

    private final RideRepository rideRepository;
    private final RideRejectionRepository rideRejectionRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final CampusLocationRepository campusLocationRepository;
    private final DriverService driverService;
    private final ResponseMapper mapper;
    private final RealtimeService realtimeService;

    public RideService(
            RideRepository rideRepository,
            RideRejectionRepository rideRejectionRepository,
            DriverProfileRepository driverProfileRepository,
            CampusLocationRepository campusLocationRepository,
            DriverService driverService,
            ResponseMapper mapper,
            RealtimeService realtimeService
    ) {
        this.rideRepository = rideRepository;
        this.rideRejectionRepository = rideRejectionRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.campusLocationRepository = campusLocationRepository;
        this.driverService = driverService;
        this.mapper = mapper;
        this.realtimeService = realtimeService;
    }

    @Transactional
    public RideResponse createRide(User user, CreateRideRequest request) {
        requirePassenger(user);
        boolean hasActiveRide = rideRepository.findByPassengerIdOrderByRequestedAtDesc(user.getId())
                .stream()
                .anyMatch(ride -> PASSENGER_ACTIVE_STATUSES.contains(ride.getStatus()));
        if (hasActiveRide) {
            throw new BadRequestException("You already have an active ride");
        }
        ResolvedLocation pickup = resolveLocation(request.pickupLocation(), request.pickupLocationId());
        ResolvedLocation destination = resolveLocation(request.destination(), request.destinationLocationId());
        if (pickup.name().equalsIgnoreCase(destination.name())) {
            throw new BadRequestException("Pickup and destination must be different");
        }
        Ride ride = new Ride(
                user,
                pickup.name(),
                destination.name(),
                pickup.latitude(),
                pickup.longitude(),
                destination.latitude(),
                destination.longitude()
        );
        RideResponse response = mapper.rideResponse(rideRepository.save(ride));
        realtimeService.rideRequested(response);
        return response;
    }

    @Transactional(readOnly = true)
    public List<RideResponse> myRides(User user) {
        if (user.getRole() == Role.PASSENGER) {
            return rideRepository.findByPassengerIdOrderByRequestedAtDesc(user.getId())
                    .stream()
                    .map(mapper::rideResponse)
                    .toList();
        }
        if (user.getRole() == Role.DRIVER) {
            return rideRepository.findByDriverIdOrderByRequestedAtDesc(user.getId())
                    .stream()
                    .map(mapper::rideResponse)
                    .toList();
        }
        return List.of();
    }

    @Transactional(readOnly = true)
    public RideResponse getRide(User user, Long rideId) {
        Ride ride = findRide(rideId);
        ensureRideParticipantOrRequestedDriver(user, ride);
        return mapper.rideResponse(ride);
    }

    @Transactional
    public RideResponse acceptRide(User user, Long rideId) {
        DriverProfile profile = driverService.requireDriverProfile(user);
        if (profile.getAvailabilityStatus() != AvailabilityStatus.ONLINE) {
            throw new BadRequestException("Driver must be online to accept a ride");
        }
        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new NotFoundException("Ride not found"));
        if (ride.getStatus() != RideStatus.REQUESTED || ride.getDriver() != null) {
            throw new BadRequestException("Ride is no longer available");
        }

        ride.setDriver(user);
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setAcceptedAt(Instant.now());
        profile.setAvailabilityStatus(AvailabilityStatus.BUSY);

        driverProfileRepository.save(profile);
        RideResponse response = mapper.rideResponse(rideRepository.save(ride));
        realtimeService.driverAvailabilityChanged(mapper.driverSummary(profile));
        realtimeService.rideChanged("RIDE_ACCEPTED", "Ride accepted", response);
        return response;
    }

    @Transactional
    public RideResponse rejectRide(User user, Long rideId) {
        driverService.requireDriverProfile(user);
        Ride ride = findRide(rideId);
        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new BadRequestException("Only requested rides can be rejected");
        }
        if (!rideRejectionRepository.existsByRideIdAndDriverId(rideId, user.getId())) {
            rideRejectionRepository.save(new RideRejection(ride, user));
        }
        RideResponse response = mapper.rideResponse(ride);
        realtimeService.driverDashboardChanged(user.getId(), "RIDE_REJECTED", response);
        realtimeService.rideChanged("RIDE_REJECTED", "Ride rejected by " + user.getName(), response);
        return response;
    }

    @Transactional
    public RideResponse startRide(User user, Long rideId) {
        Ride ride = findRide(rideId);
        requireAssignedDriver(user, ride);
        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new BadRequestException("Only accepted rides can be started");
        }
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartedAt(Instant.now());
        RideResponse response = mapper.rideResponse(rideRepository.save(ride));
        realtimeService.rideChanged("RIDE_STARTED", "Ride started", response);
        return response;
    }

    @Transactional
    public RideResponse completeRide(User user, Long rideId) {
        Ride ride = findRide(rideId);
        requireAssignedDriver(user, ride);
        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new BadRequestException("Only in-progress rides can be completed");
        }
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(Instant.now());

        DriverProfile profile = driverProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Driver profile not found"));
        profile.setAvailabilityStatus(AvailabilityStatus.ONLINE);
        driverProfileRepository.save(profile);

        RideResponse response = mapper.rideResponse(rideRepository.save(ride));
        realtimeService.driverAvailabilityChanged(mapper.driverSummary(profile));
        realtimeService.rideChanged("RIDE_COMPLETED", "Ride completed", response);
        return response;
    }

    @Transactional
    public RideResponse cancelRide(User user, Long rideId) {
        Ride ride = findRide(rideId);
        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.CANCELLED) {
            throw new BadRequestException("Ride cannot be cancelled");
        }
        boolean passengerOwnsRide = user.getRole() == Role.PASSENGER
                && ride.getPassenger().getId().equals(user.getId());
        boolean assignedDriverOwnsRide = user.getRole() == Role.DRIVER
                && ride.getDriver() != null
                && ride.getDriver().getId().equals(user.getId());
        if (!passengerOwnsRide && !assignedDriverOwnsRide) {
            throw new ForbiddenException("You cannot cancel this ride");
        }

        Long driverId = ride.getDriver() == null ? null : ride.getDriver().getId();
        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancelledAt(Instant.now());

        DriverProfile profile = null;
        if (driverId != null) {
            profile = driverProfileRepository.findByUserId(driverId)
                    .orElseThrow(() -> new NotFoundException("Driver profile not found"));
            profile.setAvailabilityStatus(AvailabilityStatus.ONLINE);
            driverProfileRepository.save(profile);
        }

        RideResponse response = mapper.rideResponse(rideRepository.save(ride));
        if (profile != null) {
            realtimeService.driverAvailabilityChanged(mapper.driverSummary(profile));
        }
        realtimeService.rideChanged("RIDE_CANCELLED", "Ride cancelled", response);
        return response;
    }

    private Ride findRide(Long rideId) {
        return rideRepository.findById(rideId).orElseThrow(() -> new NotFoundException("Ride not found"));
    }

    private ResolvedLocation resolveLocation(String typedName, Long locationId) {
        if (locationId != null) {
            CampusLocation location = campusLocationRepository.findById(locationId)
                    .orElseThrow(() -> new NotFoundException("Campus location not found"));
            return new ResolvedLocation(location.getName(), location.getLatitude(), location.getLongitude());
        }

        String name = typedName.trim();
        return campusLocationRepository.findByNameIgnoreCase(name)
                .map(location -> new ResolvedLocation(location.getName(), location.getLatitude(), location.getLongitude()))
                .orElseGet(() -> new ResolvedLocation(name, null, null));
    }

    private void requirePassenger(User user) {
        if (user.getRole() != Role.PASSENGER) {
            throw new ForbiddenException("Passenger access required");
        }
    }

    private void requireAssignedDriver(User user, Ride ride) {
        if (user.getRole() != Role.DRIVER || ride.getDriver() == null || !ride.getDriver().getId().equals(user.getId())) {
            throw new ForbiddenException("Assigned driver access required");
        }
    }

    private void ensureRideParticipantOrRequestedDriver(User user, Ride ride) {
        boolean passengerOwnsRide = ride.getPassenger().getId().equals(user.getId());
        boolean driverOwnsRide = ride.getDriver() != null && ride.getDriver().getId().equals(user.getId());
        boolean requestedRideForDriverPool = user.getRole() == Role.DRIVER && ride.getStatus() == RideStatus.REQUESTED;
        if (!passengerOwnsRide && !driverOwnsRide && !requestedRideForDriverPool) {
            throw new ForbiddenException("You cannot view this ride");
        }
    }

    private record ResolvedLocation(String name, Double latitude, Double longitude) {
    }
}
