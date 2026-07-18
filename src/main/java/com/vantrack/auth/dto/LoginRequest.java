package com.vantrack.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record LoginRequest (

        @Email
        @NotEmpty
        @Size(min = 3, max = 254)
        String email,

        @NotEmpty
        @Size(min = 8, max = 256)
        String password
) {
}
