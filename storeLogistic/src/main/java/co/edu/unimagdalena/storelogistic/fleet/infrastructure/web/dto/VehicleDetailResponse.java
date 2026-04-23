package co.edu.unimagdalena.storelogistic.fleet.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleDetailResponse {
    private Long vehicleId;
    private Long transporterId;
    private String category;
    private BigDecimal loadCapacity;
    private String status;
    private BigDecimal currentWeight;
    private BigDecimal occupancyPercentage;
    private LocalDateTime createdAt;
}