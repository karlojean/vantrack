package com.vantrack.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessRuleException extends RuntimeException {
    String title;
    HttpStatus status;
    public BusinessRuleException(String title, String message, HttpStatus status){
        super(message);
        this.title = title;
        this.status = status;
    }

}
