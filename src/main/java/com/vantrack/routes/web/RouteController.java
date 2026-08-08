package com.vantrack.routes.web;

import com.vantrack.routes.AddParentUseCase;
import com.vantrack.routes.CreateRouteUseCase;
import com.vantrack.routes.ListAllRoutesUseCase;
import com.vantrack.routes.web.dto.AddParentRequest;
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
    private final AddParentUseCase addParentUseCase;

    public RouteController(CreateRouteUseCase createRouteUseCase, ListAllRoutesUseCase listAllRoutesUseCase, AddParentUseCase addParentUseCase) {
        this.createRouteUseCase = createRouteUseCase;
        this.listAllRoutesUseCase = listAllRoutesUseCase;
        this.addParentUseCase = addParentUseCase;
    }

    @PostMapping
    RouteResponse createRoute(
            @RequestBody @Valid CreateRouteRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("userId")));
        return createRouteUseCase.execute(request, userId);
    }

    @GetMapping
    List<RouteResponse> ListAllRoutes(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("userId")));
        return listAllRoutesUseCase.execute(userId);
    }

    @PostMapping("{id}/parent")
    void addParent(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody AddParentRequest request
    ) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("userId")));
        addParentUseCase.execute(id, request, userId);
    }
}
