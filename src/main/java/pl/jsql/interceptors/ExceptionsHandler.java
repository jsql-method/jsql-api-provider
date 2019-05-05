package pl.jsql.interceptors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import pl.jsql.dto.BasicResponse;
import pl.jsql.exceptions.JSQLException;

@ControllerAdvice
public class ExceptionsHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(JSQLException.class)
    public ResponseEntity handleUnauthorizedException(JSQLException ex) {
        return new ResponseEntity<>(new BasicResponse<>(500, ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity handleSecurityException(SecurityException ex) {
        return new ResponseEntity<>(new BasicResponse<>(401, "Unauthorized"), HttpStatus.UNAUTHORIZED);
    }

}

