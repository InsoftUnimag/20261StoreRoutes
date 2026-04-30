package co.edu.unimagdalena.storelogistic.infrastructure.consultar.exception;

import co.edu.unimagdalena.storelogistic.domain.consultar.exceptions.AccessDeniedException;
import co.edu.unimagdalena.storelogistic.infrastructure.fleet.exception.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
