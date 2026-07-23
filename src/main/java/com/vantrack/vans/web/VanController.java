package com.vantrack.vans.web;

import com.vantrack.users.User;
import com.vantrack.vans.CreateVanUseCase;
import com.vantrack.vans.Van;
import com.vantrack.vans.web.dto.CreateVanRequest;
import com.vantrack.vans.web.dto.VanResponse;
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
@RequestMapping("/vans")
public class VanController {


    private final CreateVanUseCase createVanUseCase;

    public VanController(CreateVanUseCase createVanUseCase) {
        this.createVanUseCase = createVanUseCase;
    }

    @PostMapping
    public VanResponse createVan(
            @RequestBody @Valid CreateVanRequest request,
            @AuthenticationPrincipal Jwt jwt
            ) {

        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("userId")));
        return createVanUseCase.execute(request, userId);
    };


}
