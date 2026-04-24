# Implementation Plan: Consultar Método de Pago de un Pedido

**Date**: April 23, 2026
**Spec**: `@/docs/specs/consult-payment-method/spec.md`

## Summary

Implementar la consulta síncrona del método de pago de un pedido al Módulo Financiero, enviando únicamente el `orderId`. El sistema expone un endpoint interno en el módulo de Logística que delega en un cliente HTTP hacia el Módulo Financiero, normaliza la respuesta a un objeto de dominio (`PaymentMethod`) y aplica reintentos con backoff exponencial ante fallos de conectividad (máximo 3 intentos). Stack: Java 21 + Spring Boot 3.x MVC + Spring Retry + WebClient, como microservicio independiente del módulo de logística.

---

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Web (MVC), Spring Retry, Spring WebClient, Lombok, MapStruct, Gradle, springdoc-openapi
**Storage**: Sin persistencia propia — la consulta es stateless hacia el Módulo Financiero. PostgreSQL disponible si se requiere trazabilidad futura.
**Testing**: JUnit 5, Mockito, AssertJ, TestContainers (WireMock), ArchUnit, Jacoco
**Target Platform**: Backend server (REST API) — microservicio de logística independiente
**Project Type**: Web Application (Backend) — stack bloqueante (no reactivo)
**Architecture**: Hexagonal (Ports & Adapters)
**Performance Goals**: 99% de consultas completadas en menos de 2 segundos incluyendo reintentos (SC-002)
**Constraints**: Máximo 3 reintentos con backoff exponencial; solo `orderId` se envía al Módulo Financiero (FR-001); respuesta retornada intacta sin transformación de datos (SC-003)
**Scale/Scope**: Proceso interno de logística; sin requisito inmediato de escalado horizontal

---

## Project Structure

### Documentation (this feature)

```text
docs/specs/consult-payment-method/
├── plan.md      # Este archivo
└── spec.md      # Especificación de la funcionalidad
```

### Source Code (repository root)

**NOTA IMPORTANTE — Arquitectura Hexagonal Limpia:**
- **domain/**: Contiene ÚNICAMENTE lógica de negocio pura, SIN dependencias de frameworks. Incluye modelos, objetos de valor, puertos (in/out) y excepciones.
- **application/**: Contiene ÚNICAMENTE servicios que coordinan casos de uso. Sin DTOs, sin mappers, sin referencias a infraestructura. Los servicios reciben y retornan objetos de dominio.
- **infrastructure/**: Contiene TODOS los adaptadores, DTOs, controladores, mappers, cliente HTTP y configuración de reintentos.
  - Los DTOs son conceptos de presentación/API y pertenecen exclusivamente a la infraestructura.
  - El mapper (MapStruct) vive en infraestructura: es un detalle de implementación del adaptador web.
  - El controlador es el único punto donde ocurren las traducciones DTOs ↔ objetos de dominio, siempre delegando al mapper.
  - El cliente HTTP (`FinanceModuleClient`) es un adaptador de salida: implementa el puerto de dominio `FinanceGatewayPort` sin que el dominio conozca HTTP.

```text
src/main/java/co/edu/unimagdalena/storelogistic/paymentmethod/
├── domain/
│   ├── models/
│   │   └── OrderPaymentMethod.java    # orderId (Long), paymentMethod (PaymentMethod)
│   ├── values/
│   │   └── PaymentMethod.java         # CONTRA_ENTREGA | CARTERA_COMERCIAL
│   ├── ports/
│   │   ├── in/
│   │   │   └── ConsultPaymentMethodUseCase.java   # OrderPaymentMethod consult(Long orderId)
│   │   └── out/
│   │       └── FinanceGatewayPort.java            # OrderPaymentMethod findByOrderId(Long orderId)
│   └── exceptions/
│       ├── LogisticsException.java                # excepción base — reutilizar del módulo
│       ├── OrderNotFoundException.java            # Módulo Financiero: "Pedido no encontrado"
│       ├── PaymentMethodNotRegisteredException.java  # "El cliente no tiene forma de pago registrada"
│       └── FinanceServiceUnavailableException.java   # timeout / error de conectividad tras reintentos
│
├── application/
│   └── services/
│       └── ConsultPaymentMethodService.java       # implementa ConsultPaymentMethodUseCase
│
└── infrastructure/
    ├── config/
    │   ├── WebClientConfig.java        # bean WebClient con base URL del Módulo Financiero
    │   └── RetryConfig.java            # Spring Retry: maxAttempts=3, backoff exponencial
    ├── client/
    │   └── FinanceModuleClient.java    # implementa FinanceGatewayPort — adapta WebClient a puerto de dominio
    ├── web/
    │   ├── controller/
    │   │   └── PaymentMethodController.java
    │   └── dto/
    │       ├── PaymentMethodResponse.java         # orderId, paymentMethod
    │       └── FinancePaymentMethodResponse.java  # DTO interno para deserializar respuesta del Módulo Financiero
    ├── mapper/
    │   └── PaymentMethodMapper.java   # MapStruct @Component — OrderPaymentMethod ↔ DTOs
    └── exception/
        ├── GlobalExceptionHandler.java   # reutilizar del módulo — agregar nuevas excepciones
        └── ErrorResponse.java            # reutilizar del módulo
```

**Structure Decision**: Arquitectura hexagonal limpia con tres capas (domain, application, infrastructure). El dominio no tiene dependencias de frameworks. El único adaptador de entrada HTTP es `PaymentMethodController`. El único adaptador de salida HTTP es `FinanceModuleClient`. No existen DTOs en `application/` ni en `domain/`.

```text
src/test/java/co/edu/unimagdalena/storelogistic/paymentmethod/
├── unit/
│   ├── domain/
│   │   ├── models/
│   │   │   └── OrderPaymentMethodTest.java
│   │   └── values/
│   │       └── PaymentMethodTest.java
│   ├── application/
│   │   └── ConsultPaymentMethodServiceTest.java
│   └── infrastructure/
│       ├── FinanceModuleClientTest.java
│       ├── PaymentMethodMapperTest.java
│       └── PaymentMethodControllerTest.java
├── integration/
│   ├── FinanceModuleClientIntegrationTest.java    # WireMock — simula respuestas del Módulo Financiero
│   └── ConsultPaymentMethodServiceIntegrationTest.java
├── contract/
│   └── PaymentMethodApiContractTest.java
└── testdata/
    ├── fixtures/
    │   └── OrderPaymentMethodFixture.java
    ├── builders/
    │   └── OrderPaymentMethodBuilder.java
    └── wiremock/
        └── FinanceModuleWireMock.java   # stubs: 200 CONTRA_ENTREGA, 200 CARTERA_COMERCIAL, 404, 503
```

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Configurar el proyecto Gradle, dependencias y herramientas de calidad antes de cualquier implementación de lógica de negocio.

- [ ] T001 Crear la estructura de directorios siguiendo el layout definido en este plan.
- [ ] T002 Verificar/actualizar `build.gradle.kts` — incluir dependencias: `spring-boot-starter-web`, `spring-boot-starter-webflux` (WebClient), `spring-retry`, `spring-boot-starter-aop` (requerido por Spring Retry), `lombok`, `mapstruct`, `springdoc-openapi-starter-webmvc-ui`, `spring-boot-starter-test`, `wiremock-standalone`, `archunit`, `jacoco`. Verificar que no estén ya presentes desde módulos anteriores.
- [ ] T003 Verificar/actualizar `application.yml` — agregar propiedades: `finance.module.base-url`, `finance.module.retry.max-attempts=3`, `finance.module.retry.initial-interval-ms`, `finance.module.retry.multiplier`, `finance.module.timeout-ms`. Perfiles `dev`, `test`, `prod`.
- [ ] T004 Configurar `ArchUnit` — verificar reglas de capas existentes: `domain/` sin imports de Spring/JPA/web; `application/` sin imports de `infrastructure/`. Agregar regla: los servicios de `application/` no deben importar desde `infrastructure/`.
- [ ] T005 Configurar Jacoco — objetivo ≥80% global, 100% capa de dominio.
- [ ] T006 Configurar `WireMock` en `FinanceModuleWireMock.java` — stubs base: respuesta 200 con `CONTRA_ENTREGA`, respuesta 200 con `CARTERA_COMERCIAL`, respuesta 404 "Pedido no encontrado", respuesta 503 para simulación de timeout/indisponibilidad.

**Checkpoint**: El proyecto compila, las dependencias resuelven sin conflictos, WireMock inicia correctamente en tests.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Crear todos los componentes base de dominio e infraestructura antes de implementar cualquier escenario. Aún no hay lógica de negocio — solo la fundación compartida.

**⚠️ CRITICAL**: No scenario implementation should begin until this phase is complete.

- [ ] T007 Crear `PaymentMethod.java` — enum: `CONTRA_ENTREGA | CARTERA_COMERCIAL`. Sin anotaciones de framework. Método `static fromString(String value): PaymentMethod` — retorna el valor correspondiente; lanza `IllegalArgumentException` si el valor no es reconocido.
- [ ] T008 Crear `OrderPaymentMethod.java` — modelo de dominio: `orderId (Long)`, `paymentMethod (PaymentMethod)`, `totalPedido (BigDecimal, nullable)`. Inmutable (sin setters). Sin anotaciones JPA ni de Spring. Constructor canónico y método de fábrica `static of(Long orderId, PaymentMethod paymentMethod, BigDecimal totalPedido): OrderPaymentMethod`. `totalPedido` es null para CARTERA_COMERCIAL y tiene valor para CONTRA_ENTREGA.
- [ ] T009 Crear `LogisticsException.java` — excepción base runtime. Reutilizar del módulo si ya existe.
- [ ] T010 Crear `OrderNotFoundException.java` — extiende `LogisticsException`. Mensaje: `"Pedido no encontrado"` (coincide exactamente con la respuesta del Módulo Financiero según la spec).
- [ ] T011 Crear `PaymentMethodNotRegisteredException.java` — extiende `LogisticsException`. Mensaje: `"El cliente no tiene forma de pago registrada"`.
- [ ] T012 Crear `FinanceServiceUnavailableException.java` — extiende `LogisticsException`. Se lanza cuando se agotan los 3 reintentos sin respuesta exitosa del Módulo Financiero.
- [ ] T013 Crear puerto de entrada `ConsultPaymentMethodUseCase.java` en `domain/ports/in/` — firma: `OrderPaymentMethod consult(Long orderId)`. Recibe y retorna exclusivamente objetos de dominio.
- [ ] T014 Crear puerto de salida `FinanceGatewayPort.java` en `domain/ports/out/` — firma: `OrderPaymentMethod findByOrderId(Long orderId)`. El dominio define la abstracción; la infraestructura la implementa.
- [ ] T015 Crear `FinancePaymentMethodResponse.java` en `infrastructure/web/dto/` — DTO para deserializar la respuesta JSON del Módulo Financiero: `Long id_pedido`, `String forma_pago`, `BigDecimal total_pedido` (nullable). Solo vive en infraestructura.
- [ ] T016 Crear `PaymentMethodResponse.java` en `infrastructure/web/dto/` — DTO de respuesta HTTP del endpoint interno: `Long orderId`, `String paymentMethod`, `BigDecimal totalPedido` (nullable). Solo vive en infraestructura.
- [ ] T017 Crear `PaymentMethodMapper.java` en `infrastructure/mapper/` como MapStruct `@Component` — conversiones: `OrderPaymentMethod → PaymentMethodResponse`, `FinancePaymentMethodResponse → OrderPaymentMethod`.
- [ ] T018 Crear `GlobalExceptionHandler.java` — reutilizar del módulo, agregar mappings:
  - `OrderNotFoundException` → HTTP 404 con mensaje de la excepción.
  - `PaymentMethodNotRegisteredException` → HTTP 422 Unprocessable Entity.
  - `FinanceServiceUnavailableException` → HTTP 503 Service Unavailable.
  - Violaciones de `@Valid` → HTTP 400.
- [ ] T019 Crear `OrderPaymentMethodFixture.java` — instancias de prueba para `CONTRA_ENTREGA`, `CARTERA_COMERCIAL` y escenarios de error.
- [ ] T020 Pruebas unitarias para `PaymentMethod.fromString()` — `"CONTRA_ENTREGA"` → enum correcto, `"CARTERA_COMERCIAL"` → enum correcto, valor desconocido → `IllegalArgumentException`.
- [ ] T021 Pruebas unitarias para `OrderPaymentMethod.of()` — construcción correcta con ambos tipos de pago; `orderId` nulo → excepción esperada.

**Checkpoint**: Objetos de valor, modelo de dominio, puertos, excepciones, DTOs, mapper y manejo de errores listos. T020–T021 pasan. El proyecto compila sin errores. Aún no hay endpoint funcional.

---

## Phase 3: Scenario 1 — Query returns CONTRA_ENTREGA (P1)

**Goal**: When `GET /api/v1/pedidos/{id_pedido}/forma-pago` is called for an order whose `forma_pago` is `CONTRA_ENTREGA`, the system returns `{orderId, paymentMethod: "CONTRA_ENTREGA"}` with HTTP 200 (FR-001, FR-002, SC-001, SC-003).

**Independent Test**: Given an order with `forma_pago = CONTRA_ENTREGA` stubbed in WireMock, `GET /api/v1/pedidos/{id_pedido}/forma-pago` returns HTTP 200 with `orderId` and `paymentMethod: "CONTRA_ENTREGA"`.

### Tests for Scenario 1

- [ ] T022 [P] [SC1] Contract test in `PaymentMethodApiContractTest` — `GET /api/v1/pedidos/{id_pedido}/forma-pago` with a valid `orderId` whose stub returns `CONTRA_ENTREGA` → HTTP 200, body contains `orderId`, `paymentMethod: "CONTRA_ENTREGA"` y `totalPedido: 150000.00` (FR-001, FR-002, SC-001, SC-003).
- [ ] T023 [P] [SC1] Integration test in `FinanceModuleClientIntegrationTest` — WireMock stubs `GET /api/v1/pedidos/1/forma-pago` → 200 `{"id_pedido":1,"forma_pago":"CONTRA_ENTREGA","total_pedido":150000.00}`; verify `FinanceModuleClient.findByOrderId(1L)` returns `OrderPaymentMethod` with `paymentMethod = CONTRA_ENTREGA` y `totalPedido = 150000.00`.

### Implementation for Scenario 1

- [ ] T024 [SC1] Crear `WebClientConfig.java` en `infrastructure/config/` — bean `WebClient` con `baseUrl` leída de `finance.module.base-url`. Sin lógica de negocio.
- [ ] T025 [SC1] Crear `RetryConfig.java` en `infrastructure/config/` — habilitar `@EnableRetry`. Definir bean `RetryTemplate` con `maxAttempts=3`, backoff exponencial con `initialInterval` y `multiplier` leídos de `application.yml` (FR-004).
- [ ] T026 [SC1] Implementar `FinanceModuleClient.java` implementando `FinanceGatewayPort`:
  - Inyecta `WebClient` y `RetryTemplate`.
  - `findByOrderId(Long orderId)` — invoca `GET /api/v1/pedidos/{orderId}/forma-pago` usando `WebClient`.
  - Mapea la respuesta `FinancePaymentMethodResponse` → `OrderPaymentMethod` vía `PaymentMethodMapper`.
  - Ante HTTP 404: lanza `OrderNotFoundException`.
  - Ante HTTP 422 / cuerpo con error de pago no registrado: lanza `PaymentMethodNotRegisteredException`.
  - Ante timeout o 5xx: `RetryTemplate` reintenta hasta 3 veces; si se agotan, lanza `FinanceServiceUnavailableException` (FR-004).
- [ ] T027 [SC1] Implementar `ConsultPaymentMethodService.java` implementando `ConsultPaymentMethodUseCase`:
  - Inyecta `FinanceGatewayPort` (nunca `FinanceModuleClient` directamente — depende de la abstracción, no de la implementación — SRP/DIP).
  - `consult(Long orderId)` — delega en `FinanceGatewayPort.findByOrderId(orderId)` y retorna `OrderPaymentMethod`. Sin lógica de transformación ni de HTTP.
- [ ] T028 [SC1] Crear `PaymentMethodController.java` con `GET /api/v1/pedidos/{id_pedido}/forma-pago`:
  - Extrae `id_pedido` como `@PathVariable Long orderId`.
  - Llama a `ConsultPaymentMethodUseCase.consult(orderId)`.
  - Mapea `OrderPaymentMethod → PaymentMethodResponse` usando `PaymentMethodMapper`.
  - Retorna HTTP 200.
- [ ] T029 [SC1] Unit tests for `ConsultPaymentMethodService`:
  - Happy path: `FinanceGatewayPort` mockeado devuelve `OrderPaymentMethod` con `CONTRA_ENTREGA`; verificar que el servicio retorna el mismo objeto sin transformación.
  - `OrderNotFoundException` se propaga sin ser capturada ni envuelta.
  - `FinanceServiceUnavailableException` se propaga sin ser capturada ni envuelta.
- [ ] T030 [SC1] Unit tests for `PaymentMethodController` con MockMvc:
  - `GET /api/v1/pedidos/1/forma-pago` con `ConsultPaymentMethodUseCase` mockeado devolviendo `CONTRA_ENTREGA` → HTTP 200, body `{"orderId":1,"paymentMethod":"CONTRA_ENTREGA"}`.
  - `orderId` no numérico en path → HTTP 400.

**Checkpoint**: `GET /api/v1/pedidos/{id_pedido}/forma-pago` retorna `CONTRA_ENTREGA` de extremo a extremo. T022–T023 pasan con WireMock.

---

## Phase 4: Scenario 2 — Query returns CARTERA_COMERCIAL (P1)

**Goal**: When the same endpoint is called for an order whose `forma_pago` is `CARTERA_COMERCIAL`, the system returns `{orderId, paymentMethod: "CARTERA_COMERCIAL"}` with HTTP 200 (FR-001, FR-002, SC-001, SC-003).

**Independent Test**: Given an order with `forma_pago = CARTERA_COMERCIAL` stubbed in WireMock, `GET /api/v1/pedidos/{id_pedido}/forma-pago` returns HTTP 200 with `orderId` and `paymentMethod: "CARTERA_COMERCIAL"`.

### Tests for Scenario 2

- [ ] T031 [P] [SC2] Contract test in `PaymentMethodApiContractTest` — `GET /api/v1/pedidos/{id_pedido}/forma-pago` with stub returning `CARTERA_COMERCIAL` → HTTP 200, body contains `orderId`, `paymentMethod: "CARTERA_COMERCIAL"` y `totalPedido: null` (FR-002, SC-001, SC-003).
- [ ] T032 [P] [SC2] Integration test in `FinanceModuleClientIntegrationTest` — WireMock stubs `GET /api/v1/pedidos/2/forma-pago` → 200 `{"id_pedido":2,"forma_pago":"CARTERA_COMERCIAL","total_pedido":null}`; verify `FinanceModuleClient.findByOrderId(2L)` returns `OrderPaymentMethod` with `paymentMethod = CARTERA_COMERCIAL` y `totalPedido = null`.

### Implementation for Scenario 2

- [ ] T033 [SC2] Verificar que `FinanceModuleClient.findByOrderId()` implementado en T026 maneja `CARTERA_COMERCIAL` correctamente — el `PaymentMethod.fromString()` debe reconocer ambos valores del enum. No se requiere nueva implementación si T026 es genérico.
- [ ] T034 [SC2] Unit tests for `ConsultPaymentMethodService`:
  - `FinanceGatewayPort` mockeado devuelve `OrderPaymentMethod` con `CARTERA_COMERCIAL`; verificar que el servicio retorna el mismo objeto sin transformación.
- [ ] T035 [SC2] Unit tests for `PaymentMethodMapper`:
  - `FinancePaymentMethodResponse("2", "CARTERA_COMERCIAL")` → `OrderPaymentMethod(2L, CARTERA_COMERCIAL)`.
  - `OrderPaymentMethod(2L, CARTERA_COMERCIAL)` → `PaymentMethodResponse(2L, "CARTERA_COMERCIAL")`.
  - `forma_pago` con valor inesperado → mapeo lanza `IllegalArgumentException`.

**Checkpoint**: Ambos valores de `PaymentMethod` funcionan de extremo a extremo. T031–T032 pasan con WireMock. SC1 y SC2 verificados de forma independiente.

---

## Phase 5: Scenario 3 — Error when order does not exist (P1)

**Goal**: When the Finance Module returns a 404 for a non-existent `orderId`, the system propagates the error as HTTP 404 with message `"Pedido no encontrado"` (FR-003, SC-001).

**Independent Test**: Given a WireMock stub that returns 404 for a given `orderId`, `GET /api/v1/pedidos/{id_pedido}/forma-pago` returns HTTP 404 with body containing `"Pedido no encontrado"`.

### Tests for Scenario 3

- [ ] T036 [P] [SC3] Contract test in `PaymentMethodApiContractTest` — `GET /api/v1/pedidos/999/forma-pago` with WireMock stub returning 404 → HTTP 404, body contains `"Pedido no encontrado"` (FR-003, SC-001).
- [ ] T037 [P] [SC3] Integration test in `FinanceModuleClientIntegrationTest` — WireMock stubs `GET /api/v1/pedidos/999/forma-pago` → 404; verify `FinanceModuleClient.findByOrderId(999L)` throws `OrderNotFoundException` with message `"Pedido no encontrado"`.

### Implementation for Scenario 3

- [ ] T038 [SC3] Verificar que `FinanceModuleClient.findByOrderId()` implementado en T026 lanza `OrderNotFoundException` ante HTTP 404. No se requiere nueva implementación si T026 es correcto.
- [ ] T039 [SC3] Verificar que `GlobalExceptionHandler` implementado en T018 mapea `OrderNotFoundException` → HTTP 404 con el mensaje exacto de la excepción.
- [ ] T040 [SC3] Unit tests for `FinanceModuleClient`:
  - HTTP 404 del Módulo Financiero → `OrderNotFoundException` con mensaje `"Pedido no encontrado"`.
  - La excepción no dispara reintentos (los reintentos aplican solo a errores de conectividad/5xx).

**Checkpoint**: Los 3 escenarios de la spec funcionan de extremo a extremo. T036–T037 pasan con WireMock.

---

## Phase 6: Edge Cases

**Purpose**: Cubrir los casos borde explícitos definidos en la spec.

- [ ] T041 [EC] El cliente no tiene forma de pago registrada — WireMock stub retorna un error específico del Módulo Financiero (ej. HTTP 422 o cuerpo con mensaje `"El cliente no tiene forma de pago registrada"`); `FinanceModuleClient` lanza `PaymentMethodNotRegisteredException`; `GlobalExceptionHandler` retorna HTTP 422 con el mensaje exacto. Contract test: `GET /api/v1/pedidos/{id_pedido}/forma-pago` → HTTP 422 `"El cliente no tiene forma de pago registrada"` (edge case de spec).
- [ ] T042 [EC] Módulo Financiero no disponible — WireMock stub retorna 503 en los 3 intentos; verificar que `RetryTemplate` reintenta exactamente 3 veces con backoff exponencial; tras agotar reintentos, `FinanceModuleClient` lanza `FinanceServiceUnavailableException`; `GlobalExceptionHandler` retorna HTTP 503. Integration test con `FinanceModuleClientIntegrationTest` verificando que WireMock recibe exactamente 3 llamadas (FR-004, edge case de spec).
- [ ] T043 [EC] Timeout en el Módulo Financiero — WireMock stub introduce delay > `finance.module.timeout-ms`; verificar que el timeout dispara el mecanismo de reintento; tras 3 intentos fallidos, se lanza `FinanceServiceUnavailableException`. Integration test: verificar que el total de tiempo no supera 2 segundos (SC-002) si el timeout por intento está correctamente configurado.
- [ ] T044 [EC] Reintento exitoso en segundo intento — WireMock stub retorna 503 en el primer intento y 200 en el segundo; verificar que el sistema retorna la respuesta exitosa sin propagar el error (FR-004 — resiliencia, no solo fallo total).
- [ ] T045 [EC] `orderId` con valor inválido en path (no numérico, negativo, cero) — `PaymentMethodController` con `@PathVariable` no numérico → HTTP 400 sin llamar al servicio. Prueba unitaria con MockMvc.

**Checkpoint**: Todos los casos borde de la spec cubiertos con pruebas automatizadas.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Hardening, observabilidad, validaciones y documentación.

- [ ] T046 Logging estratégico con `@Slf4j` en `ConsultPaymentMethodService` y `FinanceModuleClient`:
  - `INFO` al inicio de cada consulta (`orderId` recibido).
  - `INFO` en respuesta exitosa (`orderId`, `paymentMethod` retornado).
  - `WARN` en cada reintento (intento número N de 3, causa del fallo).
  - `ERROR` al agotar reintentos (`orderId`, cantidad de intentos, última excepción).
  - `WARN` cuando el Módulo Financiero retorna `OrderNotFoundException` o `PaymentMethodNotRegisteredException` (errores esperados de negocio).
- [ ] T047 Documentar API con `springdoc-openapi` — endpoint `GET /api/v1/pedidos/{id_pedido}/forma-pago`, esquemas de respuesta, todos los códigos HTTP posibles: 200, 400, 404, 422, 503.
- [ ] T048 `@ArchTest` con ArchUnit — verificar:
  - `domain/` sin imports de Spring, JPA, WebClient ni web.
  - `application/` puede importar `domain/`, no `infrastructure/`.
  - `infrastructure/` puede importar todo.
  - Ninguna clase en `domain/` o `application/` importa desde `infrastructure/`.
  - `ConsultPaymentMethodService` solo depende de `FinanceGatewayPort` (puerto), nunca de `FinanceModuleClient` (adaptador).
- [ ] T049 Medir latencias con prueba de integración: 10 llamadas consecutivas a WireMock con respuesta inmediata → verificar que el percentil 99 está bajo los 2 segundos (SC-002).
- [ ] T050 Configurar `/actuator/health` incluyendo verificación de conectividad al Módulo Financiero (health indicator personalizado o uso de `WebClient` probe).
- [ ] T051 Cobertura de código con Jacoco — verificar ≥80% global, 100% capa de dominio.
- [ ] T052 README con: comandos de compilación/ejecución, ejecución de pruebas (unitarias / integración / todas), ejemplo curl para el endpoint, variables de entorno necesarias (`FINANCE_MODULE_BASE_URL`, etc.).
- [ ] T053 Checklist pre-despliegue: 100% pruebas pasando, ArchUnit pasando, cobertura ≥80%, sin vulnerabilidades en dependencias, propiedades de retry configuradas por entorno.

**Checkpoint**: Código listo para producción. SC-001 a SC-003 verificables con pruebas automatizadas.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias — puede comenzar de inmediato.
- **Foundational (Phase 2)**: Depende de la Fase 1 — **bloquea todos los escenarios**.
- **Scenario 1 (Phase 3)**: Depende de la Fase 2. Implementa el cliente HTTP, el servicio y el controlador — base para los demás escenarios.
- **Scenario 2 (Phase 4)**: Depende de la Fase 3 (reutiliza los mismos componentes). En solitario: implementar después de la Fase 3.
- **Scenario 3 (Phase 5)**: Depende de la Fase 3 (`FinanceModuleClient` debe existir). Extiende el manejo de errores, no reemplaza la implementación.
- **Edge Cases (Phase 6)**: Depende de que los 3 escenarios estén completos.
- **Polish (Phase 7)**: Depende de la Fase 6.

### Within Each Phase

- Objetos de valor y modelos de dominio → Puertos (interfaces) → Adaptadores (cliente HTTP) → Servicio de aplicación → Controlador.
- Pruebas de contrato e integración (`[P]`) antes de la implementación correspondiente.
- Pruebas unitarias en línea con cada componente.
- Checkpoint al final antes de pasar a la siguiente fase.

---

## Notes

- **DTOs exclusivamente en infraestructura**: `PaymentMethodResponse` y `FinancePaymentMethodResponse` viven en `infrastructure/web/dto/`. Nunca en `application/` ni en `domain/`.
- **Mapper exclusivamente en infraestructura**: `PaymentMethodMapper` vive en `infrastructure/mapper/`. El controlador es el único invocador para mapear `OrderPaymentMethod → PaymentMethodResponse`.
- **El servicio opera únicamente con objetos de dominio**: `ConsultPaymentMethodService` recibe `Long orderId` y retorna `OrderPaymentMethod`. No tiene conocimiento de `PaymentMethodResponse`, `FinancePaymentMethodResponse` ni de ningún detalle HTTP.
- **La lógica de deserialización y manejo de errores HTTP vive en el adaptador de salida**: `FinanceModuleClient` es la única clase que conoce los códigos HTTP del Módulo Financiero, los DTOs de respuesta externa y la estrategia de reintentos. Ninguna otra clase duplica esta lógica.
- **`PaymentMethod.fromString()` es la única fuente de verdad para validar valores de pago** (FR-002). Ni el mapper ni el cliente HTTP duplican esta lógica.
- **Los reintentos aplican exclusivamente a errores de conectividad (5xx, timeout)**, no a errores de negocio (404, 422). `FinanceModuleClient` debe diferenciar explícitamente ambos casos antes de aplicar el retry.
- **`ConsultPaymentMethodService` depende de `FinanceGatewayPort`, nunca de `FinanceModuleClient`** — garantiza el principio de inversión de dependencias (DIP): la capa de aplicación depende de abstracciones, no de implementaciones.
- La etiqueta `[P]` marca las pruebas a escribir antes de la implementación (test-first).
- Las etiquetas `[SC1]`, `[SC2]`, `[SC3]`, `[EC]` mapean directamente a los escenarios de la spec para trazabilidad.
- Hacer commit después de cada tarea completada con pruebas en verde.
