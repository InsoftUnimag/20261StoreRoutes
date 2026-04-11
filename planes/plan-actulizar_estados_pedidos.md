# Implementation Plan: Actualizar Estado de Pedidos en Entrega

**Date**: April 11, 2026 
**Spec**: spec-actualizar-estados-pedidos.md

## Summary

Como transportista, necesito registrar el estado final de cada pedido durante su entrega junto con la tasa de efectividad, para generar información confiable de liquidación. El enfoque técnico es implementar un microservicio backend independiente utilizando arquitectura hexagonal con Spring Boot 3.x, JPA para persistencia en PostgreSQL, y validaciones para asegurar integridad de datos.

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: Java 21  
**Primary Dependencies**: Spring Boot 3.x, Spring Web (MVC), Spring Data JPA, Hibernate, Flyway, MapStruct, Lombok, Gradle
**Storage**: PostgreSQL  
**Testing**: JUnit 5, Mockito, AssertJ, TestContainers, ArchUnit, Jacoco  
**Target Platform**: Backend server (REST API) — microservicio independiente de logística
**Project Type**: Web Application (Backend) — stack bloqueante (no reactivo)
**Architecture**: Hexagonal (Ports & Adapters)  
**Performance Goals**: [domain-specific, e.g., 1000 req/s, 10k lines/sec, 60 fps or NEEDS CLARIFICATION]  
**Constraints**: [domain-specific, e.g., <200ms p95, <100MB memory, offline-capable or NEEDS CLARIFICATION]  
**Scale/Scope**: [domain-specific, e.g., 10k users, 1M LOC, 50 screens or NEEDS CLARIFICATION]

## Project Structure

### Documentation (this feature)

```text
/
├── plan-actualizar-estados-pedidos.md    # This file 
└── spec-actualizar-estados-pedidos.md   # Feature specification
```

### Source Code (repository root)


```text
# [REMOVE IF UNUSED] Option 1: Single project (DEFAULT)
src/main/java/com/logistics/order/status/
├── domain/
│   ├── models/
│   │   ├── Order.java
│   │   ├── Carrier.java
│   │   └── Alert.java
│   ├── values/
│   │   ├── FinalStatus.java       # enum: ENTREGADO_COMPLETO, RECHAZO_PARCIAL, NO_ENTREGADO, DEVOLUCION_ERROR_EMPRESA, FALTANTE_INVENTARIO
│   │   └── EffectivenessRate.java    # value object with validation -100 to 100 
│   └── exceptions/
│       ├── InvalidStatusException.java
│       ├── OrderNotFoundException.java
│       ├── InvalidEffectivenessRateException.java
│       └── OrderStatusException.java
│
├── application/
│   ├── dto/
│   │   ├── UpdateOrderStatusRequest.java
│   │   ├── UpdateOrderStatusResponse.java
│   │   └── OrderDTO.java
│   ├── services/
│   │   └── UpdateOrderStatusService.java
│   ├── ports/
│   │   ├── in/
│   │   │   └── UpdateOrderStatusUseCase.java
│   │   └── out/
│   │       ├── OrderRepository.java
│   │       ├── CarrierRepository.java
│   │       └── AlertRepository.java
│   └── mapper/
│       └── OrderMapper.java
│
└── infrastructure/
    ├── config/
    │   └── JpaConfig.java
    ├── persistence/
    │   ├── jpa/
    │   │   ├── OrderJpaEntity.java
    │   │   ├── CarrierJpaEntity.java
    │   │   └── AlertJpaEntity.java
    │   ├── repository/
    │   │   ├── OrderRepositoryAdapter.java
    │   │   ├── CarrierRepositoryAdapter.java
    │   │   └── AlertRepositoryAdapter.java
    │   ├── jparepository/
    │       ├── OrderSpringRepository.java
    │       ├── CarrierSpringRepository.java
    │       └── AlertSpringRepository.java
    ├── web/
    │   └── controller/
    │       └── OrderStatusController.java
    └── exception/
        └── GlobalExceptionHandler.java

src/test/java/com/logistics/order/status/
├── unit/
│   ├── domain/
│   │   ├── models/
│   │   │   ├── OrderTest.java
│   │   │   ├── CarrierTest.java
│   │   │   └── AlertTest.java
│   │   └── values/
│   │       ├── FinalStatusTest.java
│   │       └── EffectivenessRateTest.java
│   ├── application/
│   │   ├── UpdateOrderStatusServiceTest.java
│   │   └── OrderMapperTest.java
│   └── infrastructure/
│       ├── OrderRepositoryAdapterTest.java
│       ├── CarrierRepositoryAdapterTest.java
│       └── OrderStatusControllerTest.java
├── integration/
│   ├── OrderRepositoryIntegrationTest.java
│   ├── CarrierRepositoryIntegrationTest.java
│   └── OrderStatusServiceIntegrationTest.java
├── contract/
│   └── OrderStatusApiContractTest.java
└── testdata/
    ├── fixtures/
    │   ├── OrderFixture.java
    │   ├── CarrierFixture.java
    │   └── AlertFixture.java
    ├── builders/
    │   ├── UpdateOrderStatusRequestBuilder.java
    │   └── OrderBuilder.java
    └── containers/
        └── PostgreSQLContainer.java
```

**Structure Decision**: Single project using Hexagonal Architecture with Spring Boot for the independent logistics microservice, separating domain, application, and infrastructure layers.


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

**Purpose**: Project initialization and basic structure for the order status update microservice

- [ ] T001 Create project structure with Hexagonal Architecture per implementation plan
- [ ] T002 Initialize Java 21 project with Spring Boot 3.x, Gradle dependencies
- [ ] T003 Configure linting and formatting tools (Spotless, Checkstyle)
- [ ] T004 Setup PostgreSQL database configuration
- [ ] T005 Configure Flyway for database migrations
- [ ] T006 Setup basic offline-capable infrastructure (as required by SC-004)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish core domain models, persistence, security, and offline capabilities required for order status updates

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T004 Create database schema for Order, Carrier, Alert entities with Flyway migrations
- [ ] T005 Implement authentication framework for carriers (transportistas)
- [ ] T006 Setup REST API routing and middleware with Spring Web
- [ ] T007 Create base domain models: Order, Carrier, Alert, FinalStatus enum, EffectivenessRate value object
- [ ] T008 Configure global exception handling and validation
- [ ] T009 Setup logging infrastructure
- [ ] T010 Implement offline synchronization framework (per SC-004)
- [ ] T011 Setup auditing system for order state changes (per FR-004)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Actualizar Estado de Pedidos (Priority: P1) 

**Goal**: Allow carrier to update order status with final state and effectiveness rate for liquidation.

**Independent Test**: Transportista can update estado_final with tasa_efectividad, confirm data available for liquidation module.

### Tests for User Story 1 

- [ ] T012 [P] [US1] Contract test for PUT /orders/{id}/status in tests/contract/OrderStatusApiContractTest.java
- [ ] T013 [P] [US1] Integration test for update order status user journey in tests/integration/OrderStatusServiceIntegrationTest.java

### Implementation for User Story 1

- [ ] T014 [P] [US1] Create FinalStatus enum in src/main/java/com/logistics/order/status/domain/values/FinalStatus.java
- [ ] T015 [P] [US1] Create EffectivenessRate value object in src/main/java/com/logistics/order/status/domain/values/EffectivenessRate.java
- [ ] T016 [US1] Implement UpdateOrderStatusUseCase in src/main/java/com/logistics/order/status/domain/ports/in/UpdateOrderStatusUseCase.java
- [ ] T017 [US1] Implement OrderRepository port in src/main/java/com/logistics/order/status/domain/ports/out/OrderRepository.java
- [ ] T018 [US1] Implement UpdateOrderStatusService in src/main/java/com/logistics/order/status/application/services/UpdateOrderStatusService.java
- [ ] T019 [US1] Implement OrderStatusController in src/main/java/com/logistics/order/status/infrastructure/web/controller/OrderStatusController.java
- [ ] T020 [US1] Add validation for effectiveness rate range (-100 to 100)
- [ ] T021 [US1] Add auditing for state changes
- [ ] T022 [US1] Generate Alert on state update

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Final validation, documentation, and cleanup for the order status update feature

- [ ] TXXX Finalize documentation in root-level plan and spec files
- [ ] TXXX Clean up and refactor code to match the agreed architecture and naming
- [ ] TXXX Verify offline synchronization and audit logging behavior
- [ ] TXXX Add any missing unit, integration, or contract tests for the completed feature
- [ ] TXXX Ensure clear error messages for invalid effectiveness rate and duplicate state updates

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion; it blocks implementation of the feature
- **Feature Implementation (Phase 3)**: Depends on Foundational completion; implements the order status update use case
- **Polish (Phase N)**: Depends on Feature Implementation completion; final cleanup and verification

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - this is the primary feature in scope

### Within the Feature

- Models before services
- Services before endpoints
- Core implementation before integration
- Complete the story before final polish
- Tests after implementation

## Notes

- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests pass
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
