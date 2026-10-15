package com.vantrack.tracking.web.dto;

import com.vantrack.tracking.TripLocation;
import com.vantrack.trips.Trip;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FinishTripResponse (
        String type,
        UUID tripId,
        OffsetDateTime endedAt
) {
    public static FinishTripResponse fromEntity(Trip trip) {
        return new FinishTripResponse(
                "TRIP_FINISHED",
                trip.getId(),
                trip.getEndedAt()
        );
    }
}
