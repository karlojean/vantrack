package com.vantrack.trips.web;

import com.vantrack.trips.StartTripUseCase;
import com.vantrack.trips.web.dto.StartTripRequest;
import com.vantrack.trips.web.dto.TripResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
public class TripController {

    private final StartTripUseCase startTripUseCase;

    public TripController(StartTripUseCase startTripUseCase) {
        this.startTripUseCase = startTripUseCase;
    }

    @PostMapping("/start")
    @PreAuthorize("hasAuthority('SCOPE_DRIVER')")
    TripResponse startTrip(
            @RequestBody @Valid StartTripRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("userId")));
        return startTripUseCase.execute(request, userId);
    }

}
