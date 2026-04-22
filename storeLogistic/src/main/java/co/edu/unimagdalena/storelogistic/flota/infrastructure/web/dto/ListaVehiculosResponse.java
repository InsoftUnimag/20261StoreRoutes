package co.edu.unimagdalena.storelogistic.flota.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListaVehiculosResponse {
    private Integer total;
    private List<VehiculoDTO> data;
}

