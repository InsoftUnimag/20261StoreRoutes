# Implementation Plan: Enviar Estado Final de Pedido (Módulo 2: Logística)

**Date**: April 7, 2026
**Spec**: @/docs/specs/estado-final-pedido/spec.md

## Summary

Implementar la actualización del estado final de un pedido con información de efectividad del transportista y la publicación asíncrona del evento "PedidoEntregado" al módulo de Conciliación Financiera vía queue. El sistema debe validar los estados finales permitidos, la tasa de efectividad en rango (-100 a 100), y garantizar que los datos de id_pedido e id_transportista existan antes de procesar. Stack: Java 21 + Spring Boot + Spring Cloud Stream + RabbitMQ + Spring Data JPA + PostgreSQL, como microservicio independiente del módulo de logística.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Cloud Stream (RabbitMQ), Spring Web (MVC), Spring Data JPA, Hibernate, Flyway, MapStruct, Lombok, Gradle
**Storage**: PostgreSQL
**Messaging**: RabbitMQ (para publicación asíncrona del evento "PedidoEntregado")
**Testing**: JUnit 5, Mockito, AssertJ, TestContainers, ArchUnit, Jacoco
**Target Platform**: Backend server (Event-Driven + REST API) — microservicio independiente de logística
**Project Type**: Web Application (Backend) — stack bloqueante (no reactivo)
**Architecture**: Hexagonal (Ports & Adapters), con event adapters
**Performance Goals**: Actualización y publicación de evento en <1s (SC-001, SC-003)
**Constraints**: Validación estricta de `estado_final` y `tasa_efectividad` antes de publicar; no publicar evento si datos inválidos
**Scale/Scope**: Proceso interno; operación por pedido al finalizar entrega; sin requerimiento de escalado horizontal inmediato

---

## Project Structure

### Documentation (this feature)

```text
docs/specs/estado-final-pedido/
├── plan.md      # This file
└── spec.md      # Feature specification
```

### Source Code (repository root)

```text
src/main/java/com/logistica/estadofinal/
├── domain/
│   ├── models/
│   │   ├── Pedido.java                          # Reutilizar de asignar-ruta
│   │   └── Transportista.java                   # Reutilizar de consultar-paradas
│   ├── values/
│   │   ├── EstadoFinalPedido.java               # NEW: ENTREGADO_COMPLETO, RECHAZO_PARCIAL,
│   │   │                                        #      DEVOLUCION_ERROR_EMPRESA,
│   │   │                                        #      FALTANTE_INVENTARIO, NO_ENTREGADO
│   │   └── TasaEfectividad.java                 # NEW: Value object con validación [-100, 100]
│   ├── ports/
│   │   ├── in/
│   │   │   └── ActualizarEstadoFinalUseCase.java # NEW
│   │   └── out/
│   │       ├── PedidoRepository.java            # Reutilizar
│   │       ├── TransportistaRepository.java     # Reutilizar de consultar-paradas
│   │       └── EventPublisher.java              # Reutilizar de solicitar-ruta
│   └── exceptions/
│       ├── EstadoFinalInvalidoException.java    # NEW: Para FR-002
│       ├── TasaEfectividadFueraDeRangoException.java  # NEW: Para FR-003
│       ├── PedidoNoEncontradoException.java     # NEW: Para FR-006
│       ├── TransportistaNoEncontradoException.java    # NEW: Para FR-006
│       └── LogisticaException.java              # Reutilizar
│
├── application/
│   ├── dto/
│   │   ├── ActualizarEstadoFinalRequest.java    # NEW: id_pedido, id_transportista,
│   │   │                                        #      estado_final, tasa_efectividad
│   │   ├── ActualizarEstadoFinalResponse.java   # NEW: confirmación con datos actualizados
│   │   └── PedidoEntregadoEvent.java            # NEW: DTO del evento de salida
│   ├── services/
│   │   └── ActualizarEstadoFinalService.java    # NEW: valida, persiste y publica evento
│   └── mapper/
│       └── EstadoFinalMapper.java               # NEW: MapStruct
│
└── infrastructure/
    ├── config/
    │   ├── JpaConfig.java                       # Reutilizar
    │   └── StreamConfig.java                    # Reutilizar de solicitar-ruta
    ├── persistence/
    │   ├── jpa/
    │   │   ├── PedidoJpaEntity.java             # Reutilizar
    │   │   └── TransportistaJpaEntity.java      # Reutilizar de consultar-paradas
    │   ├── repository/
    │   │   ├── PedidoRepositoryAdapter.java     # Reutilizar
    │   │   └── TransportistaRepositoryAdapter.java  # Reutilizar de consultar-paradas
    │   └── jparepository/
    │       ├── PedidoSpringRepository.java      # Reutilizar
    │       └── TransportistaSpringRepository.java   # Reutilizar
    ├── messaging/
    │   └── event/
    │       └── EventPublisherAdapter.java       # Reutilizar de solicitar-ruta
    ├── web/
    │   └── controller/
    │       └── EstadoFinalController.java       # NEW
    └── exception/
        └── GlobalExceptionHandler.java          # Reutilizar, agregar nuevas excepciones

src/test/java/com/logistica/estadofinal/
├── unit/
│   ├── domain/
│   │   ├── models/
│   │   │   ├── PedidoTest.java                  # Reutilizar
│   │   │   └── TransportistaTest.java           # Reutilizar
│   │   └── values/
│   │       ├── EstadoFinalPedidoTest.java        # NEW
│   │       └── TasaEfectividadTest.java          # NEW: validaciones de rango
│   ├── application/
│   │   ├── ActualizarEstadoFinalServiceTest.java
│   │   └── EstadoFinalMapperTest.java
│   └── infrastructure/
│       ├── PedidoRepositoryAdapterTest.java     # Reutilizar
│       ├── EventPublisherAdapterTest.java       # Reutilizar
│       └── EstadoFinalControllerTest.java
├── integration/
│   ├── PedidoRepositoryIntegrationTest.java     # Reutilizar
│   └── ActualizarEstadoFinalIntegrationTest.java
├── contract/
│   └── EstadoFinalEventContractTest.java        # NEW: verifica contrato del evento publicado
└── testdata/
    ├── fixtures/
    │   ├── PedidoFixture.java                   # Reutilizar
    │   ├── TransportistaFixture.java            # Reutilizar
    │   └── PedidoEntregadoEventFixture.java     # NEW
    ├── builders/
    │   ├── PedidoBuilder.java                   # Reutilizar
    │   └── ActualizarEstadoFinalRequestBuilder.java  # NEW
    └── containers/
        ├── PostgreSQLContainer.java             # Reutilizar
        └── RabbitMQContainer.java               # Reutilizar de solicitar-ruta
```

**Structure Decision**: Arquitectura hexagonal reutilizando entidades y repositorios de features previas. `TasaEfectividad` y `EstadoFinalPedido` son Value Objects en el domain que encapsulan las validaciones de negocio, garantizando que ningún dato inválido llegue a la capa de aplicación. El `EventPublisher` port y su adapter se reutilizan de solicitar-ruta. El controller REST actúa como adaptador de entrada para el reporte del transportista.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Configurar el proyecto Gradle, dependencias incluyendo messaging, y base de datos.

- [ ] T001: Verificar o crear proyecto con `build.gradle.kts` e incluir dependencias: `spring-boot-starter-web`, `spring-cloud-starter-stream-rabbit`, `spring-boot-starter-data-jpa`, `postgresql`, `flyway-core`, `lombok`, `mapstruct`, `springdoc-openapi-starter-webmvc-ui`, `spring-boot-starter-test`, `testcontainers`, `archunit`.
- [ ] T002: Configurar `application.yml` con conexión PostgreSQL, RabbitMQ (exchange/queue para "PedidoEntregado") y profiles `dev`, `test`, `prod`.
- [ ] T003: Verificar scripts de migración Flyway existentes; agregar si es necesario:
  - Verificar columnas `estado_final` y `tasa_efectividad` en tabla `pedido`.
  - Agregar `V7__add_estado_final_pedido.sql` si las columnas no existen.
- [ ] T004: Configurar `ArchUnit` con reglas de capas: domain sin imports de Spring/JPA/messaging, application sin imports de infrastructure.
- [ ] T005: Reutilizar `PostgreSQLContainer.java` y `RabbitMQContainer.java` para TestContainers.

**Checkpoint**: Proyecto compila, migraciones y contenedores levantan correctamente.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Crear componentes base, Value Objects con validaciones y puertos necesarios.

**⚠️ CRÍTICO**: Depende de Phase 1.

- [ ] T006: Reutilizar entidades del domain: `Pedido`, `Transportista`.
- [ ] T007: Crear `EstadoFinalPedido.java` como enum — valores: `ENTREGADO_COMPLETO`, `RECHAZO_PARCIAL`, `DEVOLUCION_ERROR_EMPRESA`, `FALTANTE_INVENTARIO`, `NO_ENTREGADO`. Incluir método `fromString()` que lanza `EstadoFinalInvalidoException` si el valor no existe.
- [ ] T008: Crear `TasaEfectividad.java` como Value Object — encapsula un `int`, valida rango [-100, 100] en constructor, lanza `TasaEfectividadFueraDeRangoException` si fuera de rango.
- [ ] T009: Crear excepciones: `EstadoFinalInvalidoException`, `TasaEfectividadFueraDeRangoException`, `PedidoNoEncontradoException`, `TransportistaNoEncontradoException`.
- [ ] T010: Reutilizar puertos outbound: `PedidoRepository`, `TransportistaRepository`, `EventPublisher`.
- [ ] T011: Crear puerto inbound `ActualizarEstadoFinalUseCase.java`.
- [ ] T012: Crear DTOs: `ActualizarEstadoFinalRequest.java`, `ActualizarEstadoFinalResponse.java`, `PedidoEntregadoEvent.java` (con campos: id_pedido, id_transportista, estado_final, tasa_efectividad).
- [ ] T013: Crear `EstadoFinalMapper.java` con MapStruct.
- [ ] T014: Reutilizar JPA Entities: `PedidoJpaEntity`, `TransportistaJpaEntity`.
- [ ] T015: Reutilizar Spring Data JPA interfaces: `PedidoSpringRepository`, `TransportistaSpringRepository`.
- [ ] T016: Reutilizar `EventPublisherAdapter.java` de solicitar-ruta; configurar binding para exchange "PedidoEntregado".
- [ ] T017: Actualizar `GlobalExceptionHandler.java`:
  - `EstadoFinalInvalidoException` → HTTP 400
  - `TasaEfectividadFueraDeRangoException` → HTTP 400
  - `PedidoNoEncontradoException` → HTTP 404
  - `TransportistaNoEncontradoException` → HTTP 404

**Checkpoint**: Componentes base creados. Proyecto compila sin errores.

---

## Phase 3: Scenario 1 — Entregado Completo (P1)

**Goal**: Actualizar estado a "Entregado Completo" con tasa_efectividad=100 y publicar evento.

**Independent Test**: POST con estado_final="Entregado Completo" y tasa_efectividad=100 → HTTP 200, evento "PedidoEntregado" publicado con datos correctos.

### Tests para Scenario 1

- [ ] T018 [P]: Contract test en `EstadoFinalEventContractTest` — estado "Entregado Completo" → evento publicado con tasa_efectividad=100.
- [ ] T019 [P]: Contract test en `EstadoFinalControllerTest` — request válido → HTTP 200 con datos actualizados.
- [ ] T020: Integration test en `ActualizarEstadoFinalIntegrationTest` — flujo completo con DB y RabbitMQ reales.

### Implementación de Scenario 1

- [ ] T021: Implementar `ActualizarEstadoFinalService.java`:
  1. Validar existencia de `id_pedido` e `id_transportista` (FR-006).
  2. Construir `EstadoFinalPedido` y `TasaEfectividad` (validaciones en Value Objects).
  3. Persistir estado final en `Pedido` vía `PedidoRepository`.
  4. Publicar `PedidoEntregadoEvent` vía `EventPublisher`.
- [ ] T022: Crear `EstadoFinalController.java` con `POST /api/logistica/pedidos/{idPedido}/estado-final`, inyectando `ActualizarEstadoFinalUseCase`.
- [ ] T023: Unit tests para `ActualizarEstadoFinalService` y `EstadoFinalController`.

**Checkpoint**: Scenario 1 funciona end-to-end. Tests pasan.

---

## Phase 4: Scenarios 2, 3, 4 y 5 — Estados Restantes (P1)

**Goal**: Cubrir los escenarios de Rechazo Parcial, No Entregado, Devolución (Error Empresa) y Faltante de Inventario.

**Independent Test**: Cada estado publica evento con su tasa_efectividad correspondiente (80, 0, 0, -100 respectivamente).

### Tests para Scenarios 2-5

- [ ] T024 [P]: Contract test — "Rechazo Parcial" con tasa_efectividad=80 → evento publicado correctamente.
- [ ] T025 [P]: Contract test — "No Entregado" con tasa_efectividad=0 → evento publicado correctamente.
- [ ] T026 [P]: Contract test — "Devolución (Error Empresa)" con tasa_efectividad=0 → evento publicado correctamente.
- [ ] T027 [P]: Contract test — "Faltante de Inventario" con tasa_efectividad=-100 → evento publicado correctamente.

### Implementación de Scenarios 2-5

- [ ] T028: Verificar que `ActualizarEstadoFinalService` maneja todos los valores del enum sin lógica condicional adicional — el polimorfismo del Value Object debe cubrir todos los casos.
- [ ] T029: Parametrizar unit tests del service con todos los estados válidos y sus tasas de efectividad.

**Checkpoint**: Los 5 scenarios de aceptación pasan.

---

## Phase 5: Edge Cases y Error Handling

**Purpose**: Cubrir los casos edge definidos en el spec.

- [ ] T030: Edge case — `estado_final` inválido (valor no reconocido): `EstadoFinalPedido.fromString()` lanza excepción → HTTP 400, no se publica evento.
- [ ] T031: Edge case — `tasa_efectividad` fuera de rango (ej. 150 o -200): `TasaEfectividad` lanza excepción → HTTP 400, no se publica evento.
- [ ] T032: Edge case — `id_pedido` inexistente: `PedidoNoEncontradoException` → HTTP 404, no se publica evento.
- [ ] T033: Edge case — `id_transportista` inexistente: `TransportistaNoEncontradoException` → HTTP 404, no se publica evento.
- [ ] T034: Tests unitarios y de integración para cada edge case, verificando que el evento NO se publica ante datos inválidos.

**Checkpoint**: Todos los edge cases cubiertos. SC-002 verificable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Logging, validaciones de entrada, documentación y performance.

- [ ] T035: Agregar logging en service: INFO para actualizaciones exitosas con id_pedido y estado_final, WARN para rechazos por validación, ERROR para fallos de publicación.
- [ ] T036: Bean Validation en `ActualizarEstadoFinalRequest` — `id_pedido` not null, `id_transportista` not null, `estado_final` not blank, `tasa_efectividad` not null.
- [ ] T037: Documentar API con `springdoc-openapi`; incluir ejemplos para cada estado_final.
- [ ] T038: Documentar contrato del evento "PedidoEntregado" (estructura JSON, exchange, routing key).
- [ ] T039: Test de performance — verificar actualización y publicación en <1s.
- [ ] T040: Code coverage ≥80%.
- [ ] T041: README con ejemplos curl para cada escenario de estado_final.

**Checkpoint**: Production-ready. SC-001, SC-002 y SC-003 verificables.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias.
- **Foundational (Phase 2)**: Depende de Phase 1.
- **Scenario 1 (Phase 3)**: Depende de Phase 2.
- **Scenarios 2-5 (Phase 4)**: Depende de Phase 3.
- **Edge Cases (Phase 5)**: Depende de Phase 4.
- **Polish (Phase 6)**: Depende de Phase 5.

### User Story Dependencies

- **User Story 1 (P1)**: Depende de fases completas.

## Notes

- Las validaciones de negocio viven en los Value Objects `EstadoFinalPedido` y `TasaEfectividad` — no en el service ni en el controller. Esto garantiza que el domain sea el único guardián de las reglas.
- `EventPublisher` se reutiliza de solicitar-ruta; solo se configura un nuevo binding para el exchange "PedidoEntregado".
- En tests, mockear `EventPublisher` para verificar que el evento se publica (o no se publica) según el escenario.
- Etiqueta `[P]` para tests first.
- Commit después de cada tarea con tests verdes.
