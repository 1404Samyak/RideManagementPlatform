package com.iitr.ride_management_backend.dto;

import java.time.Instant;

public record RealtimeEvent<T>(
        String type,
        String message,
        T payload,
        Instant timestamp
) {
    public static <T> RealtimeEvent<T> of(String type, String message, T payload) {
        return new RealtimeEvent<>(type, message, payload, Instant.now());
    }
}
