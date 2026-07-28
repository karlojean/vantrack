package com.vantrack.routes.web.dto;

import com.vantrack.routes.Route;

import java.util.UUID;


public record CreateRouteRequest (
        String name,
        UUID vanId
) {
    public Route toDomain() {
        Route route = new Route();
        route.setName(name);

        return route;
    }
}
