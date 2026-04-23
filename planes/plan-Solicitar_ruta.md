# Implementation Plan: Consultar Paradas de Rutas (Módulo 2: Logística)

**Date**: April 23, 2026
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

**NOTA IMPORTANTE - Arquitectura Hexagonal Limpia:**
- **domain/**: Contiene SOLO lógica de negocio pura, SIN dependencias de frameworks. Incluye models, value objects, ports (in/out) y exceptions.
- **application/**: Contiene ÚNICAMENTE servicios que coordinan casos de uso. Sin DTOs, sin mappers, sin referencias a infraestructura. Los servicios reciben y retornan domain objects.
- **infrastructure/**: Contiene TODOS los adaptadores, DTOs, controllers, persistencia y mappers.
  - Los DTOs son conceptos de presentación/API, pertenecen exclusivamente a infraestructura.
  - El mapper (MapStruct) vive en infraestructura: es un detalle de implementación del adaptador.
  - El controller es responsable de traducir DTOs → domain objects antes de llamar al use case, y domain objects → DTOs al responder.

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
│   └── services/
│       ├── ConsultarParadasService.java
│       └── AutorizacionService.java         # NEW: Para verificar acceso
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
    │   ├── controller/
    │   │   └── ConsultarController.java
    │   └── dto/
    │       ├── ConsultarParadasRequest.java     # Podría incluir idTransportista si no en auth
    │       ├── ConsultarParadasResponse.java
    │       ├── ParadaDTO.java
    │       └── RutaDTO.java                     # Reutilizar si aplica
    ├── mapper/
    │   └── ConsultarMapper.java
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
│   │   └── AutorizacionServiceTest.java     # NEW
│   └── infrastructure/
│       ├── ConsultarMapperTest.java
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

**Structure Decision**: Arquitectura hexagonal con tres capas (domain, application, infrastructure). El domain no tiene dependencias de frameworks. Los ports se ubican en `domain/ports/in` y `domain/ports/out` para reflejar su naturaleza semántica. El único adaptador de entrada HTTP es `ConsultarController`, que agrupa el caso de uso de la user story.

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
- [ ] T012: Crear DTOs en `infrastructure/web/dto/`: `ConsultarParadasRequest.java`, `ConsultarParadasResponse.java`, `ParadaDTO.java`.
- [ ] T013: Crear `ConsultarMapper.java` en `infrastructure/mapper/` con MapStruct.
- [ ] T014: Reutilizar JPA Entities: `RutaJpaEntity`, `ParadaJpaEntity`.
- [ ] T015: Crear `TransportistaJpaEntity.java`.
- [ ] T016: Reutilizar Spring Data JPA interfaces: `RutaSpringRepository`, `ParadaSpringRepository`.
- [ ] T017: Crear `TransportistaSpringRepository.java`.
- [ ] T018: Actualizar `GlobalExceptionHandler.java` para `AccesoDenegadoException` → HTTP 403.
- [ ] T019: Crear `AutorizacionService.java` — verifica si ruta está asignada al transportista.
- [ ] T020: Crear fixtures base: `TransportistaFixture.java` con datos de prueba estándar.
- [ ] T021: Unit tests para `Transportista.java` — validar creación y estado.

**Checkpoint**: Componentes base creados. Proyecto compila sin errores.

---

## Phase 3: Implementación de Consulta (P1)

**Goal**: Implementar el endpoint para consultar paradas, con autorización y logging.

**Independent Test**: Dado un transportista con ruta asignada, GET /api/logistica/rutas/{idRuta}/paradas retorna lista de paradas ordenadas.

### Tests para Implementación

- [ ] T022 [P]: Contract test en `ConsultarApiContractTest` — GET con ruta asignada → HTTP 200, lista de paradas.
- [ ] T023 [P]: Contract test — GET con ruta no asignada → HTTP 403.
- [ ] T024: Integration test en `ConsultarServiceIntegrationTest` — flujo completo con DB real.
- [ ] T025: Unit test en `AutorizacionServiceTest` — verificar acceso permitido/denegado.

### Implementación

- [ ] T026: Implementar `ConsultarParadasService.java` — verifica autorización, obtiene paradas ordenadas.
- [ ] T027: Crear `ConsultarController.java` con `GET /api/logistica/rutas/{idRuta}/paradas`, inyectando `ConsultarParadasUseCase`.
- [ ] T028: Agregar logging en service: INFO para consultas exitosas, WARN para denegadas.
- [ ] T029: Unit tests para `ConsultarParadasService` y `ConsultarController`.

**Checkpoint**: Endpoint funciona end-to-end. Tests pasan.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Validaciones, documentación, performance, observabilidad y hardening que afectan el escenario.

- [ ] T030: Logging estratégico con `@Slf4j`:
  - INFO al inicio/fin de cada consulta (idRuta consultada, cantidad de paradas retornadas)
  - WARN en intento de acceso denegado (idTransportista, idRuta rechazada)
  - ERROR en excepciones inesperadas con contexto completo
- [ ] T031: Bean Validation en requests si aplica (aunque GET, validar path params).
- [ ] T032: Documentar API con `springdoc-openapi`: el endpoint, request/response schemas, códigos HTTP posibles (200, 403, 404)
- [ ] T033: `@ArchTest` con ArchUnit:
  - domain: sin imports de Spring, JPA, web
  - application: puede importar domain, no infrastructure
  - infrastructure: puede importar todo
  - Verificar que ninguna clase en `domain/` o `application/` importa clases de `infrastructure/`
- [ ] T034: Optimización de queries — revisar N+1 en consulta de paradas, validar índices en `paradas.id_ruta` y `paradas.secuencia` con EXPLAIN ANALYZE
- [ ] T035: Configurar `/actuator/health` con BD connectivity check
- [ ] T036: Code coverage con Jacoco — verificar target ≥80% global, 100% domain layer
- [ ] T037: README con build/run commands, test execution (unit / integration / all), ejemplos curl del endpoint
- [ ] T038: Pre-deployment checklist: tests 100% passing, ArchUnit passing, coverage ≥80%, sin vulnerabilidades en dependencias

**Checkpoint**: Código production-ready. El success criterion del spec (SC-001) verificable con tests automáticos.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias — puede iniciar de inmediato.
- **Foundational (Phase 2)**: Depende de Phase 1 — **bloquea la implementación**.
- **Implementación (Phase 3)**: Depende de Phase 2. Sin dependencias de otros escenarios.
- **Polish (Phase 4)**: Depende de que Phase 3 esté completa.

### User Story Dependencies

- **User Story 1 (P1)**: Puede iniciar en cuanto Phase 2 esté completa. Sin dependencias de otros escenarios.

### Within Each User Story

- Domain models y métodos → Ports (interfaces) → Repository Adapters → Service → Controller
- Tests de contrato e integración (`[P]`) antes de la implementación
- Unit tests inline con cada componente
- Checkpoint al final antes de pasar a la siguiente fase

## Notes

- La etiqueta `[P]` indica test que debe escribirse antes de la implementación (test-first).
- Los services de `application/` son los únicos que implementan los use cases; reciben y retornan domain objects. Nunca reciben ni producen DTOs directamente.
- El controller (`ConsultarController`) es el único punto donde se realizan conversiones DTO ↔ domain, delegando siempre en `ConsultarMapper`.
- Reutilizar componentes de asignar-ruta donde posible.
- Commit después de cada tarea completada con tests verdes.
