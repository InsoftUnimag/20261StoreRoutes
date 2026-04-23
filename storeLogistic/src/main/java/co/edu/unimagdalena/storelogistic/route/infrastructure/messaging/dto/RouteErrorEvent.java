package co.edu.unimagdalena.storelogistic.route.infrastructure.messaging.dto;

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
