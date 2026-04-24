package co.edu.unimagdalena.storelogistic.orderstatus.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class UpdateOrderStatusApiContractTest {

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @DynamicPropertySource
    static void configureRabbitMQ(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    RestClient restClient;
    Long carrierId;
    Long orderId;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1")
                .build();

        jdbcTemplate.update("DELETE FROM order_status_audit");
        jdbcTemplate.update("DELETE FROM order_alerts");
        jdbcTemplate.update("DELETE FROM stops");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM routes");
        jdbcTemplate.update("DELETE FROM vehiculos");
        jdbcTemplate.update("DELETE FROM categorias");

        jdbcTemplate.update(
                "INSERT INTO categorias (tipo, capacidad_maxima_kg) VALUES (?, ?)", "CAMION", 5000);
        Long categoriaId = jdbcTemplate.queryForObject(
                "SELECT id_categoria FROM categorias WHERE tipo = 'CAMION'", Long.class);

        carrierId = 50L;
        jdbcTemplate.update(
                "INSERT INTO vehiculos (id_categoria, capacidad_carga, estado, id_transportista, peso_actual) " +
                        "VALUES (?, ?, ?, ?, ?)",
                categoriaId, 5000, "DISPONIBLE", carrierId, 0);

        jdbcTemplate.update("INSERT INTO orders (logistic_weight, delivery_address) VALUES (100, 'Calle 10 #20-30')");
        orderId = jdbcTemplate.queryForObject(
                "SELECT id_order FROM orders ORDER BY id_order DESC LIMIT 1", Long.class);
    }

    @Test
    @DisplayName("SC1: PUT Entregado Completo → 200 with tasaEfectividad=100")
    void put_entregadoCompleto_returns200WithRate100() {
        ResponseEntity<String> response = restClient.put()
                .uri("/logistics/orders/{id}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"idTransportista\":" + carrierId + ",\"estadoFinal\":\"Entregado Completo\"}")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"idPedido\":" + orderId);
        assertThat(response.getBody()).contains("\"estadoFinal\":\"Entregado Completo\"");
        assertThat(response.getBody()).contains("\"tasaEfectividad\":100");
        assertThat(response.getBody()).contains("\"idTransportista\":" + carrierId);
    }

    @Test
    @DisplayName("SC1: All five estados have correct tasaEfectividad")
    void put_allFiveEstados_haveCorrectRates() {
        record StatusRate(String status, int rate) {}
        var cases = new StatusRate[]{
                new StatusRate("Entregado Completo", 100),
                new StatusRate("Rechazo Parcial", 80),
                new StatusRate("No Entregado", 0),
                new StatusRate("Devolución (Error Empresa)", 0),
                new StatusRate("Faltante de Inventario", -100)
        };

        for (StatusRate sc : cases) {
            jdbcTemplate.update("INSERT INTO orders (logistic_weight, delivery_address) VALUES (100, 'Dir')");
            Long oid = jdbcTemplate.queryForObject(
                    "SELECT id_order FROM orders ORDER BY id_order DESC LIMIT 1", Long.class);

            ResponseEntity<String> response = restClient.put()
                    .uri("/logistics/orders/{id}/status", oid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"idTransportista\":" + carrierId + ",\"estadoFinal\":\"" + sc.status() + "\"}")
                    .retrieve()
                    .toEntity(String.class);

            assertThat(response.getStatusCode())
                    .as("status for %s", sc.status()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .as("rate for %s", sc.status())
                    .contains("\"tasaEfectividad\":" + sc.rate());
        }
    }

    @Test
    @DisplayName("SC2: Second PUT on same order → 200 with corrected state, audit record created")
    void put_secondPutOnSameOrder_returnsUpdatedStateAndCreatesAudit() {
        restClient.put()
                .uri("/logistics/orders/{id}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"idTransportista\":" + carrierId + ",\"estadoFinal\":\"No Entregado\"}")
                .retrieve().toEntity(String.class);

        ResponseEntity<String> response = restClient.put()
                .uri("/logistics/orders/{id}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"idTransportista\":" + carrierId + ",\"estadoFinal\":\"Entregado Completo\"}")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"estadoFinal\":\"Entregado Completo\"");
        assertThat(response.getBody()).contains("\"tasaEfectividad\":100");

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_status_audit WHERE id_pedido = ?",
                Integer.class, orderId);
        assertThat(auditCount).isEqualTo(1);

        String estadoAnterior = jdbcTemplate.queryForObject(
                "SELECT estado_anterior FROM order_status_audit WHERE id_pedido = ?",
                String.class, orderId);
        assertThat(estadoAnterior).isEqualTo("No Entregado");
    }

    @Test
    @DisplayName("EC: estadoFinal inválido → 422 ESTADO_FINAL_INVALIDO")
    void put_invalidEstadoFinal_returns422() {
        ResponseEntity<String> response = restClient.put()
                .uri("/logistics/orders/{id}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"idTransportista\":" + carrierId + ",\"estadoFinal\":\"EstadoInventado\"}")
                .retrieve()
                .onStatus(s -> s.value() == 422, (req, res) -> {})
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).contains("ESTADO_FINAL_INVALIDO");
    }

    @Test
    @DisplayName("EC: pedido inexistente → 404 PEDIDO_NO_ENCONTRADO")
    void put_orderDoesNotExist_returns404() {
        ResponseEntity<String> response = restClient.put()
                .uri("/logistics/orders/{id}/status", 99999L)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"idTransportista\":" + carrierId + ",\"estadoFinal\":\"Entregado Completo\"}")
                .retrieve()
                .onStatus(s -> s.value() == 404, (req, res) -> {})
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("PEDIDO_NO_ENCONTRADO");
    }

    @Test
    @DisplayName("EC: transportista inexistente → 404 TRANSPORTISTA_NO_ENCONTRADO")
    void put_carrierDoesNotExist_returns404() {
        ResponseEntity<String> response = restClient.put()
                .uri("/logistics/orders/{id}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"idTransportista\":99999,\"estadoFinal\":\"Entregado Completo\"}")
                .retrieve()
                .onStatus(s -> s.value() == 404, (req, res) -> {})
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("TRANSPORTISTA_NO_ENCONTRADO");
    }

    @Test
    @DisplayName("EC: idTransportista nulo → 400 VALIDACION_FALLIDA")
    void put_nullCarrierId_returns400() {
        ResponseEntity<String> response = restClient.put()
                .uri("/logistics/orders/{id}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"estadoFinal\":\"Entregado Completo\"}")
                .retrieve()
                .onStatus(s -> s.value() == 400, (req, res) -> {})
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("VALIDACION_FALLIDA");
    }

    @Test
    @DisplayName("SC1: NO_ENTREGADO → alert is created in DB")
    void put_noEntregado_alertIsPersistedInDb() {
        restClient.put()
                .uri("/logistics/orders/{id}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"idTransportista\":" + carrierId + ",\"estadoFinal\":\"No Entregado\"}")
                .retrieve().toEntity(String.class);

        Integer alertCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_alerts WHERE id_pedido = ?",
                Integer.class, orderId);
        assertThat(alertCount).isEqualTo(1);

        String tipo = jdbcTemplate.queryForObject(
                "SELECT tipo FROM order_alerts WHERE id_pedido = ?", String.class, orderId);
        assertThat(tipo).isEqualTo("NO_ENTREGADO");
    }
}
