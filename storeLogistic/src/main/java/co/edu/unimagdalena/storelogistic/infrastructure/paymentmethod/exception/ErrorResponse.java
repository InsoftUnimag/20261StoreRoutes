package co.edu.unimagdalena.storelogistic.infrastructure.paymentmethod.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponse {
    private String codigo;
    private String mensaje;
    private LocalDateTime timestamp;
}
