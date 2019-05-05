package pl.jsql.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import pl.jsql.dto.BasicResponse;
import pl.jsql.dto.MessageResponse;
import pl.jsql.exceptions.JSQLException;

@Component
abstract public class ValidateController {

    @ExceptionHandler(JSQLException.class)
    public ResponseEntity handleJSQLException(JSQLException ex) {

        MessageResponse messageResponse = new MessageResponse(ex.getDescription() == null ?  ex.getMessage() : ex.getDescription());
        return new ResponseEntity<>(new BasicResponse<>(500, messageResponse), HttpStatus.INTERNAL_SERVER_ERROR);

    }

}
