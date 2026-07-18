package com.vantrack.users.web.dto;

import com.vantrack.users.User;
import com.vantrack.users.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotEmpty(message = "O nome usuário não pode estar vazio")
        String name,

        @NotEmpty(message = "O e-mail do usuário não pode estar vazio")
        @Email(message = "O e-mail deve ser válido")
        String email,

        @NotEmpty(message = "A senha do usuário não pode estar vazia")
        @Size(min = 8, max = 255, message = "A senha deve ter entre 8 e 255 caracteres")
        String password,

        @NotNull(message = "O papel (role) do usuário é obrigatório e não pode estar vazio")
        UserRole role
) {

    public User toEntity() {
        User user  = new User();
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);

        return user;
    }
}
