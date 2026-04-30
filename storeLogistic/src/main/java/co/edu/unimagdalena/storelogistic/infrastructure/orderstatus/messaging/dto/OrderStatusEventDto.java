package co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.messaging.dto;

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
    private Integer tasa_efectividad;
    private Long id_transportista;
}
