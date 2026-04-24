package co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.exception;

import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.FinanceServiceUnavailableException;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.OrderNotFoundException;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.PaymentMethodNotRegisteredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice(basePackages = "co.edu.unimagdalena.storelogistic.paymentmethod")
public class PaymentMethodExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        log.warn("Order not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(build("PEDIDO_NO_ENCONTRADO", ex.getMessage()));
    }

    @ExceptionHandler(PaymentMethodNotRegisteredException.class)
    public ResponseEntity<ErrorResponse> handlePaymentMethodNotRegistered(PaymentMethodNotRegisteredException ex) {
        log.warn("Payment method not registered: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(build("FORMA_PAGO_NO_REGISTRADA", ex.getMessage()));
    }

    @ExceptionHandler(FinanceServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(FinanceServiceUnavailableException ex) {
        log.error("Finance service unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(build("SERVICIO_FINANCIERO_NO_DISPONIBLE", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Invalid path variable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build("VALIDACION_FALLIDA", "El parámetro id_pedido debe ser un número entero positivo válido"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build("VALIDACION_FALLIDA", ex.getMessage()));
    }

    private ErrorResponse build(String codigo, String mensaje) {
        return ErrorResponse.builder()
                .codigo(codigo)
                .mensaje(mensaje)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
