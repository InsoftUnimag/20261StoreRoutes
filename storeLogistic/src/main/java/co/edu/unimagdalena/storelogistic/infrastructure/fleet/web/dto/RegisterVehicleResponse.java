package co.edu.unimagdalena.storelogistic.infrastructure.fleet.web.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterVehicleResponse {
    private Long vehicleId;
    private String status;
    private LocalDateTime createdAt;
}