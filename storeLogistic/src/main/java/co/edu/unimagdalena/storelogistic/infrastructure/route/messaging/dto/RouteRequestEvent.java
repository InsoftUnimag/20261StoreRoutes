package co.edu.unimagdalena.storelogistic.infrastructure.route.messaging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteRequestEvent {

    @JsonProperty("idPedido")
    private Long orderId;

    @JsonProperty("idCliente")
    private Long clientId;

    @JsonProperty("pesoLogistico")
    private BigDecimal logisticWeight;

    @JsonProperty("direccionEntrega")
    private String deliveryAddress;
}
