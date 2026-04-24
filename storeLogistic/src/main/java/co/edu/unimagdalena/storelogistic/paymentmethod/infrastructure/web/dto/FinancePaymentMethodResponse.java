package co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FinancePaymentMethodResponse {

    @JsonProperty("id_pedido")
    private Long idPedido;

    @JsonProperty("forma_pago")
    private String formaPago;
}
