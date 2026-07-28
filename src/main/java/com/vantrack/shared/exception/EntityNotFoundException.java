package com.vantrack.shared.exception;

import org.springframework.http.HttpStatus;

public class EntityNotFoundException extends BusinessRuleException {
    public EntityNotFoundException(String entidade) {
        super(String.format("%s não encontrado", entidade), String.format("%s não foi encontrado no nosso sistema", entidade), HttpStatus.NOT_FOUND);
    }
}
