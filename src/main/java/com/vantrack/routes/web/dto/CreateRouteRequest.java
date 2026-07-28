package com.vantrack.routes.web.dto;

import com.vantrack.routes.Route;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;


public record CreateRouteRequest (
        @Size(max = 200, message = "O nome deve conter no máximo 200 caracteres")
        @NotEmpty(message = "O nome é obrigatório e não pode estar vazio")
        String name,

        @NotNull(message = "O id da van é obrigatório")
        UUID vanId
) {
    public Route toDomain() {
        Route route = new Route();
        route.setName(name);

        return route;
    }
}
