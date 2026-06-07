package com.iitr.ride_management_backend.service;

import com.iitr.ride_management_backend.domain.AvailabilityStatus;
import com.iitr.ride_management_backend.domain.DriverProfile;
import com.iitr.ride_management_backend.domain.RideStatus;
import com.iitr.ride_management_backend.domain.Role;
import com.iitr.ride_management_backend.domain.User;
import com.iitr.ride_management_backend.dto.DriverDashboardResponse;
import com.iitr.ride_management_backend.dto.DriverLocationRequest;
import com.iitr.ride_management_backend.dto.DriverLocationResponse;
import com.iitr.ride_management_backend.dto.DriverSummaryResponse;
import com.iitr.ride_management_backend.dto.RatingResponse;
import com.iitr.ride_management_backend.dto.RideResponse;
import com.iitr.ride_management_backend.dto.StatusCountResponse;
import com.iitr.ride_management_backend.exception.BadRequestException;
import com.iitr.ride_management_backend.exception.ForbiddenException;
import com.iitr.ride_management_backend.exception.NotFoundException;
import com.iitr.ride_management_backend.repository.DriverProfileRepository;
import com.iitr.ride_management_backend.repository.RatingRepository;
import com.iitr.ride_management_backend.repository.RideRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriverService {

    private static final List<RideStatus> ACTIVE_RIDE_STATUSES = List.of(RideStatus.ACCEPTED, RideStatus.IN_PROGRESS);

    private final DriverProfileRepository driverProfileRepository;
    private final RideRepository rideRepository;
    private final RatingRepository ratingRepository;
    private final ResponseMapper mapper;
    private final RealtimeService realtimeService;

    public DriverService(
            DriverProfileRepository driverProfileRepository,
            RideRepository rideRepository,
            RatingRepository ratingRepository,
            ResponseMapper mapper,
            RealtimeService realtimeService
    ) {
        this.driverProfileRepository = driverProfileRepository;
        this.rideRepository = rideRepository;
        this.ratingRepository = ratingRepository;
        this.mapper = mapper;
        this.realtimeService = realtimeService;
    }

    @Transactional
    public DriverSummaryResponse goOnline(User user) {
        DriverProfile profile = requireDriverProfile(user);
        if (hasActiveRide(user.getId())) {
            profile.setAvailabilityStatus(AvailabilityStatus.BUSY);
        } else {
            profile.setAvailabilityStatus(AvailabilityStatus.ONLINE);
        }
        DriverSummaryResponse response = mapper.driverSummary(driverProfileRepository.save(profile));
        realtimeService.driverAvailabilityChanged(response);
        return response;
    }

    @Transactional
    public DriverSummaryResponse goOffline(User user) {
        DriverProfile profile = requireDriverProfile(user);
        if (hasActiveRide(user.getId())) {
            throw new BadRequestException("Complete or cancel the active ride before going offline");
        }
        profile.setAvailabilityStatus(AvailabilityStatus.OFFLINE);
        DriverSummaryResponse response = mapper.driverSummary(driverProfileRepository.save(profile));
        realtimeService.driverAvailabilityChanged(response);
        return response;
    }

    @Transactional(readOnly = true)
    public List<DriverSummaryResponse> availableDrivers() {
        return driverProfileRepository.findByAvailabilityStatusOrderByUserNameAsc(AvailabilityStatus.ONLINE)
                .stream()
                .map(mapper::driverSummary)
                .toList();
    }

    @Transactional
    public DriverLocationResponse updateLocation(User user, DriverLocationRequest request) {
        DriverProfile profile = requireDriverProfile(user);
        if (profile.getAvailabilityStatus() == AvailabilityStatus.OFFLINE) {
            throw new BadRequestException("Go online before sending live location");
        }

        profile.setCurrentLatitude(request.latitude());
        profile.setCurrentLongitude(request.longitude());
        profile.setLocationAccuracy(request.accuracy());
        profile.setLastLocationUpdatedAt(Instant.now());
        DriverProfile savedProfile = driverProfileRepository.save(profile);
        DriverLocationResponse location = mapper.driverLocation(savedProfile);
        realtimeService.driverLocationChanged(location);

        rideRepository.findByDriverIdAndStatusInOrderByRequestedAtDesc(user.getId(), ACTIVE_RIDE_STATUSES)
                .stream()
                .findFirst()
                .ifPresent(ride -> realtimeService.driverLocationChangedForRide(mapper.rideResponse(ride), location));
        return location;
    }

    @Transactional(readOnly = true)
    public List<RideResponse> incomingRequests(User user) {
        requireDriverProfile(user);
        return rideRepository.findIncomingRequestsForDriver(user.getId())
                .stream()
                .map(mapper::rideResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DriverDashboardResponse dashboard(User user) {
        DriverProfile profile = requireDriverProfile(user);
        Long driverId = user.getId();
        List<RideResponse> history = rideRepository.findByDriverIdOrderByRequestedAtDesc(driverId)
                .stream()
                .map(mapper::rideResponse)
                .toList();
        List<RatingResponse> ratings = ratingRepository.findByDriverIdOrderByCreatedAtDesc(driverId)
                .stream()
                .map(mapper::ratingResponse)
                .toList();
        RideResponse activeRide = rideRepository.findByDriverIdAndStatusInOrderByRequestedAtDesc(driverId, ACTIVE_RIDE_STATUSES)
                .stream()
                .findFirst()
                .map(mapper::rideResponse)
                .orElse(null);
        return new DriverDashboardResponse(
                rideRepository.countByDriverIdAndStatus(driverId, RideStatus.COMPLETED),
                rideRepository.countByDriverIdAndStatusIn(driverId, ACTIVE_RIDE_STATUSES),
                profile.getAverageRating(),
                profile.getRatingCount(),
                activeRide,
                history,
                ratings,
                List.of(
                        new StatusCountResponse(RideStatus.ACCEPTED, rideRepository.countByDriverIdAndStatus(driverId, RideStatus.ACCEPTED)),
                        new StatusCountResponse(RideStatus.IN_PROGRESS, rideRepository.countByDriverIdAndStatus(driverId, RideStatus.IN_PROGRESS)),
                        new StatusCountResponse(RideStatus.COMPLETED, rideRepository.countByDriverIdAndStatus(driverId, RideStatus.COMPLETED)),
                        new StatusCountResponse(RideStatus.CANCELLED, rideRepository.countByDriverIdAndStatus(driverId, RideStatus.CANCELLED))
                )
        );
    }

    @Transactional(readOnly = true)
    public List<RideResponse> rideHistory(User user) {
        requireDriverProfile(user);
        return rideRepository.findByDriverIdOrderByRequestedAtDesc(user.getId())
                .stream()
                .map(mapper::rideResponse)
                .toList();
    }

    public DriverProfile requireDriverProfile(User user) {
        if (user.getRole() != Role.DRIVER) {
            throw new ForbiddenException("Driver access required");
        }
        return driverProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Driver profile not found"));
    }

    public boolean hasActiveRide(Long driverId) {
        return rideRepository.countByDriverIdAndStatusIn(driverId, ACTIVE_RIDE_STATUSES) > 0;
    }
}
