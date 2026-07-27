package com.vantrack.vans.web.dto;

import java.util.UUID;

public record VanFilter (
        UUID userId,
        String plate
) {
}
