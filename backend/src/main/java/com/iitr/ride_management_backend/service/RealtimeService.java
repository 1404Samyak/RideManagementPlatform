package com.iitr.ride_management_backend.service;

import com.iitr.ride_management_backend.dto.DriverSummaryResponse;
import com.iitr.ride_management_backend.dto.DriverLocationResponse;
import com.iitr.ride_management_backend.dto.RealtimeEvent;
import com.iitr.ride_management_backend.dto.RideResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void driverAvailabilityChanged(DriverSummaryResponse driver) {
        messagingTemplate.convertAndSend(
                "/topic/drivers/availability",
                RealtimeEvent.of("DRIVER_AVAILABILITY_CHANGED", "Driver availability updated", driver)
        );
    }

    public void rideRequested(RideResponse ride) {
        messagingTemplate.convertAndSend(
                "/topic/rides/requests",
                RealtimeEvent.of("RIDE_REQUESTED", "New ride request", ride)
        );
        rideChanged("RIDE_REQUESTED", "Ride requested", ride);
    }

    public void rideChanged(String type, String message, RideResponse ride) {
        messagingTemplate.convertAndSend("/topic/rides/" + ride.id(), RealtimeEvent.of(type, message, ride));
        messagingTemplate.convertAndSend("/topic/users/" + ride.passenger().id() + "/notifications", RealtimeEvent.of(type, message, ride));
        if (ride.driver() != null) {
            messagingTemplate.convertAndSend("/topic/users/" + ride.driver().id() + "/notifications", RealtimeEvent.of(type, message, ride));
            messagingTemplate.convertAndSend("/topic/drivers/" + ride.driver().id() + "/dashboard", RealtimeEvent.of(type, message, ride));
        }
    }

    public void driverLocationChanged(DriverLocationResponse location) {
        messagingTemplate.convertAndSend(
                "/topic/drivers/location",
                RealtimeEvent.of("DRIVER_LOCATION_UPDATED", "Driver location updated", location)
        );
    }

    public void driverLocationChangedForRide(RideResponse ride, DriverLocationResponse location) {
        messagingTemplate.convertAndSend(
                "/topic/rides/" + ride.id() + "/driver-location",
                RealtimeEvent.of("DRIVER_LOCATION_UPDATED", "Driver location updated", location)
        );
        rideChanged("DRIVER_LOCATION_UPDATED", "Driver location updated", ride);
    }

    public void driverDashboardChanged(Long driverId, String type, Object payload) {
        messagingTemplate.convertAndSend(
                "/topic/drivers/" + driverId + "/dashboard",
                RealtimeEvent.of(type, "Driver dashboard updated", payload)
        );
    }
}
