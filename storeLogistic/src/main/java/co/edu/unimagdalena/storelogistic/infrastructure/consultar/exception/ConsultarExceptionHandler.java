package co.edu.unimagdalena.storelogistic.infrastructure.consultar.exception;

import co.edu.unimagdalena.storelogistic.domain.consultar.exceptions.AccessDeniedException;
import co.edu.unimagdalena.storelogistic.domain.route.exceptions.InvalidStateTransitionException;
import co.edu.unimagdalena.storelogistic.domain.route.exceptions.StopNotFoundException;
import co.edu.unimagdalena.storelogistic.infrastructure.fleet.exception.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice(basePackages = "co.edu.unimagdalena.storelogistic.infrastructure.consultar")
public class ConsultarExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied attempt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildError("ACCESO_DENEGADO", "Acceso denegado"));
    }

    @ExceptionHandler(StopNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStopNotFound(StopNotFoundException ex) {
        log.warn("Stop not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildError("PARADA_NO_ENCONTRADA", ex.getMessage()));
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidStateTransitionException ex) {
        log.warn("Invalid stop state transition: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildError("TRANSICION_INVALIDA", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument in consultar: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError("ARGUMENTO_INVALIDO", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst().orElse("Datos inválidos");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError("VALIDACION_FALLIDA", mensaje));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError("PARAMETRO_REQUERIDO", "Parámetro requerido: " + ex.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError("PARAMETRO_INVALIDO", "Parámetro inválido: " + ex.getName()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError("VALIDACION_FALLIDA", "Valor inválido en parámetros de la solicitud"));
    }

    private ErrorResponse buildError(String code, String message) {
        return ErrorResponse.builder()
                .codigo(code)
                .mensaje(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
