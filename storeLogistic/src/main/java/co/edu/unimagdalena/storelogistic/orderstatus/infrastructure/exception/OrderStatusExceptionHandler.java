package co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.exception;

import co.edu.unimagdalena.storelogistic.fleet.infrastructure.exception.ErrorResponse;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.exceptions.CarrierNotFoundException;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.exceptions.InvalidFinalStatusException;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.exceptions.OrderNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(basePackages = "co.edu.unimagdalena.storelogistic.orderstatus")
public class OrderStatusExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        log.warn("Order not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildError("PEDIDO_NO_ENCONTRADO", ex.getMessage()));
    }

    @ExceptionHandler(CarrierNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCarrierNotFound(CarrierNotFoundException ex) {
        log.warn("Carrier not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildError("TRANSPORTISTA_NO_ENCONTRADO", ex.getMessage()));
    }

    @ExceptionHandler(InvalidFinalStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatus(InvalidFinalStatusException ex) {
        log.warn("Invalid final status: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(buildError("ESTADO_FINAL_INVALIDO", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError("VALIDACION_FALLIDA", mensaje));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError("PARAMETRO_INVALIDO", "Parámetro inválido: " + ex.getName()));
    }

    private ErrorResponse buildError(String code, String message) {
        return ErrorResponse.builder()
                .codigo(code)
                .mensaje(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
