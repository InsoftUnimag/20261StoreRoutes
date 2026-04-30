package co.edu.unimagdalena.storelogistic.infrastructure.paymentmethod.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponse {
    private Long orderId;
    private String paymentMethod;
    private BigDecimal totalPedido;
}
