package com.vantrack.routes.web.dto;

import com.vantrack.routes.Route;

import java.util.UUID;

public record RouteResponse (
    UUID id,
    String name,
    String plate,
    String driver
) {
    public static RouteResponse fromEntity(Route route) {
        return new RouteResponse(route.getId(), route.getName(), route.getVan().getPlate(), route.getVan().getDriver().getName());
    }
}
