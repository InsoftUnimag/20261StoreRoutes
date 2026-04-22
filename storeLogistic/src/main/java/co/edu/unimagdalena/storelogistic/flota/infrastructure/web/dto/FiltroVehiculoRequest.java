package co.edu.unimagdalena.storelogistic.flota.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FiltroVehiculoRequest {
    private String categoria;
    private String estado;
    private Double capacidadMin;
    private Double capacidadMax;
}

