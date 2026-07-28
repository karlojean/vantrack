package com.vantrack.vans.web;

import com.vantrack.vans.CreateVanUseCase;
import com.vantrack.vans.ListAllVansUseCase;
import com.vantrack.vans.web.dto.CreateVanRequest;
import com.vantrack.vans.web.dto.VanFilter;
import com.vantrack.vans.web.dto.VanResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/vans")
public class VanController {


    private final CreateVanUseCase createVanUseCase;
    private final ListAllVansUseCase listAllVansUseCase;

    public VanController(CreateVanUseCase createVanUseCase, ListAllVansUseCase listAllVansUseCase) {
        this.createVanUseCase = createVanUseCase;
        this.listAllVansUseCase = listAllVansUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_DRIVER')")
    public VanResponse createVan(
            @RequestBody @Valid CreateVanRequest request,
            @AuthenticationPrincipal Jwt jwt
            ) {

        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("userId")));
        return createVanUseCase.execute(request, userId);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_DRIVER')")
    public List<VanResponse> listAllVans(@ModelAttribute VanFilter filter) {
        return listAllVansUseCase.execute(filter);
    }
}
