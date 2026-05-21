package co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderHistoryItemResponse {

    private Long idPedido;
    private String estadoFinal;
    private Integer tasaEfectividad;
    private Long idTransportista;
    private LocalDateTime fechaActualizacion;
}
