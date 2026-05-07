package co.edu.unimagdalena.storelogistic.e2e.fleet;

import co.edu.unimagdalena.storelogistic.e2e.E2ETestBase;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class FleetE2ETest extends E2ETestBase {

    @Test
    void listar_vehiculos_retorna_200_con_lista_no_vacia() {
        given()
            .when()
                .get("/vehicles")
            .then()
                .statusCode(200)
                .body("total", greaterThanOrEqualTo(9))
                .body("data", hasSize(greaterThanOrEqualTo(9)));
    }

    @Test
    void registrar_vehiculo_retorna_201_con_estado_en_mantenimiento() {
        String body = """
                {
                  "category": "CAMION_SENCILLO",
                  "loadCapacity": 3000.0,
                  "transporterId": 999
                }
                """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
                .post("/vehicles")
            .then()
                .statusCode(201)
                .body("vehicleId", notNullValue())
                .body("status", equalTo("EN_MANTENIMIENTO"));
    }

    @Test
    void obtener_vehiculo_por_id_existente_retorna_200_con_detalle() {
        given()
            .when()
                .get("/vehicles/1")
            .then()
                .statusCode(200)
                .body("vehicleId", equalTo(1))
                .body("status", notNullValue())
                .body("category", notNullValue())
                .body("loadCapacity", notNullValue());
    }

    @Test
    void obtener_vehiculo_inexistente_retorna_404() {
        given()
            .when()
                .get("/vehicles/99999")
            .then()
                .statusCode(404);
    }

    @Test
    void cambiar_estado_de_mantenimiento_a_disponible_retorna_200() {
        // Crea un vehículo en EN_MANTENIMIENTO para no depender del seed
        String registerBody = """
                {
                  "category": "CAMIONETA_URBANA",
                  "loadCapacity": 1000.0,
                  "transporterId": 998
                }
                """;

        Long vehicleId = given()
            .contentType(ContentType.JSON)
            .body(registerBody)
            .when()
                .post("/vehicles")
            .then()
                .statusCode(201)
                .extract()
                .jsonPath().getLong("vehicleId");

        String patchBody = """
                {
                  "newStatus": "DISPONIBLE"
                }
                """;

        given()
            .contentType(ContentType.JSON)
            .body(patchBody)
            .when()
                .patch("/vehicles/" + vehicleId + "/status")
            .then()
                .statusCode(200)
                .body("status", equalTo("DISPONIBLE"));
    }

    @Test
    void cambiar_a_estado_invalido_retorna_409() {
        // DISPONIBLE → EN_RUTA es válido, pero EN_MANTENIMIENTO → EN_RUTA no lo es
        String registerBody = """
                {
                  "category": "CAMIONETA_URBANA",
                  "loadCapacity": 1000.0,
                  "transporterId": 997
                }
                """;

        Long vehicleId = given()
            .contentType(ContentType.JSON)
            .body(registerBody)
            .when()
                .post("/vehicles")
            .then()
                .statusCode(201)
                .extract()
                .jsonPath().getLong("vehicleId");

        String patchBody = """
                {
                  "newStatus": "EN_RUTA"
                }
                """;

        given()
            .contentType(ContentType.JSON)
            .body(patchBody)
            .when()
                .patch("/vehicles/" + vehicleId + "/status")
            .then()
                .statusCode(409);
    }

    @Test
    void listar_vehiculos_filtrado_por_categoria_retorna_solo_esa_categoria() {
        given()
            .queryParam("category", "CAMION_SENCILLO")
            .when()
                .get("/vehicles")
            .then()
                .statusCode(200)
                .body("data", everyItem(hasEntry("category", "CAMION_SENCILLO")));
    }

    @Test
    void registrar_vehiculo_con_body_invalido_retorna_400() {
        String invalidBody = """
                {
                  "loadCapacity": -100.0
                }
                """;

        given()
            .contentType(ContentType.JSON)
            .body(invalidBody)
            .when()
                .post("/vehicles")
            .then()
                .statusCode(400);
    }
}
