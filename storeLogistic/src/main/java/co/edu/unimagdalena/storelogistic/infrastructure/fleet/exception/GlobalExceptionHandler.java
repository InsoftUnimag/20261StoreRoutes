package co.edu.unimagdalena.storelogistic.infrastructure.fleet.exception;

import co.edu.unimagdalena.storelogistic.domain.fleet.exceptions.InvalidStateTransitionException;
import co.edu.unimagdalena.storelogistic.domain.fleet.exceptions.InvalidTransporterException;
import co.edu.unimagdalena.storelogistic.domain.fleet.exceptions.TransporterNotAvailableException;
import co.edu.unimagdalena.storelogistic.domain.fleet.exceptions.VehicleNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;

@Slf4j
@RestControllerAdvice(basePackages = "co.edu.unimagdalena.storelogistic.infrastructure.fleet")
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStateTransition(
            InvalidStateTransitionException ex, WebRequest request) {
        log.warn("Invalid state transition: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildError("INVALID_STATE_TRANSITION", ex.getMessage()));
    }

    @ExceptionHandler(VehicleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVehicleNotFound(
            VehicleNotFoundException ex, WebRequest request) {
        log.warn("Vehicle not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildError("VEHICULO_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(TransporterNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleTransporterNotAvailable(
            TransporterNotAvailableException ex, WebRequest request) {
        log.warn("No transporter available: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildError("TRANSPORTISTA_NO_DISPONIBLE", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTransporterException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransporter(
            InvalidTransporterException ex, WebRequest request) {
        log.warn("Invalid transporter: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError("TRANSPORTISTA_INVALIDO", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());

        var errors = new HashMap<String, String>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        var errorResponse = ErrorResponse.builder()
                .codigo("VALIDATION_ERROR")
                .mensaje("Invalid input parameters")
                .detalles(errors)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError("INVALID_ARGUMENT", ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildError("RESOURCE_NOT_FOUND", "The requested path does not exist"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError("INTERNAL_ERROR", "Internal server error"));
    }

    private ErrorResponse buildError(String code, String message) {
        return ErrorResponse.builder()
                .codigo(code)
                .mensaje(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}