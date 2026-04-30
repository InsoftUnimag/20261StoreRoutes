package co.edu.unimagdalena.storelogistic.infrastructure.route.web.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class AssignOrderResponse {
    private Long routeId;
    private Long vehicleId;
    private BigDecimal accumulatedWeightKg;
    private BigDecimal totalCapacityKg;
    private BigDecimal occupancyPercentage;
    private String routeStatus;
    private LocalDate dispatchDate;
    private StopResponse stop;
}
