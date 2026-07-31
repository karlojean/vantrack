package com.vantrack.trips.web.dto;


import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record StartTripRequest(
        @NotNull(message = "Id da rota é obrigatório e não deve ser nulo")
        UUID routeId,

        @NotNull(message = "Latitude é obrigatório e não deve ser nulo")
        @DecimalMin(value = "-90.0", message = "Latitude deve ser no mínimo -90.0")
        @DecimalMax(value = "90.0", message = "Latitude deve ser no máximo 90.0")
        BigDecimal latitude,

        @NotNull(message = "Longitude e obrigatório")
        @DecimalMin(value = "-180.0", message = "Longitude deve ser no mínimo -180.0")
        @DecimalMax(value = "180.0", message = "Longitude mdeve ser no máximo 180.0")
        BigDecimal longitude
) {
}
