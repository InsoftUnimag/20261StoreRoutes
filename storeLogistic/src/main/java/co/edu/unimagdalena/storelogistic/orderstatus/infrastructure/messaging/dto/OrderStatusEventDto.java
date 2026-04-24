package co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusEventDto {

    private Long id_pedido;
    private String estado_final;
    private Integer tasa_efectividad;
    private Long id_transportista;
}
