package com.vantrack.users.web.dto;

import com.vantrack.users.UserRole;

public record UserFilter (
        String name,
        UserRole role
) {
}
