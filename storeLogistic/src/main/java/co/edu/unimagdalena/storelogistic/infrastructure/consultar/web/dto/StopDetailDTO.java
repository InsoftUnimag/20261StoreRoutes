package co.edu.unimagdalena.storelogistic.infrastructure.consultar.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StopDetailDTO {
    private Long idStop;
    private int sequence;
    private String deliveryAddress;
    private Long orderId;
    private String customerContact;
    private String paymentMethod;
    private BigDecimal totalACobrar;
    private String status;
}
