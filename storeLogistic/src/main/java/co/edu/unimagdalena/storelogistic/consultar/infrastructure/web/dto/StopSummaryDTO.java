package co.edu.unimagdalena.storelogistic.consultar.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StopSummaryDTO {
    private Long idStop;
    private int sequence;
    private String deliveryAddress;
    private Long orderId;
    private String status;
}
