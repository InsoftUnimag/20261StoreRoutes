package co.edu.unimagdalena.storelogistic.fleet.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListVehiclesResponse {
    private Integer total;
    private List<VehicleDTO> data;
}