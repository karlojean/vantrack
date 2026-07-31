package com.vantrack.trips.web.dto;

import com.vantrack.trips.Trip;
import com.vantrack.trips.TripStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TripResponse (
        UUID id,
        UUID routeId,
        TripStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt
) {
    public static TripResponse fromEntity(Trip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getRoute().getId(),
                trip.getStatus(),
                trip.getStartedAt(),
                trip.getEndedAt()
        );
    }
}
