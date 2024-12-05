package com.mediplanpro.exception;

import org.everit.json.schema.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GlobalExceptionHandler handles all exceptions globally, providing detailed error messages.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle JSON Schema validation errors and return detailed messages.
     *
     * @param e The ValidationException thrown during schema validation.
     * @return A ResponseEntity containing detailed validation errors.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<String> handleValidationException(ValidationException e) {
        //logger.info("VALIDATING ERROR: SCHEMA NOT MATCH - " + e.getMessage());
        //String message = MessageUtil.build(MessageEnum.VALIDATION_ERROR, e.getMessage());
        //String message = e.getMessage();
        List<String> errorMessages = e.getAllMessages().stream()
                .map(message -> "Validation error: " + message)
                .collect(Collectors.toList());
        return new ResponseEntity<>(String.join(", ", errorMessages), HttpStatus.BAD_REQUEST);
    }





}
