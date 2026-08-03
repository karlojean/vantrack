package com.vantrack.trips.web;

import com.vantrack.trips.FinishTripUseCase;
import com.vantrack.trips.StartTripUseCase;
import com.vantrack.trips.Trip;
import com.vantrack.trips.web.dto.StartTripRequest;
import com.vantrack.trips.web.dto.TripResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
public class TripController {

    private final StartTripUseCase startTripUseCase;
    private final FinishTripUseCase finishTripUseCase;

    public TripController(StartTripUseCase startTripUseCase, FinishTripUseCase finishTripUseCase) {
        this.startTripUseCase = startTripUseCase;
        this.finishTripUseCase = finishTripUseCase;
    }

    @PutMapping("/{id}/finish")
    @PreAuthorize("hasAuthority('SCOPE_DRIVER')")
    TripResponse finishStrip(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("userId")));
        return finishTripUseCase.execute(id, userId);
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
