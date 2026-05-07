package co.edu.unimagdalena.storelogistic.infrastructure.route.messaging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteDispatchedEvent {

    @JsonProperty("idRuta")
    private Long routeId;

    @JsonProperty("fechaDespacho")
    private LocalDateTime dispatchedAt;
}
