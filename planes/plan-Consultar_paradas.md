# Implementation Plan: Consultar Paradas de Rutas (Módulo 2: Logística)

**Date**: April 6, 2026
**Spec**: @/docs/specs/consultar-paradas/spec.md

## Summary

Implementar la funcionalidad para que transportistas consulten la lista de paradas asignadas a sus rutas específicas, asegurando acceso restringido solo a rutas asignadas, con logging para auditoría y respuesta rápida. El sistema debe mostrar detalles como ID de parada, dirección y contacto del cliente, ordenados por secuencia. Stack: Java 21 + Spring Boot MVC + Spring Data JPA + PostgreSQL, como microservicio independiente del módulo de logística.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Web (MVC), Spring Data JPA, Hibernate, Flyway, MapStruct, Lombok, Gradle
**Storage**: PostgreSQL
**Testing**: JUnit 5, Mockito, AssertJ, TestContainers, ArchUnit, Jacoco
**Target Platform**: Backend server (REST API) — microservicio independiente de logística
**Project Type**: Web Application (Backend) — stack bloqueante (no reactivo)
**Architecture**: Hexagonal (Ports & Adapters)
**Performance Goals**: Consultar paradas en <3s para rutas con hasta 20 paradas (SC-001)
**Constraints**: Acceso restringido por autenticación de transportista; logging no debe afectar rendimiento
**Scale/Scope**: Proceso interno; consultas frecuentes por transportista; sin escalado horizontal inmediato

---

## Project Structure

### Documentation (this feature)

```text
docs/specs/consultar-paradas/
├── plan.md      # This file
└── spec.md      # Feature specification
```

### Source Code (repository root)

```text
src/main/java/com/logistica/consultar/
├── domain/
│   ├── models/
│   │   ├── Ruta.java
│   │   ├── Parada.java
│   │   ├── Vehiculo.java
│   │   └── Transportista.java          # NEW: Para autenticación y autorización
│   ├── values/
│   │   ├── EstadoRuta.java            # Reutilizar de asignar-ruta
│   │   └── EstadoParada.java          # Reutilizar de asignar-ruta
│   ├── ports/
│   │   ├── in/
│   │   │   └── ConsultarParadasUseCase.java
│   │   └── out/
│   │       ├── RutaRepository.java
│   │       ├── ParadaRepository.java
│   │       └── TransportistaRepository.java  # NEW: Para verificar asignación
│   └── exceptions/
│       ├── AccesoDenegadoException.java      # NEW: Para FR-003
│       └── LogisticaException.java           # Reutilizar
│
├── application/
│   ├── dto/
│   │   ├── ConsultarParadasRequest.java     # Podría incluir idTransportista si no en auth
│   │   ├── ConsultarParadasResponse.java
│   │   ├── ParadaDTO.java
│   │   └── RutaDTO.java                     # Reutilizar si aplica
│   ├── services/
│   │   ├── ConsultarParadasService.java
│   │   └── AutorizacionService.java         # NEW: Para verificar acceso
│   └── mapper/
│       └── ConsultarMapper.java
│
└── infrastructure/
    ├── config/
    │   └── JpaConfig.java                   # Reutilizar
    ├── persistence/
    │   ├── jpa/
    │   │   ├── RutaJpaEntity.java           # Reutilizar
    │   │   ├── ParadaJpaEntity.java         # Reutilizar
    │   │   └── TransportistaJpaEntity.java  # NEW
    │   ├── repository/
    │   │   ├── RutaRepositoryAdapter.java   # Reutilizar
    │   │   ├── ParadaRepositoryAdapter.java # Reutilizar
    │   │   └── TransportistaRepositoryAdapter.java  # NEW
    │   └── jparepository/
    │       ├── RutaSpringRepository.java    # Reutilizar
    │       ├── ParadaSpringRepository.java  # Reutilizar
    │       └── TransportistaSpringRepository.java  # NEW
    ├── web/
    │   └── controller/
    │       └── ConsultarController.java
    └── exception/
        └── GlobalExceptionHandler.java       # Reutilizar, agregar AccesoDenegadoException

src/test/java/com/logistica/consultar/
├── unit/
│   ├── domain/
│   │   ├── models/
│   │   │   ├── RutaTest.java                # Reutilizar
│   │   │   ├── ParadaTest.java              # Reutilizar
│   │   │   └── TransportistaTest.java       # NEW
│   │   └── values/
│   │       ├── EstadoRutaTest.java          # Reutilizar
│   │       └── EstadoParadaTest.java        # Reutilizar
│   ├── application/
│   │   ├── ConsultarParadasServiceTest.java
│   │   ├── AutorizacionServiceTest.java     # NEW
│   │   └── ConsultarMapperTest.java
│   └── infrastructure/
│       ├── RutaRepositoryAdapterTest.java   # Reutilizar
│       ├── ParadaRepositoryAdapterTest.java # Reutilizar
│       └── ConsultarControllerTest.java
├── integration/
│   ├── RutaRepositoryIntegrationTest.java   # Reutilizar
│   ├── ParadaRepositoryIntegrationTest.java # Reutilizar
│   └── ConsultarServiceIntegrationTest.java
├── contract/
│   └── ConsultarApiContractTest.java
└── testdata/
    ├── fixtures/
    │   ├── RutaFixture.java                 # Reutilizar
    │   ├── ParadaFixture.java               # Reutilizar
    │   └── TransportistaFixture.java        # NEW
    ├── builders/
    │   ├── RutaBuilder.java                 # Reutilizar
    │   ├── ParadaBuilder.java               # Reutilizar
    │   └── ConsultarParadasRequestBuilder.java
    └── containers/
        └── PostgreSQLContainer.java          # Reutilizar
```

**Structure Decision**: Arquitectura hexagonal compartiendo entidades y repositorios con el módulo de asignar-ruta donde sea posible. Nuevos componentes marcados como NEW para esta funcionalidad. El controller REST vive en `infrastructure/web/controller/` y es el único adaptador de entrada HTTP.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Configurar el proyecto Gradle, dependencias y base de datos, reutilizando de asignar-ruta si ya existe.

- [ ] T001: Verificar o crear proyecto con `build.gradle.kts` e incluir dependencias: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `postgresql`, `flyway-core`, `lombok`, `mapstruct`, `springdoc-openapi-starter-webmvc-ui`, `spring-boot-starter-test`, `testcontainers`, `archunit`.
- [ ] T002: Configurar `application.yml` con conexión PostgreSQL y profiles `dev`, `test`, `prod`.
- [ ] T003: Actualizar scripts de migración Flyway si es necesario:
  - Agregar `V6__create_transportista_table.sql` — (idTransportista, nombre, estado)
  - Actualizar índices si aplica.
- [ ] T004: Configurar `ArchUnit` con reglas de capas: domain sin imports de Spring/JPA, application sin imports de infrastructure.
- [ ] T005: Configurar `PostgreSQLContainer.java` base para TestContainers reutilizable.

**Checkpoint**: Proyecto compila, migraciones corren sin error, contenedor de test levanta correctamente.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Crear componentes base compartidos y nuevos necesarios para la consulta.

**⚠️ CRÍTICO**: Depende de Phase 1.

- [ ] T006: Reutilizar Value Objects del domain: `EstadoRuta`, `EstadoParada`.
- [ ] T007: Reutilizar entidades del domain: `Ruta`, `Parada`, `Vehiculo`.
- [ ] T008: Crear nueva entidad `Transportista.java` — idTransportista, nombre, estado.
- [ ] T009: Crear nueva excepción `AccesoDenegadoException.java`.
- [ ] T010: Actualizar puertos outbound:
  - Reutilizar `RutaRepository`, `ParadaRepository`.
  - Crear `TransportistaRepository` — `findById`, `findRutasAsignadas`.
- [ ] T011: Crear puerto inbound `ConsultarParadasUseCase.java`.
- [ ] T012: Crear DTOs: `ConsultarParadasRequest.java`, `ConsultarParadasResponse.java`, `ParadaDTO.java`.
- [ ] T013: Crear `ConsultarMapper.java` con MapStruct.
- [ ] T014: Reutilizar JPA Entities: `RutaJpaEntity`, `ParadaJpaEntity`.
- [ ] T015: Crear `TransportistaJpaEntity.java`.
- [ ] T016: Reutilizar Spring Data JPA interfaces: `RutaSpringRepository`, `ParadaSpringRepository`.
- [ ] T017: Crear `TransportistaSpringRepository.java`.
- [ ] T018: Actualizar `GlobalExceptionHandler.java` para `AccesoDenegadoException` → HTTP 403.
- [ ] T019: Crear `AutorizacionService.java` — verifica si ruta está asignada al transportista.

**Checkpoint**: Componentes base creados. Proyecto compila sin errores.

---

## Phase 3: Implementación de Consulta (P1)

**Goal**: Implementar el endpoint para consultar paradas, con autorización y logging.

**Independent Test**: Dado un transportista con ruta asignada, GET /api/logistica/rutas/{idRuta}/paradas retorna lista de paradas ordenadas.

### Tests para Implementación

- [ ] T020 [P]: Contract test en `ConsultarApiContractTest` — GET con ruta asignada → HTTP 200, lista de paradas.
- [ ] T021 [P]: Contract test — GET con ruta no asignada → HTTP 403.
- [ ] T022: Integration test en `ConsultarServiceIntegrationTest` — flujo completo con DB real.
- [ ] T023: Unit test en `AutorizacionServiceTest` — verificar acceso permitido/denegado.

### Implementación

- [ ] T024: Implementar `ConsultarParadasService.java` — verifica autorización, obtiene paradas ordenadas.
- [ ] T025: Crear `ConsultarController.java` con `GET /api/logistica/rutas/{idRuta}/paradas`, inyectando `ConsultarParadasUseCase`.
- [ ] T026: Agregar logging en service: INFO para consultas exitosas, WARN para denegadas.
- [ ] T027: Unit tests para `ConsultarParadasService` y `ConsultarController`.

**Checkpoint**: Endpoint funciona end-to-end. Tests pasan.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Validaciones, documentación, performance.

- [ ] T028: Bean Validation en request si aplica.
- [ ] T029: Documentar API con `springdoc-openapi`.
- [ ] T030: Test de performance — verificar <3s para 20 paradas.
- [ ] T031: Code coverage ≥80%.
- [ ] T032: README con ejemplos curl.

**Checkpoint**: Production-ready.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias.
- **Foundational (Phase 2)**: Depende de Phase 1.
- **Implementación (Phase 3)**: Depende de Phase 2.
- **Polish (Phase 4)**: Depende de Phase 3.

### User Story Dependencies

- **User Story 1 (P1)**: Depende de fases completas.

## Notes

- Reutilizar componentes de asignar-ruta donde posible.
- Etiqueta `[P]` para tests first.
- Commit después de cada tarea con tests verdes.