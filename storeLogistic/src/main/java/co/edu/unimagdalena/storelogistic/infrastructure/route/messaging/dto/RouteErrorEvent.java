package co.edu.unimagdalena.storelogistic.infrastructure.route.messaging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteErrorEvent {

    @JsonProperty("codigo")
    private String code;

    @JsonProperty("mensaje")
    private String message;
}
