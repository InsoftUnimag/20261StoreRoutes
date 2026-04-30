package co.edu.unimagdalena.storelogistic.infrastructure.route.messaging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteAssignedEvent {

    @JsonProperty("idPedido")
    private Long orderId;

    @JsonProperty("idRuta")
    private Long routeId;

    @JsonProperty("fechaDespacho")
    private LocalDate dispatchDate;
}
