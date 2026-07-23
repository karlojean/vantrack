package com.vantrack.vans.web.dto;

import com.vantrack.users.User;
import com.vantrack.vans.Van;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateVanRequest(
        @Size(min = 7, max = 7, message = "A placa deve conter 7 Caracteres")
        @NotEmpty(message =  "A placa é obrigatória e não pode estar vazio")
        String plate,

        @NotNull(message = "O id do usuário é obrigatório")
        UUID driverId
) {

    public Van toEntity() {
        Van van = new Van();
        van.setPlate(plate);

        return van;
    }
}
