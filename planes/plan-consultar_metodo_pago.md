# Implementation Plan: Consultar Método de Pago de un Pedido

**Date**: April 11, 2026
**Spec**: spec-Consultar_metodo_pago.md

## Summary

El feature permite que el módulo de Logística consulte de forma síncrona el método de pago de un pedido al Módulo Financiero usando únicamente `idPedido`. La solución implementará un endpoint interno en Logística que llama al servicio financiero, maneja errores de pedido no encontrado, forma de pago no registrada y aplica reintentos con backoff exponencial para fallos de conectividad.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Web (MVC), Spring Retry, Spring Cloud OpenFeign o WebClient, Hibernate, Flyway, Lombok, Gradle
**Storage**: PostgreSQL (para pedidos y trazabilidad local cuando aplique)
**Testing**: JUnit 5, Mockito, AssertJ, TestContainers, Spring MockMVC
**Target Platform**: Backend server (REST API) — microservicio de logística
**Project Type**: Web Application (Backend) — stack bloqueante
**Architecture**: Hexagonal (Ports & Adapters)
**Performance Goals**: 99% de consultas completadas en menos de 2 segundos con reintentos
**Constraints**: Reintentos hasta 3 veces con backoff exponencial; solo datos de `idPedido` se envían al módulo financiero
**Scale/Scope**: API de logística con integración al módulo financiero, prioridad P1

## Project Structure

### Documentation (this feature)

```text
docs/specs/consultar_metodo_pago.md/
├── plan.md              # This file 
└── spec.md             # Feature specification
```

### Source Code (repository root)

```text
src/main/java/com/logistics/payment/method/
├── domain/
│   ├── models/
│   │   └── Order.java
│   ├── values/
│   │   └── PaymentMethod.java        # CONTRA_ENTREGA | CARTERA_COMERCIAL
│   └── exceptions/
│       ├── OrderNotFoundException.java
│       ├── PaymentMethodNotFoundException.java
│       └── FinanceServiceUnavailableException.java
│
├── application/
│   ├── dto/
│   │   ├── PaymentMethodResponse.java
│   │   └── ConsultPaymentMethodRequest.java
│   ├── services/
│   │   └── ConsultPaymentMethodService.java
│   ├── ports/
│   │   ├── in/
│   │   │   └── ConsultPaymentMethodUseCase.java
│   │   └── out/
│   │       └── FinanceClientPort.java
│   └── mapper/
│       └── PaymentMethodMapper.java
│
└── infrastructure/
    ├── config/
    │   └── RetryConfig.java
    ├── client/
    │   └── FinanceModuleClient.java
    ├── web/
    │   └── controller/
    │       └── PaymentMethodController.java
    └── exception/
        └── GlobalExceptionHandler.java

src/test/java/com/logistics/payment/method/
├── unit/
│   ├── domain/
│   │   └── exceptions/
│   │       ├── OrderNotFoundExceptionTest.java
│   │       └── PaymentMethodNotFoundExceptionTest.java
│   ├── application/
│   │   ├── ConsultPaymentMethodServiceTest.java
│   │   └── PaymentMethodMapperTest.java
│   └── infrastructure/
│       ├── FinanceModuleClientTest.java
│       └── PaymentMethodControllerTest.java
├── integration/
│   └── ConsultPaymentMethodIntegrationTest.java
├── contract/
│   └── PaymentMethodApiContractTest.java
└── testdata/
    ├── fixtures/
    │   ├── OrderFixture.java
    │   └── PaymentMethodFixture.java
    ├── builders/
    │   └── ConsultPaymentMethodRequestBuilder.java
    └── containers/
        └── PostgreSQLContainer.java
```

**Structure Decision**: Single project using Hexagonal Architecture; plus an external HTTP client adapter for the Financiero module.


<!-- 
  ============================================================================
  IMPORTANT: The tasks below are SAMPLE TASKS for illustration purposes only.
  
  You MUST replace these with actual tasks based on:
  - User stories from spec.md
  - Feature requirements from this file
  - Entities required for the use case
  - Endpoints required
  
  DO NOT keep these sample tasks.
  ============================================================================
-->

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and common dependencies

- [ ] T001 Create project structure with domain/application/infrastructure layers
- [ ] T002 Initialize Java 21 Gradle project with Spring Boot 3.x
- [ ] T003 Add Spring Web, Spring Retry, Lombok, Flyway, PostgreSQL driver
- [ ] T004 Configure code formatting and linting (Spotless, Checkstyle)
- [ ] T005 Add base integration test support (TestContainers, MockMVC)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core integration and resiliency support before feature work

**⚠️ CRITICAL**: No feature work should begin until this phase is complete

- [ ] T006 Configure PostgreSQL datasource and Flyway migrations
- [ ] T007 Define domain model for Order and PaymentMethod
- [ ] T008 Implement global exception handling and error mapping
- [ ] T009 Configure HTTP client adapter for Financiero module
- [ ] T010 Setup retry/backoff policy for remote calls
- [ ] T011 Define request/response contract for payment method query

**Checkpoint**: Foundational infrastructure ready for the payment method query feature

---

## Phase 3: User Story 1 - Consultar Forma de Pago (Priority: P1)

**Goal**: Enable Logística to query the payment method of a pedido from Financiero using only `idPedido`.

**Independent Test**: Call the endpoint with a valid order id and receive `id_pedido` plus `forma_pago`, or a valid error if the order is missing or not registered.

### Tests for User Story 1

- [ ] T012 Contract test for GET /api/v1/pedidos/{id_pedido}/forma-pago in src/test/java/com/logistics/payment/method/contract/PaymentMethodApiContractTest.java
- [ ] T013 Integration test for remote Financiero query with retries in src/test/java/com/logistics/payment/method/integration/ConsultPaymentMethodIntegrationTest.java

### Implementation for User Story 1

- [ ] T014 Implement `PaymentMethodController` at src/main/java/com/logistics/payment/method/infrastructure/web/controller/PaymentMethodController.java
- [ ] T015 Implement `ConsultPaymentMethodUseCase` in src/main/java/com/logistics/payment/method/application/ports/in/ConsultPaymentMethodUseCase.java
- [ ] T016 Implement `ConsultPaymentMethodService` in src/main/java/com/logistics/payment/method/application/services/ConsultPaymentMethodService.java
- [ ] T017 Implement `FinanceClientPort` and `FinanceModuleClient` adapter
- [ ] T018 Add validation for `idPedido` and error handling for missing order or missing payment method
- [ ] T019 Implement retry/backoff logic for Financiero connectivity failures
- [ ] T020 Map the external response to `PaymentMethodResponse` and normalize `CONTRA_ENTREGA` / `CARTERA_COMERCIAL`

**Checkpoint**: Feature complete and testable for payment method consultation



---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Refinar la implementación, garantizar calidad y asegurar la convergencia con la especificación.

- [ ] T021 Actualizar la documentación del feature en `docs/specs/consultar_metodo_pago.md` para reflejar los escenarios de consulta y las respuestas esperadas.
- [ ] T022 Verificar y reforzar los mensajes de error para:
  - "Pedido no encontrado"
  - "El cliente no tiene forma de pago registrada"
- [ ] T023 Añadir pruebas adicionales para reintentos con backoff exponencial frente a timeout o indisponibilidad de Financiero.
- [ ] T024 Realizar limpieza de código y refactorización en dominio, adaptadores y controladores.
- [ ] T025 Validar que todas las respuestas exitosas incluyen `id_pedido` y `forma_pago` exactamente como lo retorna el Módulo Financiero.
- [ ] T026 Medir latencias de la consulta y ajustar para cumplir el objetivo de 99% de consultas en menos de 2 segundos.
- [ ] T027 Revisar validaciones de entrada y seguridad de endpoint para evitar idPedido inválido o ataques de inyección.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies, can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion and blocks feature implementation
- **Feature Implementation (Phase 3)**: Depends on Foundational completion
- **Polish (Phase N)**: Depends on Feature Implementation completion

### Within This Feature

- Define domain and contract before implementing service adapters
- Implement controller last, after use case and service are stable
- Validate error conditions and retries before final polish

### Success Criteria

- Logística consulta el método de pago correcto (`CONTRA_ENTREGA` / `CARTERA_COMERCIAL`) para pedidos válidos
- El sistema maneja `Pedido no encontrado` y `El cliente no tiene forma de pago registrada`
- El módulo reintenta hasta 3 veces con backoff exponencial en caso de timeout o indisponibilidad
- Las respuestas exitosas incluyen `id_pedido` y `forma_pago` exactamente como retorna Financiero

