package com.vantrack.routes.web;

import com.vantrack.routes.CreateRouteUseCase;
import com.vantrack.routes.ListAllRoutesUseCase;
import com.vantrack.routes.Route;
import com.vantrack.routes.web.dto.CreateRouteRequest;
import com.vantrack.routes.web.dto.RouteResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/routes")
public class RouteController {

    private final CreateRouteUseCase createRouteUseCase;
    private final ListAllRoutesUseCase listAllRoutesUseCase;

    public RouteController(CreateRouteUseCase createRouteUseCase, ListAllRoutesUseCase listAllRoutesUseCase) {
        this.createRouteUseCase = createRouteUseCase;
        this.listAllRoutesUseCase = listAllRoutesUseCase;
    }

    @PostMapping
    Route createRoute(
            @RequestBody @Valid CreateRouteRequest request,
            @AuthenticationPrincipal Jwt jwt
            ) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("userId")));
        return createRouteUseCase.execute(request, userId);
    }

    @GetMapping
    List<RouteResponse> createRoute(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("userId")));
        return listAllRoutesUseCase.execute(userId);
    }
}
