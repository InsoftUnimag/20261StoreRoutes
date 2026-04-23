package co.edu.unimagdalena.storelogistic.fleet.infrastructure.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private String codigo;
    private String mensaje;
    private Map<String, String> detalles;
    private LocalDateTime timestamp;
}