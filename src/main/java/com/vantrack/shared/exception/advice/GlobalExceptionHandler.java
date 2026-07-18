package com.vantrack.shared.exception.advice;

import com.vantrack.shared.exception.BusinessRoleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessRoleException.class)
    public ProblemDetail handleBusinessRole(BusinessRoleException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                ex.getStatus(),
                ex.getMessage()
        );
        pd.setTitle(ex.getTitle());

        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUncaughtException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado no servidor. Tente novamente mais tarde."
        );
        problemDetail.setTitle("Erro Interno do Servidor");

        return problemDetail;
    }
}
