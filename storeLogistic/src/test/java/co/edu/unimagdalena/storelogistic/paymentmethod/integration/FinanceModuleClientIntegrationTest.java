package co.edu.unimagdalena.storelogistic.paymentmethod.integration;

import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.FinanceServiceUnavailableException;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.OrderNotFoundException;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.exceptions.PaymentMethodNotRegisteredException;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.models.OrderPaymentMethod;
import co.edu.unimagdalena.storelogistic.paymentmethod.domain.values.PaymentMethod;
import co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.client.FinanceModuleClient;
import co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.config.RetryConfig;
import co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.config.WebClientConfig;
import co.edu.unimagdalena.storelogistic.paymentmethod.infrastructure.mapper.PaymentMethodMapperImpl;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.*;

@SpringJUnitConfig(FinanceModuleClientIntegrationTest.TestConfig.class)
class FinanceModuleClientIntegrationTest {

    @TestConfiguration
    @Import({WebClientConfig.class, RetryConfig.class, PaymentMethodMapperImpl.class, FinanceModuleClient.class})
    static class TestConfig {}

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("finance.module.base-url", wireMock::baseUrl);
        registry.add("finance.module.retry.max-attempts", () -> 3);
        registry.add("finance.module.retry.initial-interval-ms", () -> 10);
        registry.add("finance.module.retry.multiplier", () -> 1.0);
        registry.add("finance.module.timeout-ms", () -> 2000);
    }

    @Autowired
    private FinanceModuleClient client;

    @Test
    @DisplayName("findByOrderId → 200 CONTRA_ENTREGA maps to domain")
    void findByOrderId_contraEntrega_returnsDomain() {
        wireMock.stubFor(get(urlEqualTo("/pedidos/1/forma-pago"))
                .willReturn(okJson("{\"id_pedido\":1,\"forma_pago\":\"CONTRA_ENTREGA\"}")));

        OrderPaymentMethod result = client.findByOrderId(1L);

        assertThat(result.orderId()).isEqualTo(1L);
        assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.CONTRA_ENTREGA);
    }

    @Test
    @DisplayName("findByOrderId → 200 CARTERA_COMERCIAL maps to domain")
    void findByOrderId_carteraComercial_returnsDomain() {
        wireMock.stubFor(get(urlEqualTo("/pedidos/2/forma-pago"))
                .willReturn(okJson("{\"id_pedido\":2,\"forma_pago\":\"CARTERA_COMERCIAL\"}")));

        OrderPaymentMethod result = client.findByOrderId(2L);

        assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.CARTERA_COMERCIAL);
    }

    @Test
    @DisplayName("findByOrderId → 404 throws OrderNotFoundException")
    void findByOrderId_404_throwsOrderNotFound() {
        wireMock.stubFor(get(urlEqualTo("/pedidos/999/forma-pago"))
                .willReturn(notFound()));

        assertThatThrownBy(() -> client.findByOrderId(999L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Pedido no encontrado");
    }

    @Test
    @DisplayName("findByOrderId → 422 throws PaymentMethodNotRegisteredException")
    void findByOrderId_422_throwsPaymentMethodNotRegistered() {
        wireMock.stubFor(get(urlEqualTo("/pedidos/5/forma-pago"))
                .willReturn(aResponse().withStatus(422)));

        assertThatThrownBy(() -> client.findByOrderId(5L))
                .isInstanceOf(PaymentMethodNotRegisteredException.class);
    }

    @Test
    @DisplayName("findByOrderId → 503 on all 3 attempts throws FinanceServiceUnavailableException")
    void findByOrderId_503AllAttempts_throwsServiceUnavailable() {
        wireMock.stubFor(get(urlEqualTo("/pedidos/10/forma-pago"))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> client.findByOrderId(10L))
                .isInstanceOf(FinanceServiceUnavailableException.class);

        wireMock.verify(3, getRequestedFor(urlEqualTo("/pedidos/10/forma-pago")));
    }

    @Test
    @DisplayName("findByOrderId → 503 on first attempt, 200 on second → returns result")
    void findByOrderId_503ThenSuccess_returnsResult() {
        wireMock.stubFor(get(urlEqualTo("/pedidos/20/forma-pago"))
                .inScenario("retry-success")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("second"));

        wireMock.stubFor(get(urlEqualTo("/pedidos/20/forma-pago"))
                .inScenario("retry-success")
                .whenScenarioStateIs("second")
                .willReturn(okJson("{\"id_pedido\":20,\"forma_pago\":\"CONTRA_ENTREGA\"}")));

        OrderPaymentMethod result = client.findByOrderId(20L);
        assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.CONTRA_ENTREGA);
        wireMock.verify(2, getRequestedFor(urlEqualTo("/pedidos/20/forma-pago")));
    }
}
