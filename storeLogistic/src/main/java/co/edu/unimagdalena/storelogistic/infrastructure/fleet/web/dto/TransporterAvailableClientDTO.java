package co.edu.unimagdalena.storelogistic.infrastructure.fleet.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransporterAvailableClientDTO {
    private Long idTransportista;
    private String estado;
}