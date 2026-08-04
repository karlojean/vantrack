package com.vantrack.tracking.web;

import com.vantrack.tracking.web.dto.SendLocationRequest;
import com.vantrack.tracking.SendLocationUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/trackings")
public class TrackingController {

    private final SendLocationUseCase sendLocationUseCase;

    public TrackingController(SendLocationUseCase sendLocationUseCase) {
        this.sendLocationUseCase = sendLocationUseCase;
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

}
