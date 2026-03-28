# Implementation Plan: Asignar Ruta a Pedido (Módulo 2: Logística)

**Date**: March 28, 2026
**Spec**: `@/docs/specs/asignar-ruta/spec.md`

## Summary

Implementar el proceso automático de asignación de pedidos a rutas de entrega, validando que la carga no exceda la capacidad del vehículo y consolidando rutas al 95% de capacidad antes del despacho. El sistema debe asignar a ruta existente si hay capacidad, crear una nueva ruta si no la hay, y cerrar la ruta automáticamente al alcanzar el umbral del 95%. Stack: Java 21 + Spring Boot MVC + Spring Data JPA + PostgreSQL, como microservicio independiente del módulo de logística.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Web (MVC), Spring Data JPA, Hibernate, Flyway, MapStruct, Lombok, Gradle
**Storage**: PostgreSQL
**Testing**: JUnit 5, Mockito, AssertJ, TestContainers, ArchUnit, Jacoco
**Target Platform**: Backend server (REST API) — microservicio independiente de logística
**Project Type**: Web Application (Backend) — stack bloqueante (no reactivo)
**Architecture**: Hexagonal (Ports & Adapters)
**Performance Goals**: Asignar pedido a ruta en <100ms (P95)
**Constraints**: Procesamiento secuencial con `@Transactional(isolation = SERIALIZABLE)` para garantizar integridad de capacidades bajo concurrencia
**Scale/Scope**: Proceso interno; volumen estimado de cientos de pedidos/día por ruta; sin requerimiento de escalado horizontal inmediato

---

## Project Structure

### Documentation (this feature)

```text
docs/specs/asignar-ruta/
├── plan.md      # This file
└── spec.md      # Feature specification
```

### Source Code (repository root)

```text
src/main/java/com/logistica/asignacion/
├── domain/
│   ├── models/
│   │   ├── Ruta.java
│   │   ├── Parada.java
│   │   ├── Vehiculo.java
│   │   └── Pedido.java
│   ├── values/
│   │   ├── PesoTotal.java
│   │   ├── CapacidadCarga.java
│   │   ├── TipoVehiculo.java          # CAMIONETA | CAMION_SENCILLO | CAMION_MEDIANO | TRACTOCAMION_REGIONAL
│   │   ├── EstadoRuta.java            # DISPONIBLE | CERRADA | PENDIENTE_VEHICULO
│   │   └── EstadoParada.java          # PENDIENTE | ENTREGADO | RECHAZADO
│   ├── ports/
│   │   ├── in/
│   │   │   └── AsignarPedidoUseCase.java
│   │   └── out/
│   │       ├── RutaRepository.java
│   │       ├── ParadaRepository.java
│   │       ├── VehiculoRepository.java
│   │       └── PedidoRepository.java
│   └── exceptions/
│       ├── CapacidadExcedidaException.java
│       ├── VehiculoNoDisponibleException.java
│       └── LogisticaException.java
│
├── application/
│   ├── dto/
│   │   ├── AsignarPedidoRequest.java
│   │   ├── AsignarPedidoResponse.java
│   │   ├── RutaDTO.java
│   │   └── ParadaDTO.java
│   ├── services/
│   │   ├── AsignarPedidoService.java
│   │   └── SeleccionarVehiculoService.java
│   └── mapper/
│       └── AsignacionMapper.java
│
└── infrastructure/
    ├── config/
    │   └── JpaConfig.java
    ├── persistence/
    │   ├── jpa/
    │   │   ├── RutaJpaEntity.java
    │   │   ├── ParadaJpaEntity.java
    │   │   ├── VehiculoJpaEntity.java
    │   │   └── PedidoJpaEntity.java
    │   ├── repository/
    │   │   ├── RutaRepositoryAdapter.java
    │   │   ├── ParadaRepositoryAdapter.java
    │   │   ├── VehiculoRepositoryAdapter.java
    │   │   └── PedidoRepositoryAdapter.java
    │   └── jparepository/
    │       ├── RutaSpringRepository.java
    │       ├── ParadaSpringRepository.java
    │       ├── VehiculoSpringRepository.java
    │       └── PedidoSpringRepository.java
    ├── web/
    │   └── controller/
    │       └── AsignacionController.java
    └── exception/
        └── GlobalExceptionHandler.java

src/test/java/com/logistica/asignacion/
├── unit/
│   ├── domain/
│   │   ├── models/
│   │   │   ├── RutaTest.java
│   │   │   ├── ParadaTest.java
│   │   │   ├── VehiculoTest.java
│   │   │   └── PedidoTest.java
│   │   └── values/
│   │       ├── TipoVehiculoTest.java
│   │       ├── EstadoRutaTest.java
│   │       └── EstadoParadaTest.java
│   ├── application/
│   │   ├── AsignarPedidoServiceTest.java
│   │   ├── SeleccionarVehiculoServiceTest.java
│   │   └── AsignacionMapperTest.java
│   └── infrastructure/
│       ├── RutaRepositoryAdapterTest.java
│       ├── ParadaRepositoryAdapterTest.java
│       └── AsignacionControllerTest.java
├── integration/
│   ├── RutaRepositoryIntegrationTest.java
│   ├── ParadaRepositoryIntegrationTest.java
│   └── AsignacionServiceIntegrationTest.java
├── contract/
│   └── AsignacionApiContractTest.java
└── testdata/
    ├── fixtures/
    │   ├── RutaFixture.java
    │   ├── ParadaFixture.java
    │   ├── VehiculoFixture.java
    │   └── PedidoFixture.java
    ├── builders/
    │   ├── RutaBuilder.java
    │   ├── ParadaBuilder.java
    │   └── AsignarPedidoRequestBuilder.java
    └── containers/
        └── PostgreSQLContainer.java
```

**Structure Decision**: Arquitectura hexagonal con tres capas (domain, application, infrastructure). El domain no tiene dependencias de frameworks. El controller REST vive en `infrastructure/web/controller/` y es el único adaptador de entrada HTTP — no existe capa "presentation" separada.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Configurar el proyecto Gradle, dependencias y base de datos antes de cualquier implementación de negocio.

- [ ] T001: Crear proyecto con `build.gradle.kts` e incluir dependencias: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `postgresql`, `flyway-core`, `lombok`, `mapstruct`, `springdoc-openapi-starter-webmvc-ui`, `spring-boot-starter-test`, `testcontainers`, `archunit`.
- [ ] T002: Configurar `application.yml` con conexión PostgreSQL y profiles `dev`, `test`, `prod`.
- [ ] T003: Crear scripts de migración Flyway:
  - `V1__create_vehiculo_table.sql` — (idVehiculo, tipo, capacidad_carga, estado, idTransportista)
  - `V2__create_ruta_table.sql` — (idRuta, idVehiculo FK, capacidad_total, peso_acumulado, estado, fecha_despacho)
  - `V3__create_pedido_table.sql` — (idPedido, peso_total, direccion_entrega)
  - `V4__create_parada_table.sql` — (idParada, idRuta FK, idPedido FK, secuencia, direccion_entrega, estado, fecha_entrega)
  - `V5__create_indexes.sql` — índices en `ruta.estado`, `ruta.peso_acumulado`, `parada.idRuta`
- [ ] T004: Configurar `ArchUnit` con reglas de capas: domain sin imports de Spring/JPA, application sin imports de infrastructure.
- [ ] T005: Configurar `PostgreSQLContainer.java` base para TestContainers reutilizable en todos los tests de integración.

**Checkpoint**: Proyecto compila, migraciones corren sin error, contenedor de test levanta correctamente.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Crear todos los componentes base que las User Stories necesitan para funcionar. Nada de lógica de negocio todavía — solo la infraestructura que las historias comparten.

**⚠️ CRÍTICO**: Ninguna User Story puede comenzar hasta que esta fase esté completa.

- [ ] T006: Crear Value Objects del domain:
  - `PesoTotal.java` — valida > 0, inmutable
  - `CapacidadCarga.java` — valida > 0, inmutable
  - `TipoVehiculo.enum` — CAMIONETA (≤1.5t) | CAMION_SENCILLO (≤5t) | CAMION_MEDIANO (≤25t) | TRACTOCAMION_REGIONAL (>25t)
  - `EstadoRuta.enum` — DISPONIBLE | CERRADA | PENDIENTE_VEHICULO
  - `EstadoParada.enum` — PENDIENTE | ENTREGADO | RECHAZADO
- [ ] T007: Crear entidades del domain (sin lógica de negocio todavía, solo estructura):
  - `Vehiculo.java` — idVehiculo, tipo, capacidadCarga, estado, idTransportista
  - `Pedido.java` — idPedido, pesoTotal, direccionEntrega
  - `Parada.java` — idParada, idRuta, idPedido, secuencia, direccionEntrega, estado, fechaEntrega
  - `Ruta.java` — idRuta, idVehiculo, capacidadTotal, pesoAcumulado, estado, fechaDespacho, paradas
- [ ] T008: Crear excepciones de domain: `LogisticaException`, `CapacidadExcedidaException`, `VehiculoNoDisponibleException`.
- [ ] T009: Crear puertos outbound en `domain/ports/out/`:
  - `RutaRepository` — `findAvailableWithCapacity`, `save`, `findById`
  - `ParadaRepository` — `save`, `findByRutaId`
  - `VehiculoRepository` — `findAvailableByCapacidad`, `findMaxCapacidad`
  - `PedidoRepository` — `findById`
- [ ] T010: Crear puerto inbound `AsignarPedidoUseCase.java` en `domain/ports/in/`.
- [ ] T011: Crear DTOs: `AsignarPedidoRequest.java`, `AsignarPedidoResponse.java`, `RutaDTO.java`, `ParadaDTO.java`.
- [ ] T012: Crear `AsignacionMapper.java` con MapStruct (Domain ↔ DTO).
- [ ] T013: Crear JPA Entities:
  - `RutaJpaEntity.java` — `@OneToMany` a `ParadaJpaEntity`
  - `ParadaJpaEntity.java` — `@ManyToOne` a `RutaJpaEntity` y `PedidoJpaEntity`
  - `VehiculoJpaEntity.java`
  - `PedidoJpaEntity.java`
- [ ] T014: Crear Spring Data JPA interfaces: `RutaSpringRepository`, `ParadaSpringRepository`, `VehiculoSpringRepository`, `PedidoSpringRepository`.
- [ ] T015: Crear `GlobalExceptionHandler.java` con `@ControllerAdvice` para `LogisticaException` y subclases.
- [ ] T016: Crear `SeleccionarVehiculoService.java`:
  - Clasifica peso en rango → busca vehículo disponible del tipo correspondiente
  - Si peso > capacidad máxima → lanza `CapacidadExcedidaException`
  - Si no hay vehículo disponible → retorna `Optional.empty()`

**Checkpoint**: Todas las entidades, puertos, DTOs, mappers y JPA entities creados. El proyecto compila sin errores. Ningún endpoint funcional todavía.

---

## Phase 3: Scenario 1 — Asignar pedido a ruta existente (P1)

**Goal**: Cuando llega un pedido y existe una ruta disponible con capacidad suficiente, el sistema lo asigna a esa ruta, crea la parada correspondiente y actualiza el peso acumulado.

**Independent Test**: Dado un vehículo disponible y una ruta con estado DISPONIBLE con capacidad suficiente, se hace `POST /api/logistica/asignacion` con un pesoTotal válido y se verifica que: la respuesta contiene el idRuta existente, el peso_acumulado de la ruta aumentó, y se creó una nueva Parada con FK correctas a ruta y pedido.

### Tests para Scenario 1

- [ ] T017 [P] [SC1]: Contract test en `AsignacionApiContractTest` — `POST /api/logistica/asignacion` con ruta existente con capacidad → HTTP 200, body contiene `idRuta`, `pesoAcumulado` actualizado, `parada` creada.
- [ ] T018 [P] [SC1]: Integration test en `AsignacionServiceIntegrationTest` — flujo completo service + mocks de repositorios: verificar que `RutaRepository.save()` y `ParadaRepository.save()` son llamados exactamente una vez con los valores correctos.
- [ ] T019 [P] [SC1]: Integration test en `RutaRepositoryIntegrationTest` — `findAvailableWithCapacity` retorna la ruta con mayor % de ocupación (FR-002) cuando hay varias disponibles.
- [ ] T020 [P] [SC1]: Integration test en `ParadaRepositoryIntegrationTest` — `save` persiste parada con FK `idRuta` e `idPedido` correctas en PostgreSQL real.

### Implementación de Scenario 1

- [ ] T021 [SC1]: Agregar métodos domain a `Ruta.java`:
  - `puedeAceptarPeso(PesoTotal peso)` → boolean (FR-001)
  - `asignarPedido(Pedido pedido)` → crea y retorna `Parada`, incrementa `pesoAcumulado`
- [ ] T022 [SC1]: Agregar método domain a `Parada.java`: `marcarEntregada()`, `marcarRechazada()`.
- [ ] T023 [SC1]: Crear `RutaRepositoryAdapter.java` implementando `RutaRepository`:
  - `findAvailableWithCapacity` — query que prioriza ruta con mayor % de uso (FR-002)
  - `save`, `findById`
- [ ] T024 [SC1]: Crear `ParadaRepositoryAdapter.java` implementando `ParadaRepository`: `save`, `findByRutaId`.
- [ ] T025 [SC1]: Crear `PedidoRepositoryAdapter.java` implementando `PedidoRepository`.
- [ ] T026 [SC1]: Implementar flujo "ruta existente" en `AsignarPedidoService.java`:
  - `@Transactional(isolation = Isolation.SERIALIZABLE)`
  - Buscar ruta disponible con `RutaRepository.findAvailableWithCapacity`
  - Si existe: llamar `ruta.asignarPedido(pedido)` → guardar ruta → guardar parada → retornar response
- [ ] T027 [SC1]: Crear `AsignacionController.java` con `POST /api/logistica/asignacion`, inyectando `AsignarPedidoUseCase`, retornando HTTP 200.
- [ ] T028 [SC1]: Unit tests para `Ruta.java` — `puedeAceptarPeso` y `asignarPedido` con valores válidos e inválidos.
- [ ] T029 [SC1]: Unit tests para `AsignarPedidoService` — mock de `RutaRepository` retorna ruta disponible, verificar flujo completo.
- [ ] T030 [SC1]: Unit tests para `AsignacionController` con MockMvc — POST válido → 200, body correcto.

**Checkpoint**: `POST /api/logistica/asignacion` con ruta existente funciona end-to-end. Tests T017–T020 pasan con TestContainers.

---

## Phase 4: Scenario 2 — Crear nueva ruta cuando no hay capacidad (P1)

**Goal**: Cuando no existe ninguna ruta disponible con capacidad suficiente, el sistema crea automáticamente una nueva ruta, selecciona el vehículo más eficiente según el peso del pedido, asigna el pedido y registra la fecha de despacho.

**Independent Test**: Dado que no hay ninguna ruta DISPONIBLE con capacidad para el pesoTotal, se hace `POST /api/logistica/asignacion` y se verifica que: la respuesta retorna HTTP 201, contiene un nuevo `idRuta`, tiene `idVehiculo` asignado del tipo correcto según el peso, y la `fecha_despacho` está registrada.

### Tests para Scenario 2

- [ ] T031 [P] [SC2]: Contract test en `AsignacionApiContractTest` — POST sin ruta disponible → HTTP 201, body contiene nuevo `idRuta`, `idVehiculo` correcto, `fecha_despacho` no nula.
- [ ] T032 [P] [SC2]: Integration test en `AsignacionServiceIntegrationTest` — sin rutas disponibles: verificar que `RutaRepository.save()` crea ruta nueva y `VehiculoRepository.findAvailableByCapacidad` es llamado con el peso correcto.
- [ ] T033 [P] [SC2]: Unit test en `SeleccionarVehiculoServiceTest` — verificar clasificación correcta para cada rango de peso: ≤1.5t → CAMIONETA, ≤5t → CAMION_SENCILLO, ≤25t → CAMION_MEDIANO, >25t → TRACTOCAMION_REGIONAL.

### Implementación de Scenario 2

- [ ] T034 [SC2]: Crear `VehiculoRepositoryAdapter.java` implementando `VehiculoRepository`: `findAvailableByCapacidad`, `findMaxCapacidad`.
- [ ] T035 [SC2]: Agregar método domain a `Ruta.java`: `static crearNueva(Vehiculo vehiculo, LocalDate fechaDespacho)` — factory method que retorna nueva Ruta en estado DISPONIBLE.
- [ ] T036 [SC2]: Implementar flujo "nueva ruta" en `AsignarPedidoService.java`:
  - Si `findAvailableWithCapacity` retorna vacío: llamar `SeleccionarVehiculoService`
  - Si hay vehículo: crear nueva `Ruta`, asignar pedido, guardar ruta y parada, registrar `fecha_despacho` (FR-006)
  - Si no hay vehículo: crear ruta con estado `PENDIENTE_VEHICULO` (edge case del spec)
  - Retornar HTTP 201
- [ ] T037 [SC2]: Actualizar `AsignacionController.java` para retornar HTTP 201 cuando se crea ruta nueva.
- [ ] T038 [SC2]: Unit tests para `SeleccionarVehiculoService` — todos los rangos de TipoVehiculo + caso `CapacidadExcedidaException`.
- [ ] T039 [SC2]: Unit tests para `AsignarPedidoService` — mock sin rutas disponibles, verificar creación de nueva ruta con vehículo correcto.

**Checkpoint**: El endpoint crea nuevas rutas correctamente. SC1 y SC2 funcionan end-to-end independientemente. Tests T031–T033 pasan.

---

## Phase 5: Scenario 3 — Cerrar ruta al alcanzar 95% de capacidad (P1)

**Goal**: Cuando la asignación de un pedido lleva la ruta a ≥95% de su capacidad total, el sistema automáticamente la marca como CERRADA para que no acepte más pedidos.

**Independent Test**: Dada una ruta DISPONIBLE con peso acumulado en 94% de su capacidad, al asignar un pedido que lleva el total a ≥95%, la ruta queda con estado CERRADA en la base de datos y el siguiente pedido no puede ser asignado a ella.

### Tests para Scenario 3

- [ ] T040 [P] [SC3]: Contract test en `AsignacionApiContractTest` — POST que lleva ruta a ≥95% → response contiene `estadoRuta: CERRADA`, el siguiente POST no asigna a esa ruta.
- [ ] T041 [P] [SC3]: Integration test en `AsignacionServiceIntegrationTest` — verificar que `ruta.cerrar()` es llamado y `RutaRepository.save()` persiste estado CERRADA exactamente cuando `pesoAcumulado / capacidadTotal >= 0.95`.
- [ ] T042 [P] [SC3]: Unit test en `RutaTest` — `estaLlena()` retorna false al 94.9% y true al 95.0% y al 100% (incluyendo el edge case de peso exactamente igual a capacidad restante).

### Implementación de Scenario 3

- [ ] T043 [SC3]: Agregar método domain a `Ruta.java`:
  - `estaLlena()` → true si `pesoAcumulado / capacidadTotal >= 0.95`
  - `cerrar()` → cambia estado a CERRADA
- [ ] T044 [SC3]: Actualizar `asignarPedido` en `AsignarPedidoService.java`:
  - Después de guardar: si `ruta.estaLlena()` → llamar `ruta.cerrar()` → guardar ruta nuevamente (FR-005)
- [ ] T045 [SC3]: Unit tests para `Ruta.java` — `estaLlena` al 94%, 95%, 100%, y edge case de peso exactamente igual a capacidad restante.
- [ ] T046 [SC3]: Unit tests para `AsignarPedidoService` — verificar que el cierre ocurre solo cuando corresponde y no antes.

**Checkpoint**: Los 3 escenarios del spec funcionan end-to-end. Todos los acceptance scenarios y edge cases cubiertos por tests automáticos. SC-001 a SC-004 verificables.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Hardening, observabilidad, validaciones y documentación que afectan todas las historias.

- [ ] T047: Logging estratégico con `@Slf4j`:
  - INFO al inicio/fin de cada asignación (idPedido, idRuta, resultado)
  - INFO cuando se crea nueva ruta o se cierra una ruta
  - WARN cuando una ruta supera el 90% de capacidad
  - ERROR en excepciones de negocio con contexto completo
- [ ] T048: Bean Validation en `AsignarPedidoRequest` — `@NotNull`, `@Positive` en `pesoTotal`, `@NotBlank` en `direccionEntrega`.
- [ ] T049: Completar `GlobalExceptionHandler` con códigos HTTP correctos:
  - `CapacidadExcedidaException` → HTTP 422 Unprocessable Entity
  - `VehiculoNoDisponibleException` → HTTP 202 Accepted (ruta creada como PENDIENTE_VEHICULO; documentar en Swagger)
  - `@Valid` violations → HTTP 400
- [ ] T050: Documentar API con `springdoc-openapi`: endpoint, request/response schemas, todos los códigos HTTP posibles incluyendo el 202 y su significado.
- [ ] T051: Test de concurrencia en `AsignacionServiceIntegrationTest` — lanzar 10 hilos simultáneos asignando pedidos a la misma ruta, verificar integridad del `peso_acumulado` final gracias a `SERIALIZABLE`.
- [ ] T052: `@ArchTest` con ArchUnit:
  - domain: sin imports de Spring, JPA, web
  - application: puede importar domain, no infrastructure
  - infrastructure: puede importar todo
- [ ] T053: Optimización de queries — revisar N+1 con JPA (`@EntityGraph` si aplica), validar índices con EXPLAIN ANALYZE en PostgreSQL.
- [ ] T054: Configurar `/actuator/health` con BD connectivity check.
- [ ] T055: Code coverage con Jacoco — target ≥80% global, 100% domain layer.
- [ ] T056: README con build/run commands, test execution (unit / integration / all), ejemplos curl del endpoint.
- [ ] T057: Pre-deployment checklist: tests 100% passing, ArchUnit passing, coverage ≥80%, sin vulnerabilidades en dependencias.

**Checkpoint**: Código production-ready. Todos los success criteria del spec (SC-001 a SC-004) verificables con tests automáticos.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias — puede iniciar de inmediato.
- **Foundational (Phase 2)**: Depende de Phase 1 — **bloquea todas las User Stories**.
- **Scenario 1 (Phase 3)**: Depende de Phase 2. No depende de otros scenarios.
- **Scenario 2 (Phase 4)**: Depende de Phase 2. Puede correr en paralelo con Phase 3 si hay equipo suficiente; en solitario, hacerla después de Phase 3.
- **Scenario 3 (Phase 5)**: Depende de Phase 3 (requiere `asignarPedido` implementado). No depende de Phase 4.
- **Polish (Phase 6)**: Depende de que los 3 scenarios estén completos.

### User Story Dependencies

- **Scenario 1 (P1)**: Puede iniciar en cuanto Phase 2 esté completa. Sin dependencias de otros scenarios.
- **Scenario 2 (P1)**: Puede iniciar en cuanto Phase 2 esté completa. Reutiliza `AsignarPedidoService` de SC1 pero debe ser independientemente testeable.
- **Scenario 3 (P1)**: Depende de que `Ruta.asignarPedido()` exista (Phase 3). Extiende el flujo, no lo reemplaza.

### Within Each User Story

- Domain models y métodos → Ports (interfaces) → Repository Adapters → Service → Controller
- Tests de contrato e integración (`[P]`) antes de la implementación
- Unit tests inline con cada componente
- Checkpoint al final antes de pasar al siguiente scenario

## Notes

- La etiqueta `[SC1]` / `[SC2]` / `[SC3]` en cada tarea mapea al scenario de aceptación del spec para trazabilidad directa.
- La etiqueta `[P]` indica test que debe escribirse antes de la implementación (test-first).
- Cada scenario debe ser independientemente demostrable al terminar su phase.
- Commit después de cada tarea completada con tests verdes.
- Evitar tareas que modifiquen el mismo archivo desde distintos scenarios en paralelo.
- El único controlador REST es `AsignacionController` — no crear clases duplicadas de entrada HTTP.
- `@Transactional(isolation = SERIALIZABLE)` en `AsignarPedidoService` es no negociable según el constraint del spec.
