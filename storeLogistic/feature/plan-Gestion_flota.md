# Implementation Plan: Gestión de Flota (Módulo 2: Logística)

**Date**: March 28, 2026  
**Spec**: `@/docs/specs/gestion-flota/spec.md`

## Summary

Implementar el módulo de administración de flota vehicular, permitiendo a supervisores visualizar el estado de todos los vehículos con sus métricas de ocupación, filtrarlos por categoría/capacidad/estado, registrar nuevos vehículos y gestionar sus transiciones de estado. El sistema debe rechazar transiciones inválidas con HTTP 409 y garantizar integridad bajo accesos concurrentes. Stack: Java 21 + Spring Boot MVC + Spring Data JPA + PostgreSQL, como microservicio independiente del módulo de logística.

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Spring Boot 3.x, Spring Web (MVC), Spring Data JPA, Hibernate, Flyway, MapStruct, Lombok, Gradle  
**Storage**: PostgreSQL  
**Testing**: JUnit 5, Mockito, AssertJ, TestContainers, ArchUnit, Jacoco  
**Target Platform**: Backend server (REST API) — microservicio independiente de logística  
**Project Type**: Web Application (Backend) — stack bloqueante (no reactivo)  
**Architecture**: Hexagonal (Ports & Adapters)  
**Performance Goals**: Listar todos los vehículos en <2s (P95); cambio de estado en <100ms (P95); registro de vehículo en <500ms (P95)  
**Constraints**: `@Transactional(isolation = SERIALIZABLE)` en cambios de estado para garantizar integridad bajo concurrencia de múltiples supervisores  
**Scale/Scope**: 1000+ vehículos registrados; uso interno por supervisores de flota

---

## Project Structure

### Documentation (this feature)

```text
docs/specs/gestion-flota/
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
src/main/java/co/edu/unimagdalena/storelogistic/flota/
├── domain/
│   ├── models/
│   │   ├── Vehiculo.java
│   │   └── Categoria.java
│   ├── values/
│   │   ├── EstadoVehiculo.java       # DISPONIBLE | EN_RUTA | EN_MANTENIMIENTO | FUERA_DE_SERVICIO
│   │   ├── TipoCategoria.java        # CAMIONETA_URBANA | CAMION_SENCILLO | TRACTOCAMION_REGIONAL
│   │   ├── CapacidadCarga.java
│   │   └── FiltroVehiculo.java       # Value object de dominio para filtros de búsqueda (sin anotaciones web)
│   ├── ports/
│   │   ├── in/
│   │   │   ├── ListarVehiculosUseCase.java
│   │   │   ├── ObtenerVehiculoUseCase.java
│   │   │   ├── RegistrarVehiculoUseCase.java
│   │   │   └── CambiarEstadoVehiculoUseCase.java
│   │   └── out/
│   │       ├── VehiculoRepository.java
│   │       └── CategoriaRepository.java
│   └── exceptions/
│       ├── TransicionEstadoInvalidaException.java
│       ├── VehiculoNotFoundException.java
│       └── FlotaException.java
│
├── application/
│   └── services/
│       ├── ListarVehiculosService.java
│       ├── ObtenerVehiculoService.java
│       ├── RegistrarVehiculoService.java
│       └── CambiarEstadoVehiculoService.java
│
└── infrastructure/
    ├── config/
    │   └── JpaConfig.java
    ├── persistence/
    │   ├── jpa/
    │   │   ├── VehiculoJpaEntity.java
    │   │   └── CategoriaJpaEntity.java
    │   ├── repository/
    │   │   ├── VehiculoRepositoryAdapter.java
    │   │   └── CategoriaRepositoryAdapter.java
    │   └── jparepository/
    │       ├── VehiculoSpringRepository.java
    │       └── CategoriaSpringRepository.java
    ├── web/
    │   ├── controller/
    │   │   └── FlotaController.java
    │   └── dto/
    │       ├── VehiculoDTO.java
    │       ├── VehiculoDetailResponse.java
    │       ├── RegistrarVehiculoRequest.java
    │       ├── RegistrarVehiculoResponse.java
    │       ├── CambiarEstadoRequest.java
    │       ├── FiltroVehiculoRequest.java
    │       └── ListaVehiculosResponse.java
    ├── mapper/
    │   ├── VehiculoMapper.java        # MapStruct abstract class: Vehiculo ↔ DTOs (usa CategoriaRepository)
    │   └── CategoriaMapper.java       # MapStruct abstract class: CategoriaJpaEntity ↔ Categoria (domain)
    └── exception/
        ├── GlobalExceptionHandler.java
        └── ErrorResponse.java

src/test/java/com/logistica/flota/
├── unit/
│   ├── domain/
│   │   ├── models/
│   │   │   ├── VehiculoTest.java
│   │   │   └── CategoriaTest.java
│   │   └── values/
│   │       ├── EstadoVehiculoTest.java
│   │       └── TipoCategoriaTest.java
│   ├── application/
│   │   ├── ListarVehiculosServiceTest.java
│   │   ├── RegistrarVehiculoServiceTest.java
│   │   └── CambiarEstadoVehiculoServiceTest.java
│   └── infrastructure/
│       ├── VehiculoMapperTest.java        # ← movido a infrastructure (el mapper es infra)
│       ├── VehiculoRepositoryAdapterTest.java
│       └── FlotaControllerTest.java
├── integration/
│   ├── VehiculoRepositoryIntegrationTest.java
│   └── FlotaServiceIntegrationTest.java
├── contract/
│   └── FlotaApiContractTest.java
└── testdata/
    ├── fixtures/
    │   ├── VehiculoFixture.java
    │   └── CategoriaFixture.java
    ├── builders/
    │   ├── VehiculoBuilder.java
    │   └── RegistrarVehiculoRequestBuilder.java
    └── containers/
        └── PostgreSQLContainer.java
```

**Structure Decision**: Arquitectura hexagonal con tres capas (domain, application, infrastructure). El domain no tiene dependencias de frameworks. Los ports se ubican en `domain/ports/in` y `domain/ports/out` para reflejar su naturaleza semántica. El único adaptador de entrada HTTP es `FlotaController`, que agrupa los tres casos de uso de la user story.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialización del proyecto y estructura base.

- [ ] T001 Crear estructura de directorios según el layout definido en este plan
- [ ] T002 Inicializar proyecto Gradle con dependencias: Spring Boot 3.x, Spring Web, Spring Data JPA, Hibernate, Flyway, MapStruct, Lombok
- [ ] T003 Configurar dependencias de test: JUnit 5, Mockito, AssertJ, TestContainers, ArchUnit, Jacoco
- [ ] T004 Configurar `application.yml` para perfil local (PostgreSQL) y perfil test (TestContainers)
- [ ] T005 Configurar Flyway con migración base `V1__create_schema.sql` (tablas `categorias` y `vehiculos`)
- [ ] T006 Configurar Jacoco con target ≥80% global, 100% domain layer
- [ ] T007 Configurar Checkstyle / formateo de código

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Infraestructura core que debe estar completa antes de implementar cualquier escenario de la user story.

**⚠️ CRITICAL**: Ninguna implementación de escenario puede comenzar hasta completar esta fase.

- [ ] T008 Crear `EstadoVehiculo.java` — enum con valores: `DISPONIBLE`, `EN_RUTA`, `EN_MANTENIMIENTO`, `FUERA_DE_SERVICIO` y método `esTransicionValida(EstadoVehiculo destino)` con todas las reglas del spec (FR-005)
- [ ] T009 Crear `TipoCategoria.java` — enum: `CAMIONETA_URBANA`, `CAMION_SENCILLO`, `TRACTOCAMION_REGIONAL` con `capacidadMaximaKg` asociada
- [ ] T010 Crear `CapacidadCarga.java` — value object que encapsula el peso en kg con validación `> 0`
- [ ] T011 Crear `FiltroVehiculo.java` — value object de dominio con campos opcionales: `TipoCategoria categoria`, `CapacidadCarga capacidadMin`, `CapacidadCarga capacidadMax`, `EstadoVehiculo estado`. Sin anotaciones de Spring ni Bean Validation.
- [ ] T012 Crear `Categoria.java` — domain model con: `idCategoria`, `tipo (TipoCategoria)`, `capacidadMaxima (CapacidadCarga)`
- [ ] T013 Crear `Vehiculo.java` — domain model con: `idVehiculo`, `idCategoria`, `capacidadCarga`, `estado`, `idTransportista`, `pesoActual`, `createdAt`; método `porcentajeOcupacion()` y `cambiarEstado(EstadoVehiculo nuevo)`
- [ ] T014 Crear `TransicionEstadoInvalidaException.java`, `VehiculoNotFoundException.java`, `FlotaException.java`
- [ ] T015 Crear ports de entrada en `domain/ports/in/`: `ListarVehiculosUseCase`, `ObtenerVehiculoUseCase`, `RegistrarVehiculoUseCase`, `CambiarEstadoVehiculoUseCase`. Las firmas de los use cases reciben y retornan domain objects exclusivamente (sin DTOs).
- [ ] T016 Crear ports de salida en `domain/ports/out/`: `VehiculoRepository` con `findWithFilters(FiltroVehiculo filtro)`, `findById`, `save`; y `CategoriaRepository` con `findByTipo`, `findById`.
- [ ] T017 Crear entidades JPA: `VehiculoJpaEntity.java`, `CategoriaJpaEntity.java` con mappings correctos a PostgreSQL
- [ ] T018 Crear `VehiculoSpringRepository.java` y `CategoriaSpringRepository.java` (interfaces JpaRepository)
- [ ] T019 Crear `VehiculoMapper.java` y `CategoriaMapper.java` en `infrastructure/mapper/` como Spring `@Component` — conversión entre domain models, DTOs y entidades JPA. `VehiculoMapper` delega en `CategoriaRepository` para resolver el tipo de categoría por id.
- [ ] T020 Crear `GlobalExceptionHandler.java` con manejo base: `TransicionEstadoInvalidaException` → HTTP 409, `VehiculoNotFoundException` → HTTP 404, `@Valid` violations → HTTP 400
- [ ] T021 Crear `PostgreSQLContainer.java` — configuración TestContainers reutilizable para tests de integración
- [ ] T022 Crear fixtures base: `VehiculoFixture.java`, `CategoriaFixture.java` con datos de prueba estándar
- [ ] T023 Unit tests para `EstadoVehiculo.java` — validar todas las transiciones válidas e inválidas definidas en FR-005

**Checkpoint**: Domain models, value objects, ports, entidades JPA y manejo de excepciones listos. Los tests de `EstadoVehiculo` pasan. Se puede iniciar la implementación de escenarios.

---

## Phase 3: Scenario 1 — Visualizar y filtrar vehículos (P1)

**Goal**: El supervisor puede listar todos los vehículos con sus métricas de ocupación y filtrarlos por categoría, rango de capacidad y estado, con respuesta en menos de 2 segundos incluso con 1000+ registros.

**Independent Test**: Dado que existen vehículos registrados, `GET /api/flota/vehiculos` retorna todos los vehículos con `idVehiculo`, `idTransportista`, `categoria`, `capacidadCarga`, `estado`, `pesoActual`, `porcentajeOcupacion`. Con parámetros de filtro, retorna solo los vehículos que cumplen todos los criterios (AND lógico).

### Tests para Scenario 1

- [ ] T024 [P] [SC1] Contract test en `FlotaApiContractTest` — `GET /api/flota/vehiculos` sin filtros → HTTP 200, array con todos los campos requeridos (FR-001) incluyendo `porcentajeOcupacion`.
- [ ] T025 [P] [SC1] Contract test en `FlotaApiContractTest` — `GET /api/flota/vehiculos?categoria=CAMIONETA_URBANA&estado=DISPONIBLE` → HTTP 200, solo retorna vehículos que cumplen ambos filtros (AND lógico, FR-003).
- [ ] T026 [P] [SC1] Integration test en `VehiculoRepositoryIntegrationTest` — `findWithFilters` con combinación de `categoria + estado + rangoCapacidad` retorna solo los registros correctos con PostgreSQL real.
- [ ] T027 [P] [SC1] Integration test en `VehiculoRepositoryIntegrationTest` — `findWithFilters` con 1000 registros retorna en <2s (SC-001).

### Implementación de Scenario 1

- [ ] T028 [SC1] Agregar método domain a `Vehiculo.java`: `porcentajeOcupacion()` → `(pesoActual / capacidadCarga) * 100`, retorna `BigDecimal` con 2 decimales.
- [ ] T029 [SC1] Crear `VehiculoRepositoryAdapter.java` implementando `VehiculoRepository`:
  - `findWithFilters(FiltroVehiculo filtro)` — recibe el value object de dominio; construye predicados JPA opcionales combinados en AND; ordenado por `idVehiculo`
  - `findById`, `save`
- [ ] T030 [SC1] Implementar `ListarVehiculosService.java` implementando `ListarVehiculosUseCase`:
  - Recibe `FiltroVehiculo` (domain object), delega a `VehiculoRepository.findWithFilters`
  - Retorna `List<Vehiculo>` (domain objects); el mapeo a DTO es responsabilidad del controller
- [ ] T031 [SC1] Crear `FlotaController.java` con los siguientes endpoints:
  - `GET /vehiculos` — parámetros query opcionales: `categoria`, `capacidadMin`, `capacidadMax`, `estado`. Mapea a `FiltroVehiculo` y retorna `ListaVehiculosResponse` (HTTP 200).
  - `GET /vehiculos/{idVehiculo}` — obtiene el detalle de un vehículo por id. Llama a `ObtenerVehiculoUseCase` y retorna `VehiculoDetailResponse` (HTTP 200) o HTTP 404 si no existe.
  Implementar `ObtenerVehiculoService.java` implementando `ObtenerVehiculoUseCase`: busca por id usando `VehiculoRepository.findById`, lanza `VehiculoNotFoundException` si no existe.
- [ ] T032 [SC1] Crear `FiltroVehiculoRequest.java` con validaciones básicas (`capacidadMin` y `capacidadMax` deben ser positivos si presentes). Agregar método de conversión en `VehiculoMapper`: `FiltroVehiculoRequest → FiltroVehiculo`.
- [ ] T033 [SC1] Crear `ListaVehiculosResponse.java` y `VehiculoDTO.java`. Agregar método de conversión en `VehiculoMapper`: `Vehiculo → VehiculoDTO` incluyendo `porcentajeOcupacion`.
- [ ] T034 [SC1] Unit tests para `Vehiculo.porcentajeOcupacion()` — casos: 0%, 50%, 100%, pesoActual mayor que capacidad.
- [ ] T035 [SC1] Unit tests para `ListarVehiculosService` — mock de `VehiculoRepository`; verificar que el `FiltroVehiculo` llega correctamente al repositorio; el servicio retorna domain objects sin conocer DTOs.
- [ ] T036 [SC1] Unit tests para `FlotaController` con MockMvc — GET sin filtros → 200; GET con filtros válidos → 200; GET con `capacidadMin` negativo → 400.

**Checkpoint**: `GET /api/flota/vehiculos` funciona end-to-end con y sin filtros. `porcentajeOcupacion` calculado correctamente. Tests T024–T027 pasan con TestContainers.

---

## Phase 4: Scenario 2 — Registrar nuevo vehículo (P1)

**Goal**: El supervisor registra un nuevo vehículo proporcionando categoría, capacidad de carga e idTransportista. El sistema genera automáticamente el `idVehiculo`, asigna estado inicial `EN_MANTENIMIENTO` y persiste en menos de 500ms.

**Independent Test**: Dado un supervisor con permisos, `POST /api/flota/vehiculos` con body `{categoria, capacidadCarga, idTransportista}` → HTTP 201, body contiene `idVehiculo` auto-generado, `estado: EN_MANTENIMIENTO`, `createdAt` no nulo. El vehículo aparece en el listado posterior.

### Tests para Scenario 2

- [ ] T037 [P] [SC2] Contract test en `FlotaApiContractTest` — `POST /api/flota/vehiculos` con body válido → HTTP 201, body contiene `idVehiculo` único, `estado: EN_MANTENIMIENTO`, `createdAt` no nulo (FR-004, SC-003).
- [ ] T038 [P] [SC2] Contract test en `FlotaApiContractTest` — `POST /api/flota/vehiculos` con body inválido (falta `idTransportista`) → HTTP 400.
- [ ] T039 [P] [SC2] Integration test en `FlotaServiceIntegrationTest` — registro de vehículo persiste en PostgreSQL con `idVehiculo` único auto-generado y estado inicial `EN_MANTENIMIENTO` en <500ms (SC-003).
- [ ] T040 [P] [SC2] Integration test en `VehiculoRepositoryIntegrationTest` — dos `save` consecutivos generan `idVehiculo` distintos (unicidad garantizada).

### Implementación de Scenario 2

- [ ] T041 [SC2] Crear `RegistrarVehiculoRequest.java` con Bean Validation: `@NotNull categoria`, `@NotNull @Positive capacidadCarga`, `@NotBlank idTransportista`.
- [ ] T042 [SC2] Crear `RegistrarVehiculoResponse.java` con: `idVehiculo`, `estado`, `createdAt`.
- [ ] T043 [SC2] Agregar factory method a `Vehiculo.java`: `static registrarNuevo(Categoria categoria, CapacidadCarga capacidadCarga, String idTransportista)` — estado inicial `EN_MANTENIMIENTO`, `pesoActual = 0`, `createdAt = now()`.
- [ ] T044 [SC2] Implementar `RegistrarVehiculoService.java` implementando `RegistrarVehiculoUseCase`:
  - Recibe parámetros de dominio: `TipoCategoria`, `CapacidadCarga`, `String idTransportista`
  - Resolver `Categoria` por tipo via `CategoriaRepository`
  - Llamar `Vehiculo.registrarNuevo(...)` → persistir → retornar `Vehiculo` (domain object)
- [ ] T045 [SC2] Agregar `POST /api/flota/vehiculos` a `FlotaController.java`:
  - Mapea `RegistrarVehiculoRequest` → parámetros de dominio usando `VehiculoMapper`
  - Llama a `RegistrarVehiculoUseCase`
  - Mapea `Vehiculo` → `RegistrarVehiculoResponse` usando `VehiculoMapper`
  - Retorna HTTP 201 + `RegistrarVehiculoResponse`
- [ ] T046 [SC2] Crear `CategoriaRepositoryAdapter.java` implementando `CategoriaRepository`: `findByTipo`, `findById`.
- [ ] T047 [SC2] Unit tests para `Vehiculo.registrarNuevo()` — verificar estado inicial, pesoActual = 0, createdAt no nulo.
- [ ] T048 [SC2] Unit tests para `RegistrarVehiculoService` — mock de repositorios; el servicio recibe y retorna domain objects; verificar que `save` es llamado con el vehículo correcto.
- [ ] T049 [SC2] Unit tests para `FlotaController` con MockMvc — POST válido → 201; POST sin `idTransportista` → 400.

**Checkpoint**: `POST /api/flota/vehiculos` registra correctamente y el vehículo aparece en el listado. SC1 y SC2 funcionan end-to-end de forma independiente. Tests T037–T040 pasan.

---

## Phase 5: Scenario 3 — Cambiar estado de vehículo (P1)

**Goal**: El supervisor puede cambiar el estado de un vehículo entre transiciones válidas. Las transiciones inválidas son rechazadas con HTTP 409. Los cambios se persisten en menos de 100ms bajo acceso concurrente de múltiples supervisores.

**Independent Test**: Dado un vehículo en estado `EN_MANTENIMIENTO`, `PATCH /api/flota/vehiculos/{id}/estado` con `{estado: DISPONIBLE}` → HTTP 200, estado actualizado. El mismo request con `{estado: EN_RUTA}` (transición inválida) → HTTP 409 con descripción de la restricción.

### Tests para Scenario 3

- [ ] T050 [P] [SC3] Contract test en `FlotaApiContractTest` — `PATCH /api/flota/vehiculos/{id}/estado` con transición válida (`EN_MANTENIMIENTO → DISPONIBLE`) → HTTP 200, body contiene nuevo estado (FR-005, SC-004).
- [ ] T051 [P] [SC3] Contract test en `FlotaApiContractTest` — transición inválida (`EN_MANTENIMIENTO → EN_RUTA`) → HTTP 409 con body descriptivo indicando la restricción (edge case spec).
- [ ] T052 [P] [SC3] Contract test en `FlotaApiContractTest` — vehículo inexistente → HTTP 404.
- [ ] T053 [P] [SC3] Integration test en `FlotaServiceIntegrationTest` — cambio de estado se persiste en PostgreSQL en <100ms (SC-004, SC-005).
- [ ] T054 [P] [SC3] Integration test de concurrencia en `FlotaServiceIntegrationTest` — 10 hilos simultáneos intentan cambiar estado del mismo vehículo; verificar integridad del estado final gracias a `SERIALIZABLE`.

### Implementación de Scenario 3

- [ ] T055 [SC3] Completar `EstadoVehiculo.esTransicionValida(EstadoVehiculo destino)` con todas las reglas de FR-005:
  - `EN_MANTENIMIENTO ↔ DISPONIBLE`
  - `DISPONIBLE ↔ EN_RUTA`
  - `cualquiera → FUERA_DE_SERVICIO`
  - `FUERA_DE_SERVICIO ↔ EN_MANTENIMIENTO`
- [ ] T056 [SC3] Agregar método domain a `Vehiculo.java`: `cambiarEstado(EstadoVehiculo nuevo)` — valida con `EstadoVehiculo.esTransicionValida`, lanza `TransicionEstadoInvalidaException` si es inválida, actualiza estado si es válida.
- [ ] T057 [SC3] Implementar `CambiarEstadoVehiculoService.java` implementando `CambiarEstadoVehiculoUseCase`:
  - `@Transactional(isolation = Isolation.SERIALIZABLE)`
  - Recibe `String idVehiculo` y `EstadoVehiculo nuevoEstado` (tipos de dominio)
  - Buscar vehículo por id (lanza `VehiculoNotFoundException` si no existe)
  - Llamar `vehiculo.cambiarEstado(nuevoEstado)` → persistir → retornar `Vehiculo` (domain object)
- [ ] T058 [SC3] Crear `CambiarEstadoRequest.java` con `@NotNull EstadoVehiculo estado`.
- [ ] T059 [SC3] Agregar `PATCH /api/flota/vehiculos/{id}/estado` a `FlotaController.java`:
  - Mapea `CambiarEstadoRequest` → `EstadoVehiculo` (ya es un tipo de dominio, extracción directa)
  - Llama a `CambiarEstadoVehiculoUseCase` con `idVehiculo` y `EstadoVehiculo`
  - Mapea `Vehiculo` → response DTO usando `VehiculoMapper`
  - Retorna HTTP 200
- [ ] T060 [SC3] Completar `GlobalExceptionHandler` — `TransicionEstadoInvalidaException` → HTTP 409 con mensaje descriptivo; `VehiculoNotFoundException` → HTTP 404.
- [ ] T061 [SC3] Unit tests para `EstadoVehiculo.esTransicionValida` — todas las combinaciones válidas e inválidas de la matriz de transiciones.
- [ ] T062 [SC3] Unit tests para `Vehiculo.cambiarEstado()` — transición válida actualiza estado; transición inválida lanza excepción con mensaje descriptivo.
- [ ] T063 [SC3] Unit tests para `CambiarEstadoVehiculoService` — mock de `VehiculoRepository`; verificar flujo happy path y excepción en transición inválida; el servicio opera solo con domain objects.
- [ ] T064 [SC3] Unit tests para `FlotaController` con MockMvc — PATCH válido → 200; transición inválida → 409; id inexistente → 404.

**Checkpoint**: Los 3 escenarios del spec funcionan end-to-end. Todas las transiciones de estado válidas e inválidas cubiertas por tests automáticos. SC-001 a SC-005 verificables.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Hardening, observabilidad, validaciones y documentación que afectan los tres escenarios.

- [ ] T065 Logging estratégico con `@Slf4j`:
  - INFO al inicio/fin de cada listado (filtros aplicados, cantidad retornada)
  - INFO al registrar nuevo vehículo (idVehiculo generado, categoria, transportista)
  - INFO en cada cambio de estado (idVehiculo, estadoAnterior → estadoNuevo)
  - WARN en intento de transición inválida (idVehiculo, transición rechazada)
  - ERROR en excepciones inesperadas con contexto completo
- [ ] T066 Completar Bean Validation en todos los requests — revisar casos borde: `capacidadCarga = 0`, `idTransportista` vacío
- [ ] T067 Documentar API con `springdoc-openapi`: los 3 endpoints, request/response schemas, todos los códigos HTTP posibles (200, 201, 400, 404, 409)
- [ ] T068 `@ArchTest` con ArchUnit:
  - domain: sin imports de Spring, JPA, web
  - application: puede importar domain, no infrastructure
  - infrastructure: puede importar todo
  - Verificar que ninguna clase en `domain/` o `application/` importa clases de `infrastructure/`
- [ ] T069 Optimización de queries — revisar N+1 en listado con filtros (`@EntityGraph` si aplica), validar índices en `vehiculos.estado` y `vehiculos.id_categoria` con EXPLAIN ANALYZE
- [ ] T070 Configurar `/actuator/health` con BD connectivity check
- [ ] T071 Code coverage con Jacoco — verificar target ≥80% global, 100% domain layer
- [ ] T072 README con build/run commands, test execution (unit / integration / all), ejemplos curl de los 3 endpoints
- [ ] T073 Pre-deployment checklist: tests 100% passing, ArchUnit passing, coverage ≥80%, sin vulnerabilidades en dependencias

**Checkpoint**: Código production-ready. Todos los success criteria del spec (SC-001 a SC-005) verificables con tests automáticos.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias — puede iniciar de inmediato.
- **Foundational (Phase 2)**: Depende de Phase 1 — **bloquea todos los escenarios**.
- **Scenario 1 (Phase 3)**: Depende de Phase 2. Sin dependencias de otros escenarios.
- **Scenario 2 (Phase 4)**: Depende de Phase 2. Puede correr en paralelo con Phase 3 si hay equipo suficiente; en solitario, hacerla después de Phase 3.
- **Scenario 3 (Phase 5)**: Depende de Phase 2. Puede correr en paralelo con Phase 3 y 4; en solitario, hacerla al final dado que completa la matriz de transiciones de `EstadoVehiculo` iniciada en Phase 2.
- **Polish (Phase 6)**: Depende de que los 3 escenarios estén completos.

### User Story Dependencies

- **Scenario 1 (P1)**: Puede iniciar en cuanto Phase 2 esté completa. Sin dependencias de otros escenarios.
- **Scenario 2 (P1)**: Puede iniciar en cuanto Phase 2 esté completa. Reutiliza `VehiculoRepository` y `FlotaController` de SC1 pero es independientemente testeable.
- **Scenario 3 (P1)**: Puede iniciar en cuanto Phase 2 esté completa. Extiende `Vehiculo.java` y `FlotaController` pero no depende de SC1 ni SC2 para ser testeado de forma aislada.

### Within Each User Story

- Domain models y métodos → Ports (interfaces) → Repository Adapters → Service → Controller
- Tests de contrato e integración (`[P]`) antes de la implementación
- Unit tests inline con cada componente
- Checkpoint al final antes de pasar al siguiente escenario

## Notes

- La etiqueta `[SC1]` / `[SC2]` / `[SC3]` en cada tarea mapea al escenario de aceptación del spec para trazabilidad directa.
- La etiqueta `[P]` indica test que debe escribirse antes de la implementación (test-first).
- `@Transactional(isolation = SERIALIZABLE)` en `CambiarEstadoVehiculoService` es no negociable según el edge case de concurrencia del spec.
- Los tres casos de uso se exponen por un único controller `FlotaController` — no crear controladores duplicados.
- `EstadoVehiculo.esTransicionValida()` es la única fuente de verdad para la matriz de transiciones — no duplicar lógica en el service.
- Los services de `application/` son los únicos que implementan los use cases; reciben y retornan domain objects. Nunca reciben ni producen DTOs directamente.
- El controller (`FlotaController`) es el único punto donde se realizan conversiones DTO ↔ domain, delegando siempre en `VehiculoMapper`.
- `FiltroVehiculo` en `domain/values/` garantiza que el puerto de salida `VehiculoRepository` no tenga dependencias de la capa web.
- Commit después de cada tarea completada con tests verdes.
- Evitar tareas que modifiquen el mismo archivo desde distintos escenarios en paralelo.
