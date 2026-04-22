package co.edu.unimagdalena.storelogistic.flota.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehiculoDTO {
    private Long idVehiculo;
    private String idTransportista;
    private String categoria;
    private BigDecimal capacidadCarga;
    private String estado;
    private BigDecimal pesoActual;
    private BigDecimal porcentajeOcupacion;
    private LocalDateTime createdAt;
}

