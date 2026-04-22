package co.edu.unimagdalena.storelogistic.flota.infrastructure.exception;

import co.edu.unimagdalena.storelogistic.flota.domain.exceptions.TransicionEstadoInvalidaException;
import co.edu.unimagdalena.storelogistic.flota.domain.exceptions.VehiculoNotFoundException;
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
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TransicionEstadoInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleTransicionEstadoInvalida(
            TransicionEstadoInvalidaException ex, WebRequest request) {
        log.warn("Transición de estado inválida: {}", ex.getMessage());

        var errorResponse = ErrorResponse.builder()
                .codigo("TRANSICION_ESTADO_INVALIDA")
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(VehiculoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVehiculoNotFound(
            VehiculoNotFoundException ex, WebRequest request) {
        log.warn("Vehículo no encontrado: {}", ex.getMessage());

        var errorResponse = ErrorResponse.builder()
                .codigo("VEHICULO_NO_ENCONTRADO")
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Error de validación: {}", ex.getMessage());

        var errors = new HashMap<String, String>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        var errorResponse = ErrorResponse.builder()
                .codigo("VALIDACION_ERROR")
                .mensaje("Error en los parámetros de entrada")
                .detalles(errors)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        log.warn("Argumento inválido: {}", ex.getMessage());

        var errorResponse = ErrorResponse.builder()
                .codigo("ARGUMENTO_INVALIDO")
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, WebRequest request) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());

        var errorResponse = ErrorResponse.builder()
                .codigo("RECURSO_NO_ENCONTRADO")
                .mensaje("La ruta solicitada no existe")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        log.error("Error inesperado", ex);

        var errorResponse = ErrorResponse.builder()
                .codigo("ERROR_INTERNO")
                .mensaje("Error interno del servidor")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}