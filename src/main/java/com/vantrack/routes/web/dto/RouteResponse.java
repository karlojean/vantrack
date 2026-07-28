package com.vantrack.routes.web.dto;

import com.vantrack.routes.Route;

public record RouteResponse (
    String name,
    String plate,
    String driver
) {
    public static RouteResponse fromEntity(Route route) {
        return new RouteResponse(route.getName(), route.getVan().getPlate(), route.getVan().getDriver().getName());
    }
}
