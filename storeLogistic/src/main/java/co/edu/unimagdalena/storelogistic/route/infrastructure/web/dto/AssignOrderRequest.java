package co.edu.unimagdalena.storelogistic.route.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AssignOrderRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;
}
