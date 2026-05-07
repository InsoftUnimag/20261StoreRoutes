package co.edu.unimagdalena.storelogistic.e2e.flow;

import co.edu.unimagdalena.storelogistic.e2e.E2ETestBase;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class CompleteFlowE2ETest extends E2ETestBase {

    @Autowired
    JdbcTemplate jdbc;

    private Long orderId;
    private Long routeId;

    @BeforeEach
    void insertarOrden() {
        orderId = jdbc.queryForObject(
                "INSERT INTO orders(logistic_weight, delivery_address) VALUES (100.0, 'Calle Test E2E #123') RETURNING id_order",
                Long.class);
    }

    @AfterEach
    void limpiar() {
        // stops se eliminan en cascada al borrar la ruta
        if (routeId != null) {
            jdbc.update("DELETE FROM routes WHERE id_route = ?", routeId);
        }
        if (orderId != null) {
            jdbc.update("DELETE FROM orders WHERE id_order = ?", orderId);
        }
        routeId = null;
        orderId = null;
    }

    // ── Flujo feliz ───────────────────────────────────────────────────────────

    @Test
    void flujo_completo_pedido_asignado_a_ruta_conductor_entrega() {
        // 1. Asignar pedido a ruta
        JsonPath asignacion = given()
                .contentType(ContentType.JSON)
                .body("{\"orderId\": " + orderId + "}")
                .when()
                    .post("/logistics/routes/assignments")
                .then()
                    .statusCode(anyOf(is(200), is(201)))
                    .body("routeId", notNullValue())
                    .body("vehicleId", notNullValue())
                    .body("stop.stopId", notNullValue())
                    .body("stop.stopStatus", equalTo("PENDING"))
                    .extract().jsonPath();

        routeId = asignacion.getLong("routeId");
        Long stopId = asignacion.getLong("stop.stopId");
        // Tras V12: id_transportista == id_vehiculo
        Long carrierId = asignacion.getLong("vehicleId");

        // 2. Conductor consulta sus paradas — debe aparecer PENDING
        given()
                .queryParam("carrierId", carrierId)
                .when()
                    .get("/logistics/routes/" + routeId + "/stops")
                .then()
                    .statusCode(200)
                    .body("stops", hasSize(greaterThanOrEqualTo(1)))
                    .body("stops.find { it.idStop == " + stopId + " }.status", equalTo("PENDING"));

        // 3. Conductor marca la parada como entregada
        given()
                .contentType(ContentType.JSON)
                .queryParam("carrierId", carrierId)
                .body("""
                      {
                        "resultado": "DELIVERED",
                        "fechaEntrega": "%s"
                      }
                      """.formatted(LocalDate.now()))
                .when()
                    .patch("/logistics/routes/" + routeId + "/stops/" + stopId)
                .then()
                    .statusCode(200)
                    .body("stopId", equalTo(stopId.intValue()))
                    .body("status", equalTo("DELIVERED"))
                    .body("fechaEntrega", notNullValue());

        // 4. Verificar que la parada queda DELIVERED en la lista
        given()
                .queryParam("carrierId", carrierId)
                .when()
                    .get("/logistics/routes/" + routeId + "/stops")
                .then()
                    .statusCode(200)
                    .body("stops.find { it.idStop == " + stopId + " }.status", equalTo("DELIVERED"));
    }

    @Test
    void flujo_completo_pedido_rechazado_por_conductor() {
        JsonPath asignacion = given()
                .contentType(ContentType.JSON)
                .body("{\"orderId\": " + orderId + "}")
                .when()
                    .post("/logistics/routes/assignments")
                .then()
                    .statusCode(anyOf(is(200), is(201)))
                    .extract().jsonPath();

        routeId = asignacion.getLong("routeId");
        Long stopId = asignacion.getLong("stop.stopId");
        Long carrierId = asignacion.getLong("vehicleId");

        given()
                .contentType(ContentType.JSON)
                .queryParam("carrierId", carrierId)
                .body("{\"resultado\": \"REJECTED\"}")
                .when()
                    .patch("/logistics/routes/" + routeId + "/stops/" + stopId)
                .then()
                    .statusCode(200)
                    .body("status", equalTo("REJECTED"))
                    .body("fechaEntrega", nullValue());
    }

    // ── Casos de error ────────────────────────────────────────────────────────

    @Test
    void marcar_parada_ya_entregada_retorna_409() {
        JsonPath asignacion = given()
                .contentType(ContentType.JSON)
                .body("{\"orderId\": " + orderId + "}")
                .when()
                    .post("/logistics/routes/assignments")
                .then()
                    .statusCode(anyOf(is(200), is(201)))
                    .extract().jsonPath();

        routeId = asignacion.getLong("routeId");
        Long stopId = asignacion.getLong("stop.stopId");
        Long carrierId = asignacion.getLong("vehicleId");

        String deliverBody = "{\"resultado\": \"DELIVERED\", \"fechaEntrega\": \"%s\"}".formatted(LocalDate.now());

        // Primera entrega — OK
        given()
                .contentType(ContentType.JSON)
                .queryParam("carrierId", carrierId)
                .body(deliverBody)
                .when()
                    .patch("/logistics/routes/" + routeId + "/stops/" + stopId)
                .then()
                    .statusCode(200);

        // Segunda entrega — 409 transición inválida
        given()
                .contentType(ContentType.JSON)
                .queryParam("carrierId", carrierId)
                .body(deliverBody)
                .when()
                    .patch("/logistics/routes/" + routeId + "/stops/" + stopId)
                .then()
                    .statusCode(409);
    }

    @Test
    void conductor_sin_acceso_a_la_ruta_retorna_403() {
        JsonPath asignacion = given()
                .contentType(ContentType.JSON)
                .body("{\"orderId\": " + orderId + "}")
                .when()
                    .post("/logistics/routes/assignments")
                .then()
                    .statusCode(anyOf(is(200), is(201)))
                    .extract().jsonPath();

        routeId = asignacion.getLong("routeId");
        Long stopId = asignacion.getLong("stop.stopId");
        Long wrongCarrierId = 99999L;

        given()
                .contentType(ContentType.JSON)
                .queryParam("carrierId", wrongCarrierId)
                .body("{\"resultado\": \"DELIVERED\", \"fechaEntrega\": \"%s\"}".formatted(LocalDate.now()))
                .when()
                    .patch("/logistics/routes/" + routeId + "/stops/" + stopId)
                .then()
                    .statusCode(403);
    }

    @Test
    void asignar_pedido_inexistente_retorna_404() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"orderId\": 99999}")
                .when()
                    .post("/logistics/routes/assignments")
                .then()
                    .statusCode(404);
    }
}
