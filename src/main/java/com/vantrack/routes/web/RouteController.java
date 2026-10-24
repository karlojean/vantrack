package com.vantrack.routes.web;

import com.vantrack.routes.*;
import com.vantrack.routes.web.dto.AddParentRequest;
import com.vantrack.routes.web.dto.CreateRouteRequest;
import com.vantrack.routes.web.dto.ParentResponse;
import com.vantrack.routes.web.dto.RouteResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final ListRouteParentsUseCase listRouteParentsUseCase;
    private final ListParentRoutesUseCase listParentRoutesUseCase;
    private final RemoveParentUseCase removeParentUseCase;

    public RouteController(CreateRouteUseCase createRouteUseCase, ListAllRoutesUseCase listAllRoutesUseCase, AddParentUseCase addParentUseCase, ListRouteParentsUseCase listRouteParentsUseCase, ListParentRoutesUseCase listParentRoutesUseCase, RemoveParentUseCase removeParentUseCase) {
        this.createRouteUseCase = createRouteUseCase;
        this.listAllRoutesUseCase = listAllRoutesUseCase;
        this.addParentUseCase = addParentUseCase;
        this.listRouteParentsUseCase = listRouteParentsUseCase;
        this.listParentRoutesUseCase = listParentRoutesUseCase;
        this.removeParentUseCase = removeParentUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RouteResponse createRoute(
            @RequestBody @Valid CreateRouteRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("userId")));
        return createRouteUseCase.execute(request, userId);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_DRIVER')")
    List<RouteResponse> ListAllRoutes(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("userId")));
        return listAllRoutesUseCase.execute(userId);
    }

    @PostMapping("{id}/parent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void addParent(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid AddParentRequest request
    ) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("userId")));
        addParentUseCase.execute(id, request, userId);
    }

    @DeleteMapping("{id}/parent/{parentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeParent(
            @PathVariable UUID id,
            @PathVariable UUID parentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("userId")));
        removeParentUseCase.execute(id, parentId, userId);
    }

    @GetMapping("{id}/parent")
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_DRIVER')")
    List<ParentResponse>  listRouteParents(@PathVariable UUID id,
                                           @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("userId")));
        return listRouteParentsUseCase.execute(id, userId);
    }

    @GetMapping("me")
    @PreAuthorize("hasAuthority('SCOPE_PARENT')")
    List<RouteResponse> listParentRoutes(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("userId")));
        return listParentRoutesUseCase.execute(userId);
    }
}
