# Implementation Plan: Solicitar Ruta (Módulo 2: Logística)

**Date**: April 6, 2026
**Spec**: @/docs/specs/solicitar-ruta/spec.md

## Summary

Implementar la recepción asíncrona de solicitudes de ruta desde el módulo de Inventario vía eventos, procesar la asignación o creación de rutas (reutilizando lógica de asignar-ruta), y publicar eventos de respuesta ("RutaAsignada" o "ErrorSolicitudRuta"). El sistema debe manejar concurrencia con SERIALIZABLE y asegurar integridad de capacidades. Stack: Java 21 + Spring Boot + Spring Cloud Stream + RabbitMQ + Spring Data JPA + PostgreSQL, como microservicio independiente del módulo de logística.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Cloud Stream (RabbitMQ), Spring Web (para posibles fallbacks), Spring Data JPA, Hibernate, Flyway, MapStruct, Lombok, Gradle
**Storage**: PostgreSQL
**Messaging**: RabbitMQ (para eventos asíncronos)
**Testing**: JUnit 5, Mockito, AssertJ, TestContainers, ArchUnit, Jacoco
**Target Platform**: Backend server (Event-Driven Microservice) — microservicio independiente de logística
**Project Type**: Web Application (Backend) — stack bloqueante (no reactivo)
**Architecture**: Hexagonal (Ports & Adapters), con event adapters
**Performance Goals**: Procesar solicitud en <100ms (P95), publicar evento en <50ms
**Constraints**: Procesamiento secuencial con `@Transactional(isolation = SERIALIZABLE)` para garantizar integridad; comunicación asíncrona vía queue
**Scale/Scope**: Proceso interno; volumen estimado de cientos de solicitudes/día; sin requerimiento de escalado horizontal inmediato

---

## Project Structure

### Documentation (this feature)

```text
docs/specs/solicitar-ruta/
├── plan.md      # This file
└── spec.md      # Feature specification
```

### Source Code (repository root)

**NOTA IMPORTANTE - Arquitectura Hexagonal Limpia:**
- **domain/**: contiene solo lógica de negocio pura, sin dependencias de frameworks. Incluye models, value objects, ports (in/out) y exceptions.
- **application/**: contiene servicios de caso de uso, sin DTOs y sin referencias a infraestructura.
- **infrastructure/**: contiene adaptadores, DTOs, mappers, listeners y persistencia. Los DTOs de eventos viven aquí, y los mappers son detalles de implementación del adaptador.

Los nombres de clases y valores están en **inglés** para consistencia con el código real del proyecto (ej. `RouteStatus`, `StopStatus`, `VehicleType`, `URBAN_VAN`).

```text
src/main/java/co/edu/unimagdalena/storelogistic/route/
├── domain/
│   ├── models/
│   │   ├── Route.java                   # Reutilizar de asignar-ruta
│   │   ├── Stop.java                    # Reutilizar
│   │   ├── Vehicle.java                 # Reutilizar
│   │   └── Order.java                   # Reutilizar
│   ├── values/
│   │   ├── TotalWeight.java             # Reutilizar
│   │   ├── LoadCapacity.java            # Reutilizar
│   │   ├── VehicleType.java             # URBAN_VAN | SINGLE_TRUCK | REGIONAL_SEMI
│   │   ├── RouteStatus.java             # AVAILABLE | CLOSED | PENDING_VEHICLE
│   │   └── StopStatus.java              # PENDING | DELIVERED | REJECTED
│   ├── ports/
│   │   ├── in/
│   │   │   └── ProcessRouteRequestUseCase.java  # NEW: Para procesar evento
│   │   └── out/
│   │       ├── RouteRepository.java      # Reutilizar
│   │       ├── StopRepository.java       # Reutilizar
│   │       ├── VehicleRepository.java    # Reutilizar
│   │       ├── OrderRepository.java      # Reutilizar
│   │       └── EventPublisher.java       # NEW: Para publicar eventos
│   └── exceptions/
│       ├── CapacityExceededException.java    # Reutilizar
│       ├── VehicleNotAvailableException.java # Reutilizar
│       └── LogisticsException.java           # Reutilizar
│
├── application/
│   └── services/
│       ├── ProcessRouteRequestService.java  # NEW: Lógica de procesamiento
│       └── SelectVehicleService.java        # Reutilizar
│
└── infrastructure/
    ├── config/
    │   ├── JpaConfig.java               # Reutilizar
    │   └── MessagingConfig.java         # NEW: Config para eventos asíncronos
    ├── mapper/
    │   └── RouteRequestMapper.java      # NEW: MapStruct para eventos
    ├── persistence/
    │   ├── jpa/
    │   │   ├── RouteJpaEntity.java      # Reutilizar
    │   │   ├── StopJpaEntity.java       # Reutilizar
    │   │   ├── VehicleJpaEntity.java    # Reutilizar
    │   │   └── OrderJpaEntity.java      # Reutilizar
    │   ├── repository/
    │   │   ├── RouteRepositoryAdapter.java     # Reutilizar
    │   │   ├── StopRepositoryAdapter.java      # Reutilizar
    │   │   ├── VehicleRepositoryAdapter.java   # Reutilizar
    │   │   └── OrderRepositoryAdapter.java     # Reutilizar
    │   └── jparepository/
    │       ├── RouteSpringRepository.java      # Reutilizar
    │       ├── StopSpringRepository.java       # Reutilizar
    │       ├── VehicleSpringRepository.java    # Reutilizar
    │       └── OrderSpringRepository.java      # Reutilizar
    ├── messaging/
    │   ├── dto/
    │   │   ├── RouteRequestEvent.java          # NEW: DTO para evento entrada
    │   │   ├── RouteAssignedEvent.java         # NEW: DTO para evento salida
    │   │   ├── RouteRequestErrorEvent.java     # NEW: DTO para error
    │   │   └── RouteDTO.java                   # Reutilizar si aplica
    │   └── event/
    │       ├── RouteRequestEventListener.java  # NEW: Listener para evento entrada
    │       └── EventPublisherAdapter.java      # NEW: Publisher para salida
    └── exception/
        └── GlobalExceptionHandler.java   # Reutilizar, adaptar para eventos

src/test/java/co/edu/unimagdalena/storelogistic/route/request/
├── unit/
│   ├── domain/
│   │   ├── models/
│   │   │   ├── RutaTest.java            # Reutilizar
│   │   │   ├── ParadaTest.java          # Reutilizar
│   │   │   ├── VehiculoTest.java        # Reutilizar
│   │   │   └── PedidoTest.java          # Reutilizar
│   │   └── values/
│   │       ├── TipoVehiculoTest.java    # Reutilizar
│   │       ├── EstadoRutaTest.java      # Reutilizar
│   │       └── EstadoParadaTest.java    # Reutilizar
│   ├── application/
│   │   ├── ProcesarSolicitudRutaServiceTest.java
│   │   └── SeleccionarVehiculoServiceTest.java  # Reutilizar
│   └── infrastructure/
│       ├── RutaRepositoryAdapterTest.java     # Reutilizar
│       ├── ParadaRepositoryAdapterTest.java   # Reutilizar
│       ├── SolicitudRutaEventListenerTest.java
│       └── EventPublisherAdapterTest.java
├── integration/
│   ├── RutaRepositoryIntegrationTest.java     # Reutilizar
│   ├── ParadaRepositoryIntegrationTest.java   # Reutilizar
│   └── ProcesarSolicitudRutaIntegrationTest.java
├── contract/
│   └── SolicitarEventContractTest.java        # NEW: Tests para contratos de eventos
└── testdata/
    ├── fixtures/
    │   ├── RutaFixture.java             # Reutilizar
    │   ├── ParadaFixture.java           # Reutilizar
    │   ├── VehiculoFixture.java         # Reutilizar
    │   ├── PedidoFixture.java           # Reutilizar
    │   └── SolicitudRutaRequeridaFixture.java  # NEW
    ├── builders/
    │   ├── RutaBuilder.java             # Reutilizar
    │   ├── ParadaBuilder.java           # Reutilizar
    │   └── SolicitudRutaRequeridaBuilder.java  # NEW
    └── containers/
        ├── PostgreSQLContainer.java     # Reutilizar
        └── RabbitMQContainer.java       # NEW: Para tests de messaging
```

**Structure Decision**: Arquitectura hexagonal limpia con reutilización masiva de componentes de asignar-ruta. Los DTOs de evento y los mappers viven en `infrastructure/`; la capa de aplicación contiene solo los servicios de caso de uso. Nuevos adaptadores para messaging en `infrastructure/messaging/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Configurar proyecto, dependencias incluyendo messaging, y base de datos.

- [ ] T001: Crear proyecto con `build.gradle.kts` e incluir dependencias: `spring-boot-starter-web`, `spring-cloud-starter-stream-rabbit`, `spring-boot-starter-data-jpa`, `postgresql`, `flyway-core`, `lombok`, `mapstruct`, `springdoc-openapi-starter-webmvc-ui`, `spring-boot-starter-test`, `testcontainers`, `archunit`.
- [ ] T002: Configurar `application.yml` con conexión PostgreSQL, RabbitMQ, y profiles `dev`, `test`, `prod`.
- [ ] T003: Crear scripts de migración Flyway (reutilizar de asignar-ruta).
- [ ] T004: Configurar `ArchUnit` con reglas de capas.
- [ ] T005: Configurar `PostgreSQLContainer.java` y `RabbitMQContainer.java` para TestContainers.

**Checkpoint**: Proyecto compila, migraciones y contenedores levantan.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Reutilizar y crear componentes base.

**⚠️ CRÍTICO**: Depende de Phase 1.

- [ ] T006: Reutilizar Value Objects y entidades del domain.
- [ ] T007: Reutilizar excepciones.
- [ ] T008: Reutilizar puertos outbound, agregar `EventPublisher`.
- [ ] T009: Crear puerto inbound `ProcesarSolicitudRutaUseCase.java`.
- [ ] T010: Crear DTOs de evento en `infrastructure/messaging/dto/`: `SolicitudRutaRequeridaEvent`, `RutaAsignadaEvent`, `ErrorSolicitudRutaEvent`.
- [ ] T011: Crear `SolicitarMapper.java` en `infrastructure/mapper/`.
- [ ] T012: Reutilizar JPA Entities y Spring Repositories.
- [ ] T013: Crear `EventPublisherAdapter.java` implementando `EventPublisher` en `infrastructure/messaging/event/`.
- [ ] T014: Actualizar `GlobalExceptionHandler` para eventos.

**Checkpoint**: Componentes base listos.

---

## Phase 3: Scenario 1 — Procesar solicitud y asignar a ruta existente (P1)

**Goal**: Recibir evento, asignar a ruta existente si capacidad, publicar "RutaAsignada".

**Independent Test**: Enviar evento "SolicitudRutaRequerida", verificar publicación de "RutaAsignada" con ruta existente.

### Tests para Scenario 1

- [ ] T015 [P]: Contract test en `SolicitarEventContractTest` — evento válido con ruta disponible → publica "RutaAsignada".
- [ ] T016: Integration test — flujo completo con DB y RabbitMQ simulados.

### Implementación de Scenario 1

- [ ] T017: Implementar `ProcesarSolicitudRutaService.java` — lógica similar a AsignarPedidoService.
- [ ] T018: Crear `SolicitudRutaEventListener.java` — @StreamListener para "SolicitudRutaRequerida".
- [ ] T019: Unit tests para service y listener.

**Checkpoint**: Scenario 1 funciona.

---

## Phase 4: Scenario 2 — Crear nueva ruta cuando no hay capacidad (P1)

**Goal**: Si no hay ruta disponible, crear nueva y publicar evento.

**Independent Test**: Evento sin rutas disponibles → nueva ruta creada, evento publicado.

### Tests para Scenario 2

- [ ] T020 [P]: Contract test — sin rutas → "RutaAsignada" con nueva idRuta.

### Implementación de Scenario 2

- [ ] T021: Extender service para crear ruta nueva (reutilizar lógica).
- [ ] T022: Unit tests.

**Checkpoint**: Scenario 2 funciona.

---

## Phase 5: Scenario 3 — Cerrar ruta al 95% (P1)

**Goal**: Al asignar, si alcanza 95%, cerrar ruta.

**Independent Test**: Asignación que lleva a 95% → ruta cerrada en evento.

### Tests para Scenario 3

- [ ] T023 [P]: Contract test — asignación a 95% → estado cerrada en evento.

### Implementación de Scenario 3

- [ ] T024: Extender lógica de cierre (reutilizar de asignar-ruta).
- [ ] T025: Unit tests.

**Checkpoint**: Todos scenarios funcionan.

---

## Phase 6: Edge Cases y Error Handling

**Purpose**: Manejar casos edge y errores.

- [ ] T026: Manejar capacidad exacta → asignar y cerrar.
- [ ] T027: Peso > máxima capacidad → nueva ruta.
- [ ] T028: Datos inválidos → publicar "ErrorSolicitudRuta".
- [ ] T029: Tests para edge cases.

**Checkpoint**: Edge cases cubiertos.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Logging, validación, docs, performance.

- [ ] T030: Logging estratégico: INFO para procesamientos, ERROR para fallos.
- [ ] T031: Validación en eventos.
- [ ] T032: Documentar contratos de eventos.
- [ ] T033: Test de concurrencia.
- [ ] T034: Code coverage ≥80%.
- [ ] T035: README con ejemplos de eventos.

**Checkpoint**: Production-ready. SC-001 y SC-002 verificables.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin deps.
- **Foundational (Phase 2)**: Phase 1.
- **Scenarios (Phases 3-5)**: Phase 2.
- **Edge Cases (Phase 6)**: Phases 3-5.
- **Polish (Phase 7)**: Todas previas.

### User Story Dependencies

- **User Story 1 (P1)**: Depende de fases completas.

## Notes

- Reutilización máxima de asignar-ruta.
- Eventos asíncronos, no REST.
- Etiqueta `[P]` para tests first.
- Commit con tests verdes.
