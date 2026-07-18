package com.vantrack.shared.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends BusinessRoleException{
    public EmailAlreadyExistsException(String email) {
        super("E-mail já cadastrado", String.format("O e-mail %s já esta cadastrado no nosso sistema.", email), HttpStatus.CONFLICT);
    }
}
