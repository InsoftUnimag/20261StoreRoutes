# Documentación Detallada de Implementación
## Feature: Consultar Método de Pago de un Pedido | Módulo: Logística de Despacho y Distribución

**Versión:** 0.0.1-SNAPSHOT | **Fecha:** 24 de Abril de 2026  
**Stack:** Java 21 + Spring Boot 3.5.13 + WebClient + Spring Retry + WireMock

---

## 📋 Contenidos

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Arquitectura](#arquitectura)
3. [Estructura del Proyecto](#estructura-del-proyecto)
4. [Capa de Dominio](#capa-de-dominio)
5. [Capa de Aplicación](#capa-de-aplicación)
6. [Capa de Infraestructura](#capa-de-infraestructura)
7. [Flujo de Operación](#flujo-de-operación)
8. [API REST](#api-rest)
9. [Manejo de Errores](#manejo-de-errores)
10. [Testing](#testing)
11. [Ejecución](#ejecución)

---

## 🎯 Resumen Ejecutivo

El módulo de Logística consulta de forma síncrona el método de pago de un pedido al Módulo Financiero, enviando únicamente el `idPedido`. La respuesta incluye la forma de pago y el total a cobrar en entrega en una **sola llamada HTTP**:

- **CONTRA_ENTREGA** → retorna `paymentMethod` y `totalPedido` (monto a cobrar en puerta)
- **CARTERA_COMERCIAL** → retorna `paymentMethod` y `totalPedido: null` (pago por cartera, no se cobra en entrega)

El sistema aplica **reintentos con backoff exponencial** (máximo 3 intentos) ante fallos de conectividad, diferenciando errores de negocio (no reintentables) de errores de infraestructura (reintentables).

### Características Clave

✅ **Dominio Puro**: capa de dominio sin dependencias de Spring, JPA ni Lombok  
✅ **Una sola llamada HTTP**: forma de pago y total del pedido en un único endpoint del Módulo Financiero  
✅ **null con significado de negocio**: `totalPedido = null` indica CARTERA_COMERCIAL, nunca se convierte a 0  
✅ **Reintentos inteligentes**: backoff exponencial solo ante 5xx/timeout, nunca ante 404/422  
✅ **Mock intercambiable**: `MockFinanceModuleClient` reemplazable por `FinanceModuleClient` sin cambiar el dominio  
✅ **Testing completo**: pruebas unitarias, de integración con WireMock y de contrato de API

---

## 🏗️ Arquitectura

La feature implementa **Arquitectura Hexagonal (Ports & Adapters)** estricta. La regla de dependencias es unidireccional: infraestructura → aplicación → dominio. El dominio no importa nada externo.

**Principios aplicados:**
- Inversión de Dependencias: `ConsultPaymentMethodService` depende de `FinanceGatewayPort` (puerto), nunca de `FinanceModuleClient` (adaptador)
- Aislamiento total: el dominio usa solo `java.*` y `java.math.*`
- Resiliencia: Spring Retry con backoff exponencial en el adaptador de salida
- Comunicación síncrona: WebClient (bloqueante) hacia el Módulo Financiero

**Stack Tecnológico**

| Componente | Tecnología | Versión |
|-----------|-----------|---------|
| **JDK** | Java | 21 LTS |
| **Framework** | Spring Boot | 3.5.13 |
| **HTTP Client** | Spring WebFlux (WebClient) | 3.5.13 |
| **Reintentos** | Spring Retry | 2.x |
| **Mapping** | MapStruct | 1.6.3 |
| **Utilidades** | Lombok (solo app/infra) | 1.18.x |
| **Testing** | JUnit 5, Mockito, AssertJ, WireMock | 5.x / 3.13.0 |

---

## 📁 Estructura del Proyecto

```
src/main/java/co/edu/unimagdalena/storelogistic/
│
└── paymentmethod/                                  # Módulo Consultar Método de Pago
    │
    ├── domain/                                     # ★ CAPA DE DOMINIO (sin dependencias externas)
    │   │
    │   ├── models/
    │   │   └── OrderPaymentMethod.java             # record — orderId, paymentMethod, totalPedido (nullable)
    │   │
    │   ├── values/
    │   │   └── PaymentMethod.java                  # enum — CONTRA_ENTREGA | CARTERA_COMERCIAL
    │   │
    │   ├── ports/
    │   │   ├── in/
    │   │   │   └── ConsultPaymentMethodUseCase.java # OrderPaymentMethod consult(Long orderId)
    │   │   └── out/
    │   │       └── FinanceGatewayPort.java          # OrderPaymentMethod findByOrderId(Long orderId)
    │   │
    │   └── exceptions/
    │       ├── LogisticsException.java              # Excepción base runtime
    │       ├── OrderNotFoundException.java          # 404 — Pedido no encontrado
    │       ├── PaymentMethodNotRegisteredException  # 422 — Sin forma de pago registrada
    │       └── FinanceServiceUnavailableException   # 503 — Módulo Financiero no disponible
    │
    ├── application/
    │   └── services/
    │       └── ConsultPaymentMethodService.java     # Implementa ConsultPaymentMethodUseCase
    │
    └── infrastructure/
        ├── config/
        │   ├── WebClientConfig.java                 # Bean WebClient con baseUrl del Módulo Financiero
        │   └── RetryConfig.java                     # RetryTemplate: 3 intentos, backoff exponencial
        ├── client/
        │   ├── FinanceModuleClient.java             # Adaptador real HTTP (activar cuando el módulo esté disponible)
        │   └── MockFinanceModuleClient.java         # Stub temporal — reemplazar por FinanceModuleClient
        ├── web/
        │   ├── controller/
        │   │   └── PaymentMethodController.java     # GET /pedidos/{id_pedido}/forma-pago
        │   └── dto/
        │       ├── PaymentMethodResponse.java        # orderId, paymentMethod, totalPedido
        │       └── FinancePaymentMethodResponse.java # DTO interno — deserializa respuesta del Módulo Financiero
        ├── mapper/
        │   └── PaymentMethodMapper.java             # MapStruct: FinanceResponse ↔ Domain ↔ PaymentMethodResponse
        └── exception/
            ├── PaymentMethodExceptionHandler.java   # @RestControllerAdvice(basePackages=...)
            └── ErrorResponse.java                   # Estructura estándar de error
```

### Convenciones de Nombres

| Componente | Sufijo | Ejemplo |
|-----------|--------|---------|
| **Puerto de entrada** | `UseCase` | `ConsultPaymentMethodUseCase` |
| **Implementación de use case** | `Service` | `ConsultPaymentMethodService` |
| **Puerto de salida** | `Port` | `FinanceGatewayPort` |
| **Adaptador HTTP** | `Client` | `FinanceModuleClient` |
| **Transfer Object** | `Request`/`Response` | `PaymentMethodResponse` |

---

## 🎯 Capa de Dominio

Contiene la **lógica de negocio pura** independiente de tecnología. Sin Spring, sin JPA, sin Lombok.

### Value Object — PaymentMethod

```java
public enum PaymentMethod {
    CONTRA_ENTREGA,
    CARTERA_COMERCIAL;

    public static PaymentMethod fromString(String value) {
        return Arrays.stream(values())
                .filter(pm -> pm.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown payment method: " + value));
    }
}
```

`fromString()` es la **única fuente de verdad** para parsear valores del Módulo Financiero. Ni el mapper ni el cliente duplican esta lógica.

### Modelo de Dominio — OrderPaymentMethod

```java
public record OrderPaymentMethod(Long orderId, PaymentMethod paymentMethod, BigDecimal totalPedido) {

    public static OrderPaymentMethod of(Long orderId, PaymentMethod paymentMethod, BigDecimal totalPedido) {
        return new OrderPaymentMethod(
                Optional.ofNullable(orderId)
                        .orElseThrow(() -> new IllegalArgumentException("orderId must not be null")),
                Optional.ofNullable(paymentMethod)
                        .orElseThrow(() -> new IllegalArgumentException("paymentMethod must not be null")),
                totalPedido   // nullable — null = CARTERA_COMERCIAL, valor = CONTRA_ENTREGA
        );
    }
}
```

**Regla de negocio clave:** `totalPedido` es `null` para `CARTERA_COMERCIAL` y contiene el monto liquidado para `CONTRA_ENTREGA`. Este `null` tiene **significado de negocio explícito** — nunca debe convertirse a `0` en ninguna capa.

### Puertos de Dominio

```java
// Entrada
public interface ConsultPaymentMethodUseCase {
    OrderPaymentMethod consult(Long orderId);
}

// Salida
public interface FinanceGatewayPort {
    OrderPaymentMethod findByOrderId(Long orderId);
}
```

### Excepciones de Dominio

| Excepción | Causa | HTTP |
|-----------|-------|------|
| `OrderNotFoundException` | Módulo Financiero retorna 404 | 404 |
| `PaymentMethodNotRegisteredException` | Módulo Financiero retorna 422 | 422 |
| `FinanceServiceUnavailableException` | 3 reintentos agotados (5xx/timeout) | 503 |

---

## 💼 Capa de Aplicación

### ConsultPaymentMethodService

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultPaymentMethodService implements ConsultPaymentMethodUseCase {

    private final FinanceGatewayPort financeGatewayPort;

    @Override
    public OrderPaymentMethod consult(Long orderId) {
        log.info("Consulting payment method for orderId={}", orderId);
        OrderPaymentMethod result = financeGatewayPort.findByOrderId(orderId);
        log.info("Payment method for orderId={} is {}", orderId, result.paymentMethod());
        return result;
    }
}
```

El servicio es deliberadamente delgado: solo delega al puerto de salida y registra logs. No tiene conocimiento de HTTP, DTOs ni reintentos. Depende de `FinanceGatewayPort` (abstracción), nunca de `FinanceModuleClient` (implementación) — garantía del principio DIP.

---

## 🌐 Capa de Infraestructura

### Adaptador de Salida — FinanceModuleClient

El cliente HTTP real implementa `FinanceGatewayPort` y encapsula toda la complejidad de HTTP y reintentos:

```java
@Override
public OrderPaymentMethod findByOrderId(Long orderId) {
    return retryTemplate.execute(
            context -> callFinanceModule(orderId),  // intento normal
            context -> {                            // recovery tras agotar reintentos
                // Si el último error es no-reintentable (404/422), propágalo sin envolver
                Optional.ofNullable(context.getLastThrowable())
                        .filter(ex -> NON_RETRYABLE_EXCEPTIONS.contains(ex.getClass()))
                        .map(RuntimeException.class::cast)
                        .ifPresent(ex -> { throw ex; });
                throw new FinanceServiceUnavailableException("...");
            }
    );
}
```

**Clasificación de errores:**

| Error del Módulo Financiero | Reintentable | Excepción lanzada |
|-----------------------------|-------------|-------------------|
| HTTP 404 | No | `OrderNotFoundException` |
| HTTP 422 | No | `PaymentMethodNotRegisteredException` |
| HTTP 5xx | Sí (hasta 3 veces) | `FinanceServiceUnavailableException` |
| Timeout/red | Sí (hasta 3 veces) | `FinanceServiceUnavailableException` |

### Mock — MockFinanceModuleClient

Stub temporal activo hasta que el Módulo Financiero esté disponible:

```java
@Component  // ← Cambiar a @Component en FinanceModuleClient para activar el real
public class MockFinanceModuleClient implements FinanceGatewayPort {

    private static final Map<Long, OrderPaymentMethod> HAPPY_PATHS = Map.of(
            1L, OrderPaymentMethod.of(1L, PaymentMethod.CONTRA_ENTREGA, new BigDecimal("150000.00")),
            2L, OrderPaymentMethod.of(2L, PaymentMethod.CARTERA_COMERCIAL, null)
    );

    private static final Map<Long, Supplier<LogisticsException>> ERROR_CASES = Map.of(
            404L, () -> new OrderNotFoundException("Pedido no encontrado"),
            422L, () -> new PaymentMethodNotRegisteredException("..."),
            503L, () -> new FinanceServiceUnavailableException("...")
    );
}
```

**Para activar el cliente real:** agregar `@Component` a `FinanceModuleClient` y eliminar `@Component` de `MockFinanceModuleClient`.

### Configuración de Reintentos

```java
@Bean("financeRetryTemplate")
public RetryTemplate financeRetryTemplate(...) {
    // Excepciones no reintentables
    Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
    retryableExceptions.put(OrderNotFoundException.class, false);           // 404 — no reintentar
    retryableExceptions.put(PaymentMethodNotRegisteredException.class, false); // 422 — no reintentar

    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(maxAttempts, retryableExceptions, true, true);

    ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
    backOffPolicy.setInitialInterval(initialIntervalMs);  // default: 500ms
    backOffPolicy.setMultiplier(multiplier);              // default: 2.0x
    // Esperas: 500ms → 1000ms → 2000ms
}
```

### Adaptador de Entrada — PaymentMethodController

```java
@GetMapping("/{id_pedido}/forma-pago")
public ResponseEntity<PaymentMethodResponse> consultPaymentMethod(
        @PathVariable("id_pedido") Long orderId) {
    log.info("GET /pedidos/{}/forma-pago", orderId);
    return ResponseEntity.ok(mapper.toResponse(consultPaymentMethodUseCase.consult(orderId)));
}
```

### Mapper — MapStruct

```java
@Mapper(componentModel = "spring")
public interface PaymentMethodMapper {

    @Mapping(source = "idPedido",  target = "orderId")
    @Mapping(source = "formaPago", target = "paymentMethod", qualifiedByName = "parsePaymentMethod")
    // totalPedido se mapea automáticamente por nombre
    OrderPaymentMethod toDomain(FinancePaymentMethodResponse dto);

    // orderId, paymentMethod, totalPedido se mapean automáticamente por nombre
    @Mapping(source = "paymentMethod", target = "paymentMethod", qualifiedByName = "serializePaymentMethod")
    PaymentMethodResponse toResponse(OrderPaymentMethod domain);

    @Named("parsePaymentMethod")
    static PaymentMethod parsePaymentMethod(String value) { return PaymentMethod.fromString(value); }

    @Named("serializePaymentMethod")
    static String serializePaymentMethod(PaymentMethod pm) { return pm.name(); }
}
```

`totalPedido` (nullable `BigDecimal`) fluye automáticamente en ambas direcciones sin configuración adicional — Jackson serializa `null` como `null` en el JSON de respuesta.

---

## 🔄 Flujo de Operación

```
GET /api/v1/pedidos/{id_pedido}/forma-pago
        │
        ▼
┌─────────────────────────────┐
│  PaymentMethodController    │
│  • Extrae orderId del path  │
│  • Delega al use case       │
│  • Mapea domain → DTO       │
│  • Retorna HTTP 200         │
└────────┬────────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│  ConsultPaymentMethodService         │
│  • Delega a FinanceGatewayPort       │
│  • Sin lógica de HTTP ni reintentos  │
└────────┬─────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────┐
│  MockFinanceModuleClient / FinanceModuleClient   │
│                                                  │
│  RetryTemplate (máx 3 intentos)                  │
│  ┌── Intento 1 ─────────────────────────────┐   │
│  │  GET /pedidos/{id}/forma-pago            │   │
│  │  HTTP 200 → mapper.toDomain() → domain   │   │
│  │  HTTP 404 → OrderNotFoundException       │   │
│  │  HTTP 422 → PaymentMethodNotRegistered   │   │
│  │  HTTP 5xx → RuntimeException (reintento) │   │
│  └──────────────────────────────────────────┘   │
│  Si 5xx: backoff 500ms → intento 2              │
│  Si 5xx: backoff 1000ms → intento 3             │
│  Si 5xx × 3: FinanceServiceUnavailableException │
└────────┬─────────────────────────────────────────┘
         │
         ├─ CONTRA_ENTREGA  → { orderId, paymentMethod, totalPedido: 150000.00 }
         └─ CARTERA_COMERCIAL → { orderId, paymentMethod, totalPedido: null }
```

---

## 🌍 API REST

### Configuración Base

- **Host:** `localhost:8080`
- **Base Path:** `/api/v1`
- **Documentación OpenAPI:** `http://localhost:8080/api/v1/docs/openapi.json`
- **Swagger UI:** `http://localhost:8080/api/v1/docs/swagger-ui.html`

### GET /pedidos/{id_pedido}/forma-pago

**Descripción:** Consulta el método de pago y el total a cobrar de un pedido al Módulo Financiero en una sola llamada.

**Response 200 — CONTRA_ENTREGA:**
```json
{
  "orderId": 1,
  "paymentMethod": "CONTRA_ENTREGA",
  "totalPedido": 150000.00
}
```

**Response 200 — CARTERA_COMERCIAL:**
```json
{
  "orderId": 2,
  "paymentMethod": "CARTERA_COMERCIAL",
  "totalPedido": null
}
```

**Response 400 — id no numérico:**
```json
{
  "codigo": "VALIDACION_FALLIDA",
  "mensaje": "El parámetro id_pedido debe ser un número entero positivo válido",
  "timestamp": "2026-04-24T09:29:38"
}
```

**Response 404 — Pedido no encontrado:**
```json
{
  "codigo": "PEDIDO_NO_ENCONTRADO",
  "mensaje": "Pedido no encontrado",
  "timestamp": "2026-04-24T09:29:38"
}
```

**Response 422 — Sin forma de pago:**
```json
{
  "codigo": "FORMA_PAGO_NO_REGISTRADA",
  "mensaje": "El cliente no tiene forma de pago registrada",
  "timestamp": "2026-04-24T09:29:38"
}
```

**Response 503 — Módulo Financiero no disponible:**
```json
{
  "codigo": "SERVICIO_FINANCIERO_NO_DISPONIBLE",
  "mensaje": "El Módulo Financiero no está disponible. Intente nuevamente más tarde.",
  "timestamp": "2026-04-24T09:29:38"
}
```

**cURL de prueba:**
```bash
# CONTRA_ENTREGA
curl http://localhost:8080/api/v1/pedidos/1/forma-pago

# CARTERA_COMERCIAL
curl http://localhost:8080/api/v1/pedidos/2/forma-pago

# Error 404
curl http://localhost:8080/api/v1/pedidos/404/forma-pago

# Error 503
curl http://localhost:8080/api/v1/pedidos/503/forma-pago
```

---

## ⚠️ Manejo de Errores

```java
@RestControllerAdvice(basePackages = "co.edu.unimagdalena.storelogistic.paymentmethod")
public class PaymentMethodExceptionHandler { ... }
```

El `basePackages` garantiza que este manejador no interfiere con los handlers de otros módulos (`route`, `fleet`).

### Tabla de Códigos

| Excepción | HTTP | Código JSON |
|-----------|------|-------------|
| `OrderNotFoundException` | 404 | `PEDIDO_NO_ENCONTRADO` |
| `PaymentMethodNotRegisteredException` | 422 | `FORMA_PAGO_NO_REGISTRADA` |
| `FinanceServiceUnavailableException` | 503 | `SERVICIO_FINANCIERO_NO_DISPONIBLE` |
| `MethodArgumentTypeMismatchException` | 400 | `VALIDACION_FALLIDA` |
| `IllegalArgumentException` | 400 | `VALIDACION_FALLIDA` |

---

## 🧪 Testing

### Estructura de Tests

```
src/test/java/co/edu/unimagdalena/storelogistic/paymentmethod/
│
├── unit/
│   ├── domain/
│   │   ├── models/
│   │   │   └── OrderPaymentMethodTest.java       ✅ 4 casos (CONTRA_ENTREGA, CARTERA_COMERCIAL, nulls)
│   │   └── values/
│   │       └── PaymentMethodTest.java            ✅ 3 casos (fromString válidos e inválido)
│   ├── application/
│   │   └── ConsultPaymentMethodServiceTest.java  ✅ 3 casos (happy path, propagación de errores)
│   └── infrastructure/
│       ├── PaymentMethodMapperTest.java          ✅ 5 casos (toDomain, toResponse, null total, error)
│       └── PaymentMethodControllerTest.java      ✅ 6 casos (200, 404, 422, 503, 400)
│
├── integration/
│   ├── FinanceModuleClientIntegrationTest.java   ✅ 6 casos WireMock (200×2, 404, 422, 503×3, retry)
│   └── PaymentMethodApiContractTest.java         ✅ 7 casos contrato (shape, errores, campos internos)
│
└── testdata/
    └── OrderPaymentMethodFixture.java            # Fixtures: contraEntrega(), carteraComercial(), withId()
```

### Ejemplo — FinanceModuleClientIntegrationTest (WireMock)

```java
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
            .inScenario("retry-success").whenScenarioStateIs("Started")
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("second"));

    wireMock.stubFor(get(urlEqualTo("/pedidos/20/forma-pago"))
            .inScenario("retry-success").whenScenarioStateIs("second")
            .willReturn(okJson("{\"id_pedido\":20,\"forma_pago\":\"CONTRA_ENTREGA\",\"total_pedido\":150000.00}")));

    OrderPaymentMethod result = client.findByOrderId(20L);
    assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.CONTRA_ENTREGA);
    wireMock.verify(2, getRequestedFor(urlEqualTo("/pedidos/20/forma-pago")));
}
```

### Ejemplo — PaymentMethodApiContractTest

```java
@Test
@DisplayName("200 response must NOT expose idPedido or forma_pago (internal field names)")
void contract_successResponse_doesNotExposeInternalFieldNames() throws Exception {
    when(consultPaymentMethodUseCase.consult(1L)).thenReturn(OrderPaymentMethodFixture.contraEntrega());

    mockMvc.perform(get("/pedidos/1/forma-pago").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.idPedido").doesNotExist())
            .andExpect(jsonPath("$.forma_pago").doesNotExist())
            .andExpect(jsonPath("$.id_pedido").doesNotExist());
}
```

### Ejecutar Tests

```bash
# Solo tests de paymentmethod
./gradlew test --tests "*.paymentmethod.*"

# Solo integración WireMock
./gradlew test --tests "*FinanceModuleClientIntegrationTest"

# Todos los tests
./gradlew test

# Con reporte de cobertura
./gradlew test jacocoTestReport
# Reporte en: build/reports/jacoco/test/html/index.html
```

---

## 🚀 Ejecución

### Prerequisitos

1. **Java 21 LTS**
2. **PostgreSQL 17** con base de datos `storeLogistic`
3. **RabbitMQ 3.x** corriendo en `localhost:5672`

### Variables de Entorno

```bash
FINANCE_MODULE_BASE_URL=http://localhost:8082/api/v1   # URL del Módulo Financiero
FINANCE_TIMEOUT_MS=3000                                # Timeout por intento (ms)
FINANCE_RETRY_MAX_ATTEMPTS=3                           # Máximo de reintentos
FINANCE_RETRY_INITIAL_INTERVAL_MS=500                  # Espera inicial backoff (ms)
FINANCE_RETRY_MULTIPLIER=2.0                           # Multiplicador backoff
```

### Ejecución

```bash
./gradlew bootRun
```

### Activar cliente real

Cuando el Módulo Financiero esté disponible:
1. Agregar `@Component` a `FinanceModuleClient`
2. Eliminar `@Component` de `MockFinanceModuleClient`
3. Configurar `FINANCE_MODULE_BASE_URL` con la URL real

---

## 📊 Capacidades Implementadas

✅ Consultar método de pago de un pedido al Módulo Financiero  
✅ Retornar `totalPedido` en una sola llamada (sin doble consulta)  
✅ `totalPedido: null` para CARTERA_COMERCIAL con significado de negocio explícito  
✅ Reintentos con backoff exponencial (máx 3) solo ante errores de infraestructura  
✅ Diferenciación errores de negocio (404/422) vs infraestructura (5xx/timeout)  
✅ Mock intercambiable por cliente real sin modificar dominio ni aplicación  
✅ Testing unitario, de integración (WireMock) y de contrato de API  
✅ Manejador de errores aislado por módulo (`basePackages`)  

---

## 📝 Información del Proyecto

| Atributo | Valor |
|----------|-------|
| **Versión** | 0.0.1-SNAPSHOT |
| **Fecha** | 24 de Abril de 2026 |
| **Java** | 21 LTS |
| **Spring Boot** | 3.5.13 |
| **Rama** | feature/consultar-metodo-pago |

---

**Fin de la Documentación.**