package com.vantrack.users.web.dto;

import com.vantrack.users.User;
import com.vantrack.users.UserRole;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        UserRole role
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
