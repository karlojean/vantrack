package com.vantrack.routes.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AddParentRequest (
        @NotNull(message = "ID do pai não pode ser nulo.")
        UUID parentId,

        @NotBlank(message = "Nome do estudante não pode ser vazio.")
        @Size(max = 200, message = "Nome do estudante deve ser apenas 200 caracteres.")
        String studentName
) {
}
