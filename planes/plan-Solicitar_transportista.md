# Implementation Plan: Solicitar Transportista (Módulo 2: Logística)

**Date**: April 7, 2026
**Spec**: @/docs/specs/solicitar-transportista/spec.md

## Summary

Implementar la integración entre el módulo de Logística de Despacho y Distribución y el módulo de Transportista para solicitar un transportista disponible y asignarlo a un vehículo específico. El sistema debe validar disponibilidad del transportista, actualizar el campo `idTransportista` del vehículo y manejar errores claros ante ausencia o invalidez del transportista. Stack: Java 21 + Spring Boot MVC + Spring Data JPA + PostgreSQL, como microservicio independiente del módulo de logística.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Web (MVC), Spring Data JPA, Hibernate, Flyway, MapStruct, Lombok, Gradle
**Storage**: PostgreSQL
**Testing**: JUnit 5, Mockito, AssertJ, TestContainers, ArchUnit, Jacoco
**Target Platform**: Backend server (REST API) — microservicio independiente de logística
**Project Type**: Web Application (Backend) — stack bloqueante (no reactivo)
**Architecture**: Hexagonal (Ports & Adapters)
**Performance Goals**: Solicitar y recibir idTransportista disponible en <1s (SC-001)
**Constraints**: Validación de disponibilidad antes de asignar; idTransportista provisto por módulo externo de Transportista
**Scale/Scope**: Proceso interno; operación puntual por vehículo; sin requerimiento de escalado horizontal inmediato

---

## Project Structure

### Documentation (this feature)

```text
docs/specs/solicitar-transportista/
├── plan.md      # This file
└── spec.md      # Feature specification
```

### Source Code (repository root)

```text
src/main/java/com/logistica/transportista/
├── domain/
│   ├── models/
│   │   ├── Vehiculo.java                        # Reutilizar de asignar-ruta
│   │   └── Transportista.java                   # Reutilizar de consultar-paradas
│   ├── values/
│   │   ├── EstadoVehiculo.java                  # Reutilizar
│   │   └── EstadoTransportista.java             # NEW: DISPONIBLE, NO_DISPONIBLE
│   ├── ports/
│   │   ├── in/
│   │   │   └── SolicitarTransportistaUseCase.java  # NEW
│   │   └── out/
│   │       ├── VehiculoRepository.java           # Reutilizar
│   │       └── TransportistaServicePort.java     # NEW: Puerto hacia módulo externo
│   └── exceptions/
│       ├── TransportistaNoDisponibleException.java  # NEW: Para FR-006
│       ├── TransportistaInvalidoException.java      # NEW: Para edge case idTransportista inválido
│       └── LogisticaException.java                  # Reutilizar
│
├── application/
│   ├── dto/
│   │   ├── SolicitarTransportistaRequest.java    # NEW: idVehiculo
│   │   ├── SolicitarTransportistaResponse.java   # NEW: idVehiculo, idTransportista
│   │   └── TransportistaDisponibleDTO.java       # NEW: idTransportista, nombre, estado
│   ├── services/
│   │   └── SolicitarTransportistaService.java    # NEW: Orquesta solicitud y asignación
│   └── mapper/
│       └── TransportistaMapper.java              # NEW: MapStruct
│
└── infrastructure/
    ├── config/
    │   └── JpaConfig.java                        # Reutilizar
    ├── persistence/
    │   ├── jpa/
    │   │   ├── VehiculoJpaEntity.java            # Reutilizar
    │   │   └── TransportistaJpaEntity.java       # Reutilizar de consultar-paradas
    │   ├── repository/
    │   │   └── VehiculoRepositoryAdapter.java    # Reutilizar
    │   └── jparepository/
    │       └── VehiculoSpringRepository.java     # Reutilizar
    ├── client/
    │   └── TransportistaServiceClient.java       # NEW: HTTP client al módulo Transportista
    ├── web/
    │   └── controller/
    │       └── SolicitarTransportistaController.java  # NEW
    └── exception/
        └── GlobalExceptionHandler.java           # Reutilizar, agregar nuevas excepciones

src/test/java/com/logistica/transportista/
├── unit/
│   ├── domain/
│   │   ├── models/
│   │   │   ├── VehiculoTest.java                 # Reutilizar
│   │   │   └── TransportistaTest.java            # Reutilizar de consultar-paradas
│   │   └── values/
│   │       └── EstadoTransportistaTest.java      # NEW
│   ├── application/
│   │   ├── SolicitarTransportistaServiceTest.java
│   │   └── TransportistaMapperTest.java
│   └── infrastructure/
│       ├── VehiculoRepositoryAdapterTest.java    # Reutilizar
│       ├── TransportistaServiceClientTest.java   # NEW
│       └── SolicitarTransportistaControllerTest.java
├── integration/
│   ├── VehiculoRepositoryIntegrationTest.java    # Reutilizar
│   └── SolicitarTransportistaIntegrationTest.java
├── contract/
│   └── SolicitarTransportistaApiContractTest.java
└── testdata/
    ├── fixtures/
    │   ├── VehiculoFixture.java                  # Reutilizar
    │   └── TransportistaFixture.java             # Reutilizar de consultar-paradas
    ├── builders/
    │   ├── VehiculoBuilder.java                  # Reutilizar
    │   └── SolicitarTransportistaRequestBuilder.java  # NEW
    └── containers/
        └── PostgreSQLContainer.java              # Reutilizar
```

**Structure Decision**: Arquitectura hexagonal reutilizando entidades y repositorios de asignar-ruta y consultar-paradas. El puerto `TransportistaServicePort` abstrae la comunicación con el módulo externo de Transportista, implementado por `TransportistaServiceClient` en infraestructura. El controller REST vive en `infrastructure/web/controller/` como único adaptador de entrada HTTP.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Configurar el proyecto Gradle, dependencias y base de datos, reutilizando de features previas si ya existe.

- [ ] T001: Verificar o crear proyecto con `build.gradle.kts` e incluir dependencias: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `postgresql`, `flyway-core`, `lombok`, `mapstruct`, `springdoc-openapi-starter-webmvc-ui`, `spring-boot-starter-test`, `testcontainers`, `archunit`.
- [ ] T002: Configurar `application.yml` con conexión PostgreSQL, URL del módulo externo de Transportista y profiles `dev`, `test`, `prod`.
- [ ] T003: Verificar scripts de migración Flyway existentes; agregar si es necesario:
  - Reutilizar `V6__create_transportista_table.sql` de consultar-paradas.
  - Verificar columna `idTransportista` en tabla `vehiculo`.
- [ ] T004: Configurar `ArchUnit` con reglas de capas: domain sin imports de Spring/JPA, application sin imports de infrastructure.
- [ ] T005: Configurar `PostgreSQLContainer.java` base para TestContainers reutilizable.

**Checkpoint**: Proyecto compila, migraciones corren sin error, contenedor de test levanta correctamente.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Crear componentes base compartidos y nuevos necesarios para la solicitud y asignación.

**⚠️ CRÍTICO**: Depende de Phase 1.

- [ ] T006: Reutilizar entidades del domain: `Vehiculo`, `Transportista`.
- [ ] T007: Reutilizar `EstadoVehiculo`. Crear nuevo `EstadoTransportista.java` — valores: `DISPONIBLE`, `NO_DISPONIBLE`.
- [ ] T008: Crear excepciones nuevas: `TransportistaNoDisponibleException.java`, `TransportistaInvalidoException.java`.
- [ ] T009: Reutilizar puerto outbound `VehiculoRepository`.
- [ ] T010: Crear puerto outbound `TransportistaServicePort.java` — métodos: `obtenerDisponible(): TransportistaDisponibleDTO`, `validarExistencia(idTransportista)`.
- [ ] T011: Crear puerto inbound `SolicitarTransportistaUseCase.java`.
- [ ] T012: Crear DTOs: `SolicitarTransportistaRequest.java`, `SolicitarTransportistaResponse.java`, `TransportistaDisponibleDTO.java`.
- [ ] T013: Crear `TransportistaMapper.java` con MapStruct.
- [ ] T014: Reutilizar JPA Entities: `VehiculoJpaEntity`, `TransportistaJpaEntity`.
- [ ] T015: Reutilizar Spring Data JPA interfaces: `VehiculoSpringRepository`.
- [ ] T016: Crear `TransportistaServiceClient.java` implementando `TransportistaServicePort` — cliente HTTP con `RestClient` de Spring.
- [ ] T017: Actualizar `GlobalExceptionHandler.java`:
  - `TransportistaNoDisponibleException` → HTTP 409
  - `TransportistaInvalidoException` → HTTP 400

**Checkpoint**: Componentes base creados. Proyecto compila sin errores.

---

## Phase 3: Scenario 1 — Obtener idTransportista disponible (P1)

**Goal**: Solicitar un transportista disponible al módulo externo y retornar su idTransportista.

**Independent Test**: Dado un vehículo sin transportista asignado, POST /api/logistica/vehiculos/{idVehiculo}/transportista retorna idTransportista válido.

### Tests para Scenario 1

- [ ] T018 [P]: Contract test en `SolicitarTransportistaApiContractTest` — solicitud válida con transportista disponible → HTTP 200 con idTransportista.
- [ ] T019 [P]: Contract test — sin transportistas disponibles → HTTP 409 con mensaje de error.
- [ ] T020: Integration test en `SolicitarTransportistaIntegrationTest` — flujo completo con DB real y mock del cliente HTTP.

### Implementación de Scenario 1

- [ ] T021: Implementar `SolicitarTransportistaService.java` — llama a `TransportistaServicePort.obtenerDisponible()`.
- [ ] T022: Crear `SolicitarTransportistaController.java` con `POST /api/logistica/vehiculos/{idVehiculo}/transportista`, inyectando `SolicitarTransportistaUseCase`.
- [ ] T023: Unit tests para `SolicitarTransportistaService` y `SolicitarTransportistaController`.

**Checkpoint**: Scenario 1 funciona end-to-end. Tests pasan.

---

## Phase 4: Scenario 2 — Asignar idTransportista a vehículo (P1)

**Goal**: Asignar el idTransportista obtenido al vehículo, actualizando su campo `idTransportista`.

**Independent Test**: Dado un idTransportista válido, el vehículo actualiza su campo idTransportista correctamente.

### Tests para Scenario 2

- [ ] T024 [P]: Contract test — asignación exitosa → HTTP 200 con vehículo actualizado (idTransportista presente).
- [ ] T025 [P]: Contract test — idTransportista inválido → HTTP 400 con mensaje de error.
- [ ] T026: Integration test — persistencia del idTransportista verificada en DB.

### Implementación de Scenario 2

- [ ] T027: Extender `SolicitarTransportistaService` — tras obtener idTransportista, llama a `VehiculoRepository.save()` con idTransportista actualizado.
- [ ] T028: Agregar validación con `TransportistaServicePort.validarExistencia()` antes de asignar.
- [ ] T029: Unit tests para lógica de asignación y persistencia.

**Checkpoint**: Scenario 2 funciona. idTransportista persiste en el vehículo correctamente.

---

## Phase 5: Edge Cases y Error Handling

**Purpose**: Cubrir los casos edge definidos en el spec.

- [ ] T030: Edge case — idTransportista ya asignado a otro vehículo: validar disponibilidad antes de asignar → retornar error si no disponible.
- [ ] T031: Edge case — reasignación: permitir actualizar idTransportista en vehículo que ya tiene uno asignado.
- [ ] T032: Edge case — idTransportista inválido (no existe en módulo Transportista): retornar `TransportistaInvalidoException` → HTTP 400.
- [ ] T033: Tests unitarios y de integración para cada edge case.

**Checkpoint**: Todos los edge cases cubiertos y verificados con tests.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Logging, validaciones, documentación y performance.

- [ ] T034: Agregar logging en service: INFO para asignaciones exitosas, WARN para transportistas no disponibles, ERROR para fallos inesperados.
- [ ] T035: Bean Validation en `SolicitarTransportistaRequest` — `idVehiculo` not null.
- [ ] T036: Documentar API con `springdoc-openapi`.
- [ ] T037: Test de performance — verificar respuesta <1s (SC-001).
- [ ] T038: Code coverage ≥80%.
- [ ] T039: README con ejemplos curl.

**Checkpoint**: Production-ready. SC-001, SC-002, SC-003 y SC-004 verificables.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias.
- **Foundational (Phase 2)**: Depende de Phase 1.
- **Scenario 1 (Phase 3)**: Depende de Phase 2.
- **Scenario 2 (Phase 4)**: Depende de Phase 3.
- **Edge Cases (Phase 5)**: Depende de Phase 4.
- **Polish (Phase 6)**: Depende de Phase 5.

### User Story Dependencies

- **User Story 1 (P1)**: Depende de fases completas.

## Notes

- Reutilizar componentes de asignar-ruta y consultar-paradas donde sea posible.
- `TransportistaServicePort` desacopla el domain del cliente HTTP externo — en tests se mockea este puerto.
- Etiqueta `[P]` para tests first.
- Commit después de cada tarea con tests verdes.
