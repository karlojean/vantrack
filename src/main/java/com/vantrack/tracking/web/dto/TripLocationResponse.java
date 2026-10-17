package com.vantrack.tracking.web.dto;

import com.vantrack.tracking.TripLocation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TripLocationResponse(
        UUID id,
        UUID tripId,
        BigDecimal latitude,
        BigDecimal longitude,
        OffsetDateTime createdAt
) {
    public static TripLocationResponse fromEntity(TripLocation location) {
        return new TripLocationResponse(
                location.getId(),
                location.getTrip().getId(),
                location.getLatitude(),
                location.getLongitude(),
                location.getCreatedAt()
        );
    }
}
