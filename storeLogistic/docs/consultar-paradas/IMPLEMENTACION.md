# Documentación Detallada de Implementación
## Feature: Consultar Paradas de Rutas | Módulo: Logística de Despacho y Distribución

**Versión:** 0.0.1-SNAPSHOT | **Fecha:** 24 de Abril de 2026  
**Stack:** Java 21 + Spring Boot 3.5.13 + JPA (native SQL) + MapStruct + Testcontainers

---

## 📋 Contenidos

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Arquitectura](#arquitectura)
3. [Estructura del Proyecto](#estructura-del-proyecto)
4. [Modelo de Datos](#modelo-de-datos)
5. [Capa de Dominio](#capa-de-dominio)
6. [Capa de Aplicación](#capa-de-aplicación)
7. [Capa de Infraestructura](#capa-de-infraestructura)
8. [Flujo de Operación](#flujo-de-operación)
9. [API REST](#api-rest)
10. [Manejo de Errores](#manejo-de-errores)
11. [Testing](#testing)
12. [Ejecución](#ejecución)

---

## 🎯 Resumen Ejecutivo

El módulo de Logística expone dos endpoints REST para que el transportista consulte sus paradas desde su dispositivo:

- **Lista** `GET /logistics/routes/{routeId}/stops?carrierId={carrierId}` — retorna paradas básicas sin llamadas externas, optimizada para la pantalla principal del conductor.
- **Detalle** `GET /logistics/routes/{routeId}/stops/{stopId}?carrierId={carrierId}` — retorna el detalle completo de una parada, incluyendo `customerContact`, `paymentMethod` y `totalACobrar` consultados al Módulo Financiero.

La **autorización** se resuelve con un JOIN nativo entre `routes` y `vehiculos`: la ruta debe estar asignada a un vehículo cuyo `id_transportista = carrierId`. El transportista es un módulo externo — no existe tabla propia de transportistas en este módulo.

### Características Clave

✅ **Dominio Puro**: capa de dominio sin dependencias de Spring, JPA ni Lombok  
✅ **Autorización por JOIN nativo**: `routes JOIN vehiculos WHERE id_transportista = carrierId`  
✅ **403 ambiguo intencional**: ruta inexistente y no autorizada dan la misma respuesta (FR-003)  
✅ **Lista sin pago**: `GET /stops` no llama al Módulo Financiero — rápida y sin dependencias externas  
✅ **null con significado**: `totalACobrar = null` para CARTERA_COMERCIAL, nunca se convierte a 0  
✅ **Services puros**: `GetStopDetailService` retorna solo `Stop`; el controller ensambla con `OrderPaymentMethod`  
✅ **Testing completo**: unitarios, integración con Testcontainers PostgreSQL real

---

## 🏗️ Arquitectura

La feature implementa **Arquitectura Hexagonal (Ports & Adapters)** estricta. La regla de dependencias es unidireccional: infraestructura → aplicación → dominio.

**Principios aplicados:**
- `AuthorizationService` centraliza la verificación de acceso — evita duplicación en cada service
- `GetStopDetailService` retorna `Stop` puro — el ensamblado con pago es responsabilidad del controller
- `QueryStopsMapper` es `abstract class` (no interface) porque `Stop` usa fluent accessors (`stopId()`) en lugar de JavaBean getters (`getStopId()`), lo que impide el mapeo automático de MapStruct
- `@RestControllerAdvice(basePackages = "...consultar")` — el manejador de errores no interfiere con otros módulos

**Stack Tecnológico**

| Componente | Tecnología | Versión |
|-----------|-----------|---------|
| **JDK** | Java | 21 LTS |
| **Framework** | Spring Boot | 3.5.13 |
| **Persistencia** | Spring Data JPA + native SQL | 3.5.13 |
| **Migración** | Flyway | 10.x |
| **Mapping** | MapStruct (abstract class) | 1.6.3 |
| **Utilidades** | Lombok (solo app/infra) | 1.18.x |
| **Testing** | JUnit 5, Mockito, AssertJ, Testcontainers | 5.x / 1.19.x |

---

## 📁 Estructura del Proyecto

```
src/main/java/co/edu/unimagdalena/storelogistic/
│
└── consultar/                                      # Módulo Consultar Paradas
    │
    ├── domain/                                     # ★ CAPA DE DOMINIO (sin dependencias externas)
    │   ├── exceptions/
    │   │   └── AccessDeniedException.java          # "Acceso denegado" — sin revelar existencia
    │   └── ports/
    │       └── in/
    │           ├── QueryStopsUseCase.java          # List<Stop> query(routeId, carrierId)
    │           └── GetStopDetailUseCase.java       # Stop get(routeId, stopId, carrierId)
    │
    ├── application/
    │   └── services/
    │       ├── AuthorizationService.java           # verifyCarrierHasAccess(routeId, carrierId)
    │       ├── QueryStopsService.java              # implementa QueryStopsUseCase
    │       └── GetStopDetailService.java           # implementa GetStopDetailUseCase
    │
    └── infrastructure/
        ├── exception/
        │   └── ConsultarExceptionHandler.java      # AccessDeniedException → HTTP 403
        ├── mapper/
        │   └── QueryStopsMapper.java               # abstract class — toSummaryDTO, toDetailDTO, toResponse
        └── web/
            ├── controller/
            │   └── QueryStopsController.java       # GET /stops y GET /stops/{stopId}
            └── dto/
                ├── StopSummaryDTO.java             # idStop, sequence, deliveryAddress, orderId, status
                ├── StopDetailDTO.java              # + customerContact, paymentMethod, totalACobrar
                └── QueryStopsResponse.java         # routeId, carrierId, totalStops, stops
```

**Reusa del módulo `route`:** `RouteRepository`, `StopRepository`, `RouteJpaEntity`, `StopJpaEntity`, `Stop`, `Route`.  
**Reusa del módulo `paymentmethod`:** `ConsultPaymentMethodUseCase`, `OrderPaymentMethod`.  
**Sin tabla `carrier`:** el transportista es externo; su ID (`id_transportista BIGINT`) vive en `vehiculos`.

---

## 🗄️ Modelo de Datos

### Flyway V7 — Cambios en Schema

```sql
-- Añadir customer_contact a stops (nullable)
ALTER TABLE stops ADD COLUMN IF NOT EXISTS customer_contact VARCHAR(200);

-- Índice compuesto para búsqueda y ordenamiento por ruta + secuencia
CREATE INDEX IF NOT EXISTS idx_stops_sequence ON stops(id_route, sequence);
```

### Query de Autorización (native SQL)

```sql
SELECT r.* FROM routes r
JOIN vehiculos v ON r.id_vehicle = v.id_vehiculo
WHERE r.id_route = :routeId
  AND v.id_transportista = :carrierId
```

Se usa `nativeQuery = true` porque `RouteJpaEntity.vehicleId` es un `Long` plano sin `@ManyToOne` — el JOIN cross-entidad JPQL no aplica entre módulos.

### Esquema Relevante

```
categorias ──< vehiculos >── routes ──< stops >── orders
                  id_transportista (BIGINT)
                       ↑
                   carrierId del request
```

---

## 🎯 Capa de Dominio

Contiene la **lógica de negocio pura** sin dependencias de tecnología. Sin Spring, sin JPA, sin Lombok.

### AccessDeniedException

```java
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException() {
        super("Acceso denegado");
    }
}
```

Mensaje fijo e intencionalmente genérico: no revela si la ruta existe o pertenece a otro transportista (FR-003).

### Puertos de Dominio

```java
// Entrada — lista de paradas
public interface QueryStopsUseCase {
    List<Stop> query(Long routeId, Long carrierId);
}

// Entrada — detalle de parada
public interface GetStopDetailUseCase {
    Stop get(Long routeId, Long stopId, Long carrierId);
}
```

Los puertos de salida (`RouteRepository`, `StopRepository`) pertenecen al módulo `route` y son reutilizados directamente.

---

## 💼 Capa de Aplicación

### AuthorizationService

```java
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final RouteRepository routeRepository;

    public Route verifyCarrierHasAccess(Long routeId, Long carrierId) {
        return routeRepository.findByIdAndCarrierId(routeId, carrierId)
                .orElseThrow(AccessDeniedException::new);
    }
}
```

Centraliza la autorización. `Optional.empty()` se produce tanto para ruta inexistente como para ruta de otro transportista — ambigüedad intencional.

### QueryStopsService

```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueryStopsService implements QueryStopsUseCase {

    private final AuthorizationService authorizationService;
    private final StopRepository stopRepository;

    @Override
    public List<Stop> query(Long routeId, Long carrierId) {
        log.info("Querying stops: routeId={}, carrierId={}", routeId, carrierId);
        try {
            authorizationService.verifyCarrierHasAccess(routeId, carrierId);
            List<Stop> stops = stopRepository.findByRouteIdOrderBySequence(routeId);
            log.info("Stops found: count={}, routeId={}", stops.size(), routeId);
            return stops;
        } catch (AccessDeniedException e) {
            log.warn("Access denied: carrierId={} on routeId={}", carrierId, routeId);
            throw e;
        }
    }
}
```

No llama al Módulo Financiero — solo verifica acceso y retorna paradas ordenadas.

### GetStopDetailService

```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetStopDetailService implements GetStopDetailUseCase {

    private final AuthorizationService authorizationService;
    private final StopRepository stopRepository;

    @Override
    public Stop get(Long routeId, Long stopId, Long carrierId) {
        log.info("Stop detail requested: routeId={}, stopId={}, carrierId={}", routeId, stopId, carrierId);
        try {
            authorizationService.verifyCarrierHasAccess(routeId, carrierId);
            Stop stop = stopRepository.findByIdAndRouteId(stopId, routeId)
                    .orElseThrow(AccessDeniedException::new);
            log.info("Stop detail found: stopId={}, orderId={}", stop.stopId(), stop.orderId());
            return stop;
        } catch (AccessDeniedException e) {
            log.warn("Access denied: carrierId={} attempted stop {} on route {}", carrierId, stopId, routeId);
            throw e;
        }
    }
}
```

Retorna solo `Stop` — el ensamblado con `OrderPaymentMethod` es responsabilidad del controller.

---

## 🌐 Capa de Infraestructura

### RouteSpringRepository — Query de Autorización

```java
@Query(value = """
    SELECT r.* FROM routes r
    JOIN vehiculos v ON r.id_vehicle = v.id_vehiculo
    WHERE r.id_route = :routeId
      AND v.id_transportista = :carrierId
    """, nativeQuery = true)
Optional<RouteJpaEntity> findByRouteIdAndCarrierId(@Param("routeId") Long routeId,
                                                   @Param("carrierId") Long carrierId);
```

### StopSpringRepository — Métodos Agregados

```java
// Lista ordenada para QueryStopsService
List<StopJpaEntity> findByRouteIdOrderBySequence(Long routeId);

// Búsqueda por ID verificando pertenencia a la ruta
Optional<StopJpaEntity> findByStopIdAndRouteId(Long stopId, Long routeId);
```

### QueryStopsMapper — Abstract Class

```java
@Mapper(componentModel = "spring")
public abstract class QueryStopsMapper {

    public StopSummaryDTO toSummaryDTO(Stop stop) {
        if (stop == null) return null;
        return new StopSummaryDTO(
                stop.stopId(), stop.sequence(), stop.deliveryAddress(),
                stop.orderId(),
                stop.status() != null ? stop.status().name() : null
        );
    }

    public StopDetailDTO toDetailDTO(Stop stop, OrderPaymentMethod payment) {
        if (stop == null) return null;
        return new StopDetailDTO(
                stop.stopId(), stop.sequence(), stop.deliveryAddress(), stop.orderId(),
                stop.customerContact(),
                payment != null && payment.paymentMethod() != null ? payment.paymentMethod().name() : null,
                payment != null ? payment.totalPedido() : null,
                stop.status() != null ? stop.status().name() : null
        );
    }

    public QueryStopsResponse toResponse(Long routeId, Long carrierId, List<Stop> stops) {
        List<StopSummaryDTO> dtos = stops.stream().map(this::toSummaryDTO).toList();
        return new QueryStopsResponse(routeId, carrierId, dtos.size(), dtos);
    }
}
```

`abstract class` en lugar de `interface` porque `Stop` usa fluent accessors (`stop.stopId()`) — MapStruct no puede generar mapeos automáticos sin JavaBean getters.

### QueryStopsController — Ensamblado de Stop + Payment

```java
@RestController
@RequestMapping("/logistics/routes")
@RequiredArgsConstructor
@Validated
public class QueryStopsController {

    private final QueryStopsUseCase queryStopsUseCase;
    private final GetStopDetailUseCase getStopDetailUseCase;
    private final ConsultPaymentMethodUseCase consultPaymentMethodUseCase;
    private final QueryStopsMapper mapper;

    @GetMapping("/{routeId}/stops")
    public ResponseEntity<QueryStopsResponse> listStops(
            @PathVariable @Positive Long routeId,
            @RequestParam @Positive Long carrierId) {
        List<Stop> stops = queryStopsUseCase.query(routeId, carrierId);
        return ResponseEntity.ok(mapper.toResponse(routeId, carrierId, stops));
    }

    @GetMapping("/{routeId}/stops/{stopId}")
    public ResponseEntity<StopDetailDTO> getStopDetail(
            @PathVariable @Positive Long routeId,
            @PathVariable @Positive Long stopId,
            @RequestParam @Positive Long carrierId) {
        Stop stop = getStopDetailUseCase.get(routeId, stopId, carrierId);
        OrderPaymentMethod payment = consultPaymentMethodUseCase.consult(stop.orderId());
        return ResponseEntity.ok(mapper.toDetailDTO(stop, payment));
    }
}
```

El controller ensambla `Stop + OrderPaymentMethod` — esto mantiene `GetStopDetailService` puro y testeable sin mockear el módulo financiero.

### ConsultarExceptionHandler

```java
@RestControllerAdvice(basePackages = "co.edu.unimagdalena.storelogistic.consultar")
public class ConsultarExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(build("ACCESO_DENEGADO", ex.getMessage()));
    }

    // + handlers para @Positive violations (400) y MissingServletRequestParameterException (400)
}
```

`basePackages` garantiza que solo maneja excepciones del módulo `consultar`.

---

## 🔄 Flujo de Operación

### Lista de Paradas

```
GET /api/v1/logistics/routes/{routeId}/stops?carrierId={carrierId}
        │
        ▼
┌─────────────────────────────────────────┐
│  QueryStopsController                   │
│  • Valida @Positive en routeId/carrierId│
│  • Delega a QueryStopsUseCase           │
│  • Mapea List<Stop> → QueryStopsResponse│
└────────┬────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────┐
│  QueryStopsService                           │
│  • AuthorizationService.verifyAccess()       │
│    → RouteRepository.findByIdAndCarrierId()  │
│    → native SQL JOIN vehiculos               │
│    → Optional.empty() → AccessDeniedException│
│  • StopRepository.findByRouteIdOrderBySeq()  │
└────────┬─────────────────────────────────────┘
         │
         ├─ Autorizado  → 200 { routeId, carrierId, totalStops, stops[] }
         └─ Denegado    → 403 { codigo: "ACCESO_DENEGADO" }
```

### Detalle de Parada

```
GET /api/v1/logistics/routes/{routeId}/stops/{stopId}?carrierId={carrierId}
        │
        ▼
┌──────────────────────────────────────────────────┐
│  QueryStopsController                            │
│  1. GetStopDetailUseCase.get(routeId,stopId,cId) │
│  2. ConsultPaymentMethodUseCase.consult(orderId) │
│  3. mapper.toDetailDTO(stop, payment)            │
└──────┬───────────────────────┬───────────────────┘
       │                       │
       ▼                       ▼
┌────────────────┐   ┌──────────────────────────────┐
│GetStopDetail   │   │ ConsultPaymentMethodService   │
│Service         │   │ → MockFinanceModuleClient     │
│• verifyAccess()│   │   (hasta que módulo real esté │
│• findByIdAnd   │   │    disponible)                │
│  RouteId()     │   └──────────────────────────────┘
└────────────────┘
         │
         ├─ Autorizado  → 200 { idStop, ..., paymentMethod, totalACobrar, status }
         └─ Denegado    → 403 { codigo: "ACCESO_DENEGADO" }
```

---

## 🌍 API REST

### Configuración Base

- **Host:** `localhost:8080`
- **Base Path:** `/api/v1`
- **Documentación OpenAPI:** `http://localhost:8080/api/v1/docs/openapi.json`
- **Swagger UI:** `http://localhost:8080/api/v1/docs/swagger-ui.html`

---

### GET /logistics/routes/{routeId}/stops

**Descripción:** Lista las paradas de una ruta, ordenadas por sequence ASC. No llama al Módulo Financiero.

**Response 200:**
```json
{
  "routeId": 3,
  "carrierId": 1,
  "totalStops": 2,
  "stops": [
    {
      "idStop": 7,
      "sequence": 1,
      "deliveryAddress": "Calle 10 #20-30",
      "orderId": 6,
      "status": "PENDING"
    },
    {
      "idStop": 8,
      "sequence": 2,
      "deliveryAddress": "Carrera 5 #15-20",
      "orderId": 7,
      "status": "PENDING"
    }
  ]
}
```

**Response 403 — Acceso denegado:**
```json
{
  "codigo": "ACCESO_DENEGADO",
  "mensaje": "Acceso denegado",
  "timestamp": "2026-04-24T15:32:34"
}
```

**Response 400 — carrierId faltante o inválido:**
```json
{
  "codigo": "VALIDACION_FALLIDA",
  "mensaje": "Valor inválido en parámetros de la solicitud",
  "timestamp": "2026-04-24T15:32:34"
}
```

---

### GET /logistics/routes/{routeId}/stops/{stopId}

**Descripción:** Retorna el detalle completo de una parada, incluyendo método de pago consultado al Módulo Financiero.

**Response 200 — CONTRA_ENTREGA:**
```json
{
  "idStop": 7,
  "sequence": 1,
  "deliveryAddress": "Calle 10 #20-30",
  "orderId": 6,
  "customerContact": "3001234567",
  "paymentMethod": "CONTRA_ENTREGA",
  "totalACobrar": 150000.00,
  "status": "PENDING"
}
```

**Response 200 — CARTERA_COMERCIAL:**
```json
{
  "idStop": 8,
  "sequence": 2,
  "deliveryAddress": "Carrera 5 #15-20",
  "orderId": 2,
  "customerContact": "3009876543",
  "paymentMethod": "CARTERA_COMERCIAL",
  "totalACobrar": null,
  "status": "PENDING"
}
```

> `totalACobrar: null` indica que el pago es por cartera comercial — el conductor **no cobra** en la entrega. El frontend debe mostrar "No cobrar", nunca "$0".

**cURL de prueba:**
```bash
# Lista de paradas
curl "http://localhost:8080/api/v1/logistics/routes/3/stops?carrierId=1"

# Detalle de parada
curl "http://localhost:8080/api/v1/logistics/routes/3/stops/7?carrierId=1"

# Error 403 — carrierId incorrecto
curl "http://localhost:8080/api/v1/logistics/routes/3/stops?carrierId=99"

# Error 400 — stopId = 0
curl "http://localhost:8080/api/v1/logistics/routes/3/stops/0?carrierId=1"
```

---

## ⚠️ Manejo de Errores

```java
@RestControllerAdvice(basePackages = "co.edu.unimagdalena.storelogistic.consultar")
public class ConsultarExceptionHandler { ... }
```

### Tabla de Códigos

| Excepción | HTTP | Código JSON | Causa |
|-----------|------|-------------|-------|
| `AccessDeniedException` | 403 | `ACCESO_DENEGADO` | Ruta no asignada, ruta inexistente, o stop fuera de ruta |
| `ConstraintViolationException` | 400 | `VALIDACION_FALLIDA` | routeId/stopId/carrierId ≤ 0 |
| `MissingServletRequestParameterException` | 400 | `PARAMETRO_REQUERIDO` | carrierId faltante |
| `MethodArgumentTypeMismatchException` | 400 | `PARAMETRO_INVALIDO` | routeId/stopId no numérico |

---

## 🧪 Testing

### Estructura de Tests

```
src/test/java/co/edu/unimagdalena/storelogistic/consultar/
│
├── unit/
│   ├── domain/
│   │   └── exceptions/
│   │       └── AccessDeniedExceptionTest.java           ✅ mensaje fijo, no revela existencia
│   ├── application/
│   │   ├── AuthorizationServiceTest.java                ✅ 3 casos: asignada/otro/inexistente
│   │   ├── QueryStopsServiceTest.java                   ✅ 3 casos: happy path, denegado, vacío
│   │   └── GetStopDetailServiceTest.java                ✅ 3 casos: happy path, denegado, stop fuera ruta
│   └── infrastructure/
│       ├── QueryStopsMapperTest.java                    ✅ 6 casos: summary, detail CONTRA/CARTERA/null, response
│       └── QueryStopsControllerTest.java                ✅ 7 casos: lista y detalle (200/403/400)
│
├── integration/
│   └── QueryStopsApiContractTest.java                   ✅ 9 casos end-to-end con Testcontainers
│
└── testdata/
    └── StopFixture.java                                 # Fixtures: standard(), withSequence(), withRoute()
```

### Setup de Integración — vehiculos en lugar de carrier

```java
@BeforeEach
void setUp() {
    // Limpieza en orden FK
    jdbcTemplate.update("DELETE FROM stops");
    jdbcTemplate.update("DELETE FROM orders");
    jdbcTemplate.update("DELETE FROM routes");
    jdbcTemplate.update("DELETE FROM vehiculos");
    jdbcTemplate.update("DELETE FROM categorias");

    // Insertar categoria → vehiculo con id_transportista = carrierId
    jdbcTemplate.update("INSERT INTO categorias (tipo, capacidad_maxima_kg) VALUES (?, ?)", "CAMION", 5000);
    Long categoriaId = jdbcTemplate.queryForObject("SELECT id_categoria FROM categorias WHERE tipo = ?", Long.class, "CAMION");

    carrierId = 42L;
    jdbcTemplate.update(
        "INSERT INTO vehiculos (id_categoria, capacidad_carga, estado, id_transportista, peso_actual) VALUES (?, ?, ?, ?, ?)",
        categoriaId, 5000, "DISPONIBLE", carrierId, 0
    );
    Long vehiculoId = jdbcTemplate.queryForObject("SELECT id_vehiculo FROM vehiculos WHERE id_transportista = ?", Long.class, carrierId);

    // Ruta asignada al vehiculo (sin tabla carrier)
    jdbcTemplate.update("INSERT INTO routes (id_vehicle, total_capacity_kg, accumulated_weight_kg, status, dispatch_date) VALUES (?, 1000, 0, 'AVAILABLE', CURRENT_DATE)", vehiculoId);
    routeId = jdbcTemplate.queryForObject("SELECT id_route FROM routes WHERE id_vehicle = ? ORDER BY id_route DESC LIMIT 1", Long.class, vehiculoId);
    // ... insertar orders y stops
}
```

### Ejemplo — Test de Autorización

```java
@Test
@DisplayName("EC: GET stops with wrong carrierId → 403 Acceso denegado")
void getStops_routeNotAssignedToCarrier_returns403() {
    Long otherCarrierId = 9999L;

    ResponseEntity<String> response = restClient.get()
            .uri("/logistics/routes/{routeId}/stops?carrierId={carrierId}", routeId, otherCarrierId)
            .retrieve()
            .onStatus(status -> status.value() == 403, (req, res) -> {})
            .toEntity(String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("ACCESO_DENEGADO");
    assertThat(response.getBody()).contains("Acceso denegado");
}
```

### Ejecutar Tests

```bash
# Solo tests de consultar paradas
./gradlew cleanTest test --tests "co.edu.unimagdalena.storelogistic.consultar.*"

# Todos los tests
./gradlew cleanTest test
```

---

## 🚀 Ejecución

### Prerequisitos

1. **Java 21 LTS**
2. **PostgreSQL 17** con base de datos `storeLogistic`
3. **RabbitMQ 3.x** corriendo en `localhost:5672`
4. **Docker** (solo para tests de integración con Testcontainers)

### Datos de Prueba

Todos los vehículos del seed (V2) quedan con `id_transportista = 1` después de la migración V3. Para probar los endpoints:

```sql
-- Insertar órdenes
INSERT INTO orders (logistic_weight, delivery_address) VALUES (100, 'Calle 10 #20-30');
INSERT INTO orders (logistic_weight, delivery_address) VALUES (150, 'Carrera 5 #15-20');

-- Insertar ruta asignada al vehículo 1 (id_transportista = 1)
INSERT INTO routes (id_vehicle, total_capacity_kg, accumulated_weight_kg, status, dispatch_date)
VALUES (1, 1500, 250, 'AVAILABLE', CURRENT_DATE);

-- Insertar paradas (ajustar IDs según los generados)
INSERT INTO stops (id_route, id_order, sequence, delivery_address, status, customer_contact)
VALUES (:routeId, :orderId1, 1, 'Calle 10 #20-30', 'PENDING', '3001234567');

INSERT INTO stops (id_route, id_order, sequence, delivery_address, status, customer_contact)
VALUES (:routeId, :orderId2, 2, 'Carrera 5 #15-20', 'PENDING', '3009876543');
```

Luego probar con `carrierId=1`.

### Activar cliente real del Módulo Financiero

Cuando el Módulo Financiero esté disponible:
1. Agregar `@Component` a `FinanceModuleClient`
2. Eliminar `@Component` de `MockFinanceModuleClient`
3. Configurar `FINANCE_MODULE_BASE_URL` en `application.yml`

---

## 📊 Capacidades Implementadas

✅ Listar paradas de una ruta con verificación de autorización via JOIN nativo  
✅ Ver detalle de parada con customerContact, paymentMethod y totalACobrar  
✅ Autorización sin tabla carrier — usando id_transportista en vehiculos  
✅ 403 ambiguo: ruta inexistente y no autorizada dan la misma respuesta  
✅ totalACobrar: null para CARTERA_COMERCIAL con significado de negocio explícito  
✅ Services puros: GetStopDetailService retorna Stop, el controller ensambla con pago  
✅ MapStruct con abstract class para Stop con fluent accessors  
✅ Testing unitario y de integración con Testcontainers PostgreSQL real  

---

## 📝 Información del Proyecto

| Atributo | Valor |
|----------|-------|
| **Versión** | 0.0.1-SNAPSHOT |
| **Fecha** | 24 de Abril de 2026 |
| **Java** | 21 LTS |
| **Spring Boot** | 3.5.13 |
| **Rama** | feature/consultar-paradas |

---

**Fin de la Documentación.**
