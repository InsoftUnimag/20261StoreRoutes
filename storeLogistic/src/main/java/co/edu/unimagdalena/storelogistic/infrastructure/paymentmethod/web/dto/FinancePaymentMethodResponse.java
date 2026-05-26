package co.edu.unimagdalena.storelogistic.infrastructure.paymentmethod.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FinancePaymentMethodResponse {

    @JsonProperty("idPedido")
    private Long idPedido;

    @JsonProperty("formaPago")
    private String formaPago;

    @JsonProperty("valorContraEntrega")
    private BigDecimal valorContraEntrega;
}
