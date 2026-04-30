package co.edu.unimagdalena.storelogistic.infrastructure.route.exception;

import co.edu.unimagdalena.storelogistic.infrastructure.fleet.exception.ErrorResponse;
import co.edu.unimagdalena.storelogistic.domain.route.exceptions.CapacityExceededException;
import co.edu.unimagdalena.storelogistic.domain.route.exceptions.InvalidStateTransitionException;
import co.edu.unimagdalena.storelogistic.domain.route.exceptions.OrderNotFoundException;
import co.edu.unimagdalena.storelogistic.domain.route.exceptions.RouteNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;

@Slf4j
@RestControllerAdvice(basePackages = "co.edu.unimagdalena.storelogistic.infrastructure.route")
public class RouteExceptionHandler {

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidStateTransitionException ex, WebRequest request) {
        log.warn("Invalid state transition: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildError("INVALID_STATE_TRANSITION", ex.getMessage()));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex, WebRequest request) {
        log.warn("Order not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildError("ORDER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(RouteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRouteNotFound(RouteNotFoundException ex, WebRequest request) {
        log.warn("Route not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildError("ROUTE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(CapacityExceededException.class)
    public ResponseEntity<ErrorResponse> handleCapacityExceeded(CapacityExceededException ex, WebRequest request) {
        log.warn("Capacity exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(buildError("CAPACITY_EXCEEDED", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation error in route module: {}", ex.getMessage());
        var errors = new HashMap<String, String>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });
        var response = ErrorResponse.builder()
                .codigo("VALIDATION_ERROR")
                .mensaje("Invalid request parameters")
                .detalles(errors)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument in route module: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError("INVALID_ARGUMENT", ex.getMessage()));
    }

    private ErrorResponse buildError(String code, String message) {
        return ErrorResponse.builder()
                .codigo(code)
                .mensaje(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
