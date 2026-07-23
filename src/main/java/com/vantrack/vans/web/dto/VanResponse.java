package com.vantrack.vans.web.dto;

import com.vantrack.users.UserRole;
import com.vantrack.vans.Van;

import java.util.UUID;

public record VanResponse (
        UUID id,
        String plate
) {

    public static VanResponse fromEntity(Van van) {
        return new VanResponse(
                van.getId(),
                van.getPlate()
        );
    }
}
