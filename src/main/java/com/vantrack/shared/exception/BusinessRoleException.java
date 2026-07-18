package com.vantrack.shared.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor(force = true)
@EqualsAndHashCode(callSuper = true)
public class BusinessRoleException extends RuntimeException {
    String title;
    HttpStatus status;
    public BusinessRoleException(String title, String message, HttpStatus status){
        super(message);
        this.title = title;
        this.status = status;
    }

}
