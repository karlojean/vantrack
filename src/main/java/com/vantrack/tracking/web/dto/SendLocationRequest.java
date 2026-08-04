package com.vantrack.tracking.web.dto;

import com.vantrack.tracking.TripLocation;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record SendLocationRequest(
        @NotNull(message = "Id da viagem e obrigatório")
        UUID tripId,

        @NotNull(message = "Latitude e obrigatório")
        @DecimalMin(value = "-90.0", message = "Latitude deve ser no mínimo -90.0")
        @DecimalMax(value = "90.0", message = "Latitude deve ser no máximo 90.0")
        BigDecimal latitude,

        @NotNull(message = "Longitude e obrigatório")
        @DecimalMin(value = "-180.0", message = "Longitude deve ser no mínimo -180.0")
        @DecimalMax(value = "180.0", message = "Longitude mdeve ser no máximo 180.0")
        BigDecimal longitude

) {
    public static TripLocation toEntity(SendLocationRequest request) {
        TripLocation location = new TripLocation();
        location.setLatitude(request.latitude);
        location.setLongitude(request.longitude);

        return location;
    }
}
