package co.edu.unimagdalena.storelogistic.infrastructure.fleet.web.dto;

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
public class VehicleDTO {
    private Long vehicleId;
    private Long transporterId;
    private String category;
    private BigDecimal loadCapacity;
    private String status;
    private LocalDateTime createdAt;
}