package co.edu.unimagdalena.storelogistic.infrastructure.fleet.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleFilterRequest {
    private String category;
    private String status;
    private Double minCapacity;
    private Double maxCapacity;
}