package co.edu.unimagdalena.storelogistic.consultar.integration;

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
class QueryStopsApiContractTest {

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
    Long routeId;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1")
                .build();

        jdbcTemplate.update("DELETE FROM stops");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM routes");
        jdbcTemplate.update("DELETE FROM carrier");

        jdbcTemplate.update(
                "INSERT INTO carrier (name, status, email) VALUES (?, ?, ?)",
                "Juan Eguis", "ACTIVE", "juan@logistica.com"
        );
        carrierId = jdbcTemplate.queryForObject(
                "SELECT id_carrier FROM carrier WHERE email = ?", Long.class, "juan@logistica.com");

        jdbcTemplate.update(
                "INSERT INTO routes (total_capacity_kg, accumulated_weight_kg, status, dispatch_date, id_carrier) " +
                        "VALUES (1000, 0, 'AVAILABLE', CURRENT_DATE, ?)",
                carrierId
        );
        routeId = jdbcTemplate.queryForObject(
                "SELECT id_route FROM routes WHERE id_carrier = ? ORDER BY id_route DESC LIMIT 1",
                Long.class, carrierId);

        jdbcTemplate.update("INSERT INTO orders (logistic_weight, delivery_address) VALUES (100, 'Calle 10 #20-30')");
        Long orderId1 = jdbcTemplate.queryForObject(
                "SELECT id_order FROM orders ORDER BY id_order DESC LIMIT 1", Long.class);

        jdbcTemplate.update("INSERT INTO orders (logistic_weight, delivery_address) VALUES (150, 'Carrera 5 #15-20')");
        Long orderId2 = jdbcTemplate.queryForObject(
                "SELECT id_order FROM orders ORDER BY id_order DESC LIMIT 1", Long.class);

        jdbcTemplate.update("INSERT INTO orders (logistic_weight, delivery_address) VALUES (200, 'Avenida 19 #30-40')");
        Long orderId3 = jdbcTemplate.queryForObject(
                "SELECT id_order FROM orders ORDER BY id_order DESC LIMIT 1", Long.class);

        jdbcTemplate.update(
                "INSERT INTO stops (id_route, id_order, sequence, delivery_address, status, customer_contact) " +
                        "VALUES (?, ?, 1, 'Calle 10 #20-30', 'PENDING', '3001234567')",
                routeId, orderId1);
        jdbcTemplate.update(
                "INSERT INTO stops (id_route, id_order, sequence, delivery_address, status, customer_contact) " +
                        "VALUES (?, ?, 2, 'Carrera 5 #15-20', 'PENDING', '3009876543')",
                routeId, orderId2);
        jdbcTemplate.update(
                "INSERT INTO stops (id_route, id_order, sequence, delivery_address, status, customer_contact) " +
                        "VALUES (?, ?, 3, 'Avenida 19 #30-40', 'PENDING', '3005551234')",
                routeId, orderId3);
    }

    @Test
    @DisplayName("SC1: GET stops of assigned route → 200 with ordered stop list")
    void getStops_routeAssignedToCarrier_returns200WithOrderedStops() {
        ResponseEntity<String> response = restClient.get()
                .uri("/logistics/routes/{routeId}/stops?carrierId={carrierId}", routeId, carrierId)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"routeId\"");
        assertThat(response.getBody()).contains("\"totalStops\":3");
        assertThat(response.getBody()).contains("Calle 10");
        assertThat(response.getBody()).contains("Carrera 5");
        assertThat(response.getBody()).contains("Avenida 19");
    }

    @Test
    @DisplayName("SC1: Stops are returned ordered by sequence ASC")
    void getStops_returnsStopsOrderedBySequence() {
        ResponseEntity<String> response = restClient.get()
                .uri("/logistics/routes/{routeId}/stops?carrierId={carrierId}", routeId, carrierId)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        int pos1 = body.indexOf("Calle 10");
        int pos2 = body.indexOf("Carrera 5");
        int pos3 = body.indexOf("Avenida 19");
        assertThat(pos1).isLessThan(pos2);
        assertThat(pos2).isLessThan(pos3);
    }

    @Test
    @DisplayName("EC: GET stops with other carrier's routeId → 403 Acceso denegado")
    void getStops_routeNotAssignedToCarrier_returns403() {
        jdbcTemplate.update(
                "INSERT INTO carrier (name, status, email) VALUES (?, ?, ?)",
                "Otro Transportista", "ACTIVE", "otro@logistica.com"
        );
        Long otherCarrierId = jdbcTemplate.queryForObject(
                "SELECT id_carrier FROM carrier WHERE email = ?", Long.class, "otro@logistica.com");

        ResponseEntity<String> response = restClient.get()
                .uri("/logistics/routes/{routeId}/stops?carrierId={carrierId}", routeId, otherCarrierId)
                .retrieve()
                .onStatus(status -> status.value() == 403, (req, res) -> {})
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("ACCESO_DENEGADO");
        assertThat(response.getBody()).contains("Acceso denegado");
    }

    @Test
    @DisplayName("EC: GET stops with non-existent routeId → 403 (same as unauthorized)")
    void getStops_routeDoesNotExist_returns403() {
        ResponseEntity<String> response = restClient.get()
                .uri("/logistics/routes/{routeId}/stops?carrierId={carrierId}", 99999L, carrierId)
                .retrieve()
                .onStatus(status -> status.value() == 403, (req, res) -> {})
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("Acceso denegado");
    }

    @Test
    @DisplayName("EC: Route with zero stops → 200 with empty list")
    void getStops_routeWithNoStops_returns200WithEmptyList() {
        jdbcTemplate.update("DELETE FROM stops WHERE id_route = ?", routeId);

        ResponseEntity<String> response = restClient.get()
                .uri("/logistics/routes/{routeId}/stops?carrierId={carrierId}", routeId, carrierId)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"totalStops\":0");
        assertThat(response.getBody()).contains("\"stops\":[]");
    }

    @Test
    @DisplayName("EC: Missing carrierId → 400")
    void getStops_missingCarrierId_returns400() {
        ResponseEntity<String> response = restClient.get()
                .uri("/logistics/routes/{routeId}/stops", routeId)
                .retrieve()
                .onStatus(status -> status.value() == 400, (req, res) -> {})
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}