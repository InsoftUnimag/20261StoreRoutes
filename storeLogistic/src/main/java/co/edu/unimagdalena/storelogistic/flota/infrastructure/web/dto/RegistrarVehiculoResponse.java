package co.edu.unimagdalena.storelogistic.flota.infrastructure.web.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrarVehiculoResponse {
    private Long idVehiculo;
    private String estado;
    private LocalDateTime createdAt;
}

