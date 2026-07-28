package com.vantrack.routes.web;

import com.vantrack.routes.CreateRouteUseCase;
import com.vantrack.routes.Route;
import com.vantrack.routes.web.dto.CreateRouteRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/routes")
public class RouteController {

    private final CreateRouteUseCase createRouteUseCase;

    public RouteController(CreateRouteUseCase createRouteUseCase) {
        this.createRouteUseCase = createRouteUseCase;
    }

    @PostMapping
    Route createRoute(
            @RequestBody @Valid CreateRouteRequest request,
            @AuthenticationPrincipal Jwt jwt
            ) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("userId")));
        return createRouteUseCase.execute(request, userId);
    }
}
