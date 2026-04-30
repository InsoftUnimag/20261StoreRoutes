package co.edu.unimagdalena.storelogistic.infrastructure.route.web.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class StopResponse {
    private Long stopId;
    private Long orderId;
    private int sequence;
    private String deliveryAddress;
    private String stopStatus;
    private LocalDate deliveryDate;
}
