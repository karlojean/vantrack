package com.vantrack.tracking.web;

import com.vantrack.tracking.ListTripLocationsUseCase;
import com.vantrack.tracking.web.dto.SendLocationRequest;
import com.vantrack.tracking.SendLocationUseCase;
import com.vantrack.tracking.web.dto.TripLocationResponse;
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
@RequestMapping("/trackings")
public class TrackingController {

    private final SendLocationUseCase sendLocationUseCase;
    private final ListTripLocationsUseCase listTripLocationsUseCase;

    public TrackingController(SendLocationUseCase sendLocationUseCase, ListTripLocationsUseCase listTripLocationsUseCase) {
        this.sendLocationUseCase = sendLocationUseCase;
        this.listTripLocationsUseCase = listTripLocationsUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_DRIVER')")
    @ResponseStatus(HttpStatus.CREATED)
    void sendLocation(
            @RequestBody @Valid SendLocationRequest request,
            @AuthenticationPrincipal Jwt jwt
    ){
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("userId")));
        sendLocationUseCase.execute(request, userId);
    }

    @GetMapping("/{tripId}")
    List<TripLocationResponse> listTripLocations(
            @PathVariable UUID tripId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("userId")));
        return listTripLocationsUseCase.execute(tripId, userId);
    }

}
