package co.edu.unimagdalena.storelogistic.infrastructure.consultar.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStopStatusResponse {
    private Long stopId;
    private Long orderId;
    private int sequence;
    private String deliveryAddress;
    private String status;
    private LocalDate fechaEntrega;
}
