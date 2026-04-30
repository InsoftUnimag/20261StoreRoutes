package co.edu.unimagdalena.storelogistic.infrastructure.paymentmethod.web.controller;

import co.edu.unimagdalena.storelogistic.domain.paymentmethod.ports.in.ConsultPaymentMethodUseCase;
import co.edu.unimagdalena.storelogistic.infrastructure.paymentmethod.mapper.PaymentMethodMapper;
import co.edu.unimagdalena.storelogistic.infrastructure.paymentmethod.web.dto.PaymentMethodResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
@Tag(name = "Payment Method", description = "Consulta el método de pago de un pedido al Módulo Financiero")
public class PaymentMethodController {

    private final ConsultPaymentMethodUseCase consultPaymentMethodUseCase;
    private final PaymentMethodMapper mapper;

    @GetMapping("/{id_pedido}/forma-pago")
    @Operation(summary = "Consultar método de pago de un pedido",
               description = "Consulta de forma síncrona el método de pago al Módulo Financiero. Aplica reintentos con backoff exponencial.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "id_pedido inválido"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado"),
            @ApiResponse(responseCode = "422", description = "Cliente sin forma de pago registrada"),
            @ApiResponse(responseCode = "503", description = "Módulo Financiero no disponible")
    })
    public ResponseEntity<PaymentMethodResponse> consultPaymentMethod(
            @PathVariable("id_pedido") Long orderId) {
        log.info("GET /pedidos/{}/forma-pago", orderId);
        return ResponseEntity.ok(mapper.toResponse(consultPaymentMethodUseCase.consult(orderId)));
    }
}
