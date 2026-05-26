# Implementation Plan: Solicitar Transportista (Módulo 2: Logística)

**Date**: April 22, 2026
**Spec**: `@/docs/specs/solicitar-transportista/spec.md`

## Summary

Implementar la integración entre el módulo de Logística de Despacho y Distribución y el módulo externo de Transportista para solicitar un transportista disponible y asignarlo a un vehículo específico. El sistema debe validar la disponibilidad del transportista antes de asignar, actualizar el campo `idTransportista` del vehículo y retornar errores claros ante ausencia o invalidez del transportista. Stack: Java 21 + Spring Boot MVC + Spring Data JPA + PostgreSQL, como microservicio independiente del módulo de logística.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Web (MVC), Spring Data JPA, Hibernate, Flyway, MapStruct, Lombok, Gradle
**Storage**: PostgreSQL
**Testing**: JUnit 5, Mockito, AssertJ, TestContainers, ArchUnit, Jacoco
**Target Platform**: Backend server (REST API) — microservicio independiente de logística
**Project Type**: Web Application (Backend) — stack bloqueante (no reactivo)
**Architecture**: Hexagonal (Ports & Adapters)
**Performance Goals**: Solicitar y recibir `idTransportista` disponible y asignarlo al vehículo en <1s (SC-001)
**Constraints**: Validación de disponibilidad del transportista obligatoria antes de asignar; `idTransportista` provisto por módulo externo de Transportista; sin acceso directo a su base de datos
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

**NOTA IMPORTANTE — Arquitectura Hexagonal Limpia:**
- **domain/**: Contiene SOLO lógica de negocio pura, SIN dependencias de frameworks. Incluye models, value objects, ports (in/out) y exceptions.
- **application/**: Contiene ÚNICAMENTE servicios que coordinan casos de uso. Sin DTOs, sin mappers, sin referencias a infraestructura. Los servicios reciben y retornan domain objects.
- **infrastructure/**: Contiene TODOS los adaptadores, DTOs, controllers, mappers, clientes HTTP y persistencia.
  - Los DTOs son conceptos de presentación/API, pertenecen exclusivamente a infraestructura.
  - El mapper (MapStruct) vive en infraestructura: es un detalle de implementación del adaptador.
  - El controller es el único punto donde se traducen DTOs → domain objects antes de llamar al use case, y domain objects → DTOs al responder.
  - El cliente HTTP (`TransportistaServiceClient`) es un adaptador de salida: implementa el puerto de dominio `TransportistaServicePort` y es el único componente con conocimiento de la API externa.

```text
src/main/java/co/edu/unimagdalena/storelogistic/transportista/
├── domain/
│   ├── models/
│   │   └── Vehiculo.java                           # Reutilizar de gestion-flota — agregar método asignarTransportista()
│   ├── values/
│   │   ├── EstadoVehiculo.java                     # Reutilizar de gestion-flota
│   │   └── EstadoTransportista.java                # NEW: DISPONIBLE | NO_DISPONIBLE
│   ├── ports/
│   │   ├── in/
│   │   │   └── SolicitarTransportistaUseCase.java  # NEW
│   │   └── out/
│   │       ├── VehiculoRepository.java             # Reutilizar de gestion-flota
│   │       └── TransportistaServicePort.java       # NEW: puerto de salida hacia módulo externo
│   └── exceptions/
│       ├── TransportistaNoDisponibleException.java # NEW — FR-006, Scenario 3
│       ├── TransportistaInvalidoException.java     # NEW — edge case idTransportista inválido
│       ├── VehiculoNotFoundException.java          # Reutilizar de gestion-flota
│       └── LogisticaException.java                 # Reutilizar de gestion-flota (base exception)
│
├── application/
│   └── services/
│       └── SolicitarTransportistaService.java      # NEW: implementa SolicitarTransportistaUseCase
│
└── infrastructure/
    ├── config/
    │   └── JpaConfig.java                          # Reutilizar de gestion-flota
    ├── persistence/
    │   ├── jpa/
    │   │   └── VehiculoJpaEntity.java              # Reutilizar de gestion-flota
    │   ├── repository/
    │   │   └── VehiculoRepositoryAdapter.java      # Reutilizar de gestion-flota
    │   └── jparepository/
    │       └── VehiculoSpringRepository.java       # Reutilizar de gestion-flota
    ├── client/
    │   └── TransportistaServiceClient.java         # NEW: adaptador HTTP — implementa TransportistaServicePort
    ├── web/
    │   ├── controller/
    │   │   └── SolicitarTransportistaController.java  # NEW
    │   └── dto/
    │       ├── SolicitarTransportistaResponse.java    # NEW: idVehiculo, idTransportista
    │       └── TransportistaDisponibleClientDTO.java  # NEW: DTO interno para deserializar respuesta del módulo externo
    ├── mapper/
    │   └── VehiculoTransportistaMapper.java           # NEW: MapStruct — Vehiculo ↔ DTOs de respuesta
    └── exception/
        ├── GlobalExceptionHandler.java                # Reutilizar — agregar nuevas excepciones
        └── ErrorResponse.java                         # Reutilizar de gestion-flota
```

**Structure Decision**: Arquitectura hexagonal limpia con tres capas (domain, application, infrastructure). El domain no tiene dependencias de frameworks. `TransportistaServicePort` en `domain/ports/out/` abstrae la comunicación con el módulo externo — el domain define el contrato, la infraestructura lo implementa. `SolicitarTransportistaController` es el único adaptador de entrada HTTP. No existen DTOs en application ni en domain.

```text
src/test/java/co/edu/unimagdalena/storelogistic/transportista/
├── unit/
│   ├── domain/
│   │   ├── models/
│   │   │   └── VehiculoTest.java                        # Reutilizar — agregar tests de asignarTransportista()
│   │   └── values/
│   │       └── EstadoTransportistaTest.java             # NEW
│   ├── application/
│   │   └── SolicitarTransportistaServiceTest.java       # NEW
│   └── infrastructure/
│       ├── TransportistaServiceClientTest.java          # NEW
│       ├── VehiculoTransportistaMapperTest.java         # NEW
│       └── SolicitarTransportistaControllerTest.java    # NEW
├── integration/
│   ├── VehiculoRepositoryIntegrationTest.java           # Reutilizar — agregar casos de asignación
│   └── SolicitarTransportistaIntegrationTest.java       # NEW
├── contract/
│   └── SolicitarTransportistaApiContractTest.java       # NEW
└── testdata/
    ├── fixtures/
    │   └── VehiculoFixture.java                         # Reutilizar de gestion-flota
    ├── builders/
    │   └── SolicitarTransportistaRequestBuilder.java    # NEW
    └── containers/
        └── PostgreSQLContainer.java                     # Reutilizar de gestion-flota
```

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verificar o configurar el proyecto Gradle, dependencias base y entorno de test, reutilizando de features previas donde aplique.

- [ ] T001 Verificar o crear estructura de directorios según el layout definido en este plan.
- [ ] T002 Verificar `build.gradle.kts` — incluir dependencias necesarias: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `postgresql`, `flyway-core`, `lombok`, `mapstruct`, `spring-retry`, `spring-boot-starter-aop` (requerido por Spring Retry), `springdoc-openapi-starter-webmvc-ui`, `spring-boot-starter-test`, `testcontainers`, `archunit`. Agregar si no están presentes.
- [ ] T003 Verificar `application.yml` — agregar propiedades de configuración:
  - `logistics.transporter.service.url` — URL base del módulo externo de Transportista.
  - `logistics.transporter.service.timeout-ms` — timeout de conexión/lectura (default 3000ms).
  - `logistics.transporter.service.retry.max-attempts` — reintentos máximos (default 3).
  - `logistics.transporter.service.retry.initial-interval-ms` — intervalo inicial backoff (default 500ms).
  - `logistics.transporter.service.retry.multiplier` — multiplicador backoff (default 2.0).
  Verificar profiles `dev`, `test`, `prod`.
- [ ] T004 Verificar scripts de migración Flyway — la tabla `vehiculos` ya debe existir con columna `id_transportista` de `gestion-flota`. Crear `V_solicitar_transportista__verify_column.sql` solo si la columna no existe o requiere ajuste.
- [ ] T005 Verificar `ArchUnit` — reglas de capas ya deben existir de `gestion-flota`: domain sin imports de Spring/JPA, application sin imports de infrastructure. Agregar regla que `client/` no puede ser referenciado desde `application/` ni `domain/`.
- [ ] T006 Verificar `PostgreSQLContainer.java` — reutilizar configuración TestContainers existente.
- [ ] T006b Crear `TransporterRestClientConfig.java` en `infrastructure/config/` — bean `transporterRestClient` de tipo `RestClient` con `baseUrl` de `logistics.transporter.service.url` y timeout configurable.
- [ ] T006c Crear `TransporterRetryConfig.java` en `infrastructure/config/` — `@EnableRetry`, bean `transporterRetryTemplate` con `maxAttempts=3`, `ExponentialBackOffPolicy` (initialInterval=500ms, multiplier=2.0). Política de no reintentar `TransporterNotAvailableException` ni `InvalidTransporterException`.

**Checkpoint**: Proyecto compila, configuración de URL externa presente, migraciones corren sin error, contenedor de test levanta correctamente.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Crear todos los componentes base de domain e infrastructure necesarios antes de implementar cualquier escenario. Ningún escenario puede comenzar hasta completar esta fase.

**⚠️ CRITICAL**: Ninguna implementación de escenario puede comenzar hasta completar esta fase.

- [ ] T007 Reutilizar `EstadoVehiculo.java` de `gestion-flota` — sin modificaciones.
- [ ] T008 Crear `EstadoTransportista.java` — enum con valores: `DISPONIBLE`, `NO_DISPONIBLE`. Sin dependencias de frameworks.
- [ ] T009 Reutilizar `Vehiculo.java` de `gestion-flota` — agregar método de dominio: `asignarTransportista(String idTransportista)` que actualiza el campo `idTransportista`. La lógica de negocio de la asignación vive en el domain model, no en el service.
- [ ] T010 Crear `TransportistaNoDisponibleException.java` — extiende `LogisticaException`. Mensaje claro indicando ausencia de transportistas (FR-006).
- [ ] T011 Crear `TransportistaInvalidoException.java` — extiende `LogisticaException`. Mensaje claro indicando `idTransportista` inexistente (edge case).
- [ ] T012 Crear puerto de entrada `SolicitarTransportistaUseCase.java` en `domain/ports/in/` — firma: `Vehiculo solicitar(String idVehiculo)`. Recibe y retorna domain objects exclusivamente, sin DTOs.
- [ ] T013 Crear puerto de salida `TransportistaServicePort.java` en `domain/ports/out/` — métodos:
  - `String obtenerDisponible()` — retorna el `idTransportista` de un transportista disponible. Lanza `TransportistaNoDisponibleException` si no hay ninguno.
  - `void validarExistencia(String idTransportista)` — lanza `TransportistaInvalidoException` si el id no existe en el módulo externo.
- [ ] T014 Reutilizar puerto de salida `VehiculoRepository.java` de `gestion-flota` — sin modificaciones.
- [ ] T015 Reutilizar `VehiculoJpaEntity.java` de `gestion-flota` — verificar que el campo `id_transportista` esté mapeado correctamente.
- [ ] T016 Reutilizar `VehiculoSpringRepository.java` y `VehiculoRepositoryAdapter.java` de `gestion-flota` — sin modificaciones.
- [ ] T017 Crear `TransporterServiceClient.java` en `infrastructure/client/` implementando `TransporterServicePort`:
  - Usa `RestClient` de Spring (no `RestTemplate`) inyectado via `@Qualifier("transporterRestClient")`.
  - Lee la URL base desde la propiedad `logistics.transporter.service.url`.
  - Maneja reintentos automáticos via `RetryTemplate` inyectado via `@Qualifier("transporterRetryTemplate")` con backoff exponencial (máximo 3 intentos, 500ms inicial, multiplicador 2.0) — configurado en `TransporterRetryConfig.java`.
  - `RestClient` configurado con timeout de conexión y lectura (default 3s) via `TransporterRestClientConfig.java`.
  - Llama a `GET /transportistas/disponible` para obtener transportista disponible y `GET /transportistas/{id}` para validar existencia.
  - Convierte respuestas HTTP en domain objects o excepciones de dominio — ningún detalle HTTP escapa de este adaptador.
- [ ] T018 Crear `TransportistaDisponibleClientDTO.java` en `infrastructure/web/dto/` — DTO de deserialización de la respuesta del módulo externo. Interno a infraestructura, nunca expuesto fuera de `TransportistaServiceClient`.
- [ ] T019 Crear `SolicitarTransportistaResponse.java` en `infrastructure/web/dto/` — campos: `idVehiculo`, `idTransportista`. Respuesta de la API REST de este módulo.
- [ ] T020 Crear `VehiculoTransportistaMapper.java` en `infrastructure/mapper/` como `@Component` MapStruct — conversión: `Vehiculo → SolicitarTransportistaResponse`.
- [ ] T021 Actualizar `GlobalExceptionHandler.java` — agregar handlers:
  - `TransportistaNoDisponibleException` → HTTP 409 con mensaje descriptivo (SC-004).
  - `TransportistaInvalidoException` → HTTP 400 con mensaje descriptivo (SC-004).
- [ ] T022 Verificar o crear `VehiculoFixture.java` — agregar fixtures con y sin `idTransportista` asignado para los tests de esta feature.
- [ ] T023 Crear `SolicitarTransportistaRequestBuilder.java` en `testdata/builders/`.
- [ ] T024 Unit tests para `Vehiculo.asignarTransportista()` — verificar que el campo `idTransportista` se actualiza correctamente; verificar que un vehículo sin transportista puede recibirlo; verificar que un vehículo con transportista previo admite reasignación (edge case del spec).
- [ ] T025 Unit tests para `EstadoTransportista.java` — verificar valores del enum.

**Checkpoint**: Domain models, value objects, ports, adaptadores de persistencia y cliente HTTP listos. Beans `transporterRestClient` y `transporterRetryTemplate` configurados. El proyecto compila sin errores. Tests T024–T025 pasan.

---

## Phase 3: Scenario 1 — Obtener idTransportista disponible (P1)

**Goal**: El módulo de Logística puede solicitar al módulo externo de Transportista un `idTransportista` disponible y recibirlo en menos de 1 segundo (SC-001).

**Independent Test**: Dado que el módulo externo de Transportista tiene al menos un transportista disponible, `POST /api/logistica/vehiculos/{idVehiculo}/transportista` retorna HTTP 200 con un `idTransportista` válido.

### Tests para Scenario 1

- [ ] T026 [P] [SC1] Contract test en `SolicitarTransportistaApiContractTest` — `POST /api/logistica/vehiculos/{idVehiculo}/transportista` con transportista disponible → HTTP 200, body contiene `idVehiculo` e `idTransportista` (FR-001, FR-002, SC-002).
- [ ] T027 [P] [SC1] Contract test en `SolicitarTransportistaApiContractTest` — sin transportistas disponibles → HTTP 409 con mensaje de error claro (FR-006, SC-004).
- [ ] T028 [P] [SC1] Integration test en `SolicitarTransportistaIntegrationTest` — flujo completo con DB real (TestContainers) y mock del `TransportistaServicePort` — verifica que el `idTransportista` retornado es válido y el vehículo queda actualizado en DB.

### Implementación de Scenario 1

- [ ] T029 [SC1] Implementar `SolicitarTransportistaService.java` implementando `SolicitarTransportistaUseCase`:
  - Recibe `String idVehiculo` (tipo primitivo de dominio).
  - Busca el vehículo via `VehiculoRepository.findById` — lanza `VehiculoNotFoundException` si no existe.
  - Llama a `TransportistaServicePort.obtenerDisponible()` — propaga `TransportistaNoDisponibleException` si no hay disponibles.
  - Llama a `vehiculo.asignarTransportista(idTransportista)` — la lógica de negocio vive en el domain model.
  - Persiste el vehículo actualizado via `VehiculoRepository.save(vehiculo)`.
  - Retorna `Vehiculo` (domain object). No conoce DTOs.
- [ ] T030 [SC1] Crear `SolicitarTransportistaController.java` con `POST /api/logistica/vehiculos/{idVehiculo}/transportista`:
  - Extrae `idVehiculo` del path.
  - Llama a `SolicitarTransportistaUseCase.solicitar(idVehiculo)`.
  - Mapea `Vehiculo` → `SolicitarTransportistaResponse` usando `VehiculoTransportistaMapper`.
  - Retorna HTTP 200 + `SolicitarTransportistaResponse`.
- [ ] T031 [SC1] Unit tests para `SolicitarTransportistaService`:
  - Happy path: `VehiculoRepository` y `TransportistaServicePort` mockeados; verificar que `vehiculo.asignarTransportista()` se llama con el id correcto y que `VehiculoRepository.save` es invocado.
  - Sin transportistas disponibles: `TransportistaServicePort` lanza `TransportistaNoDisponibleException` → el service la propaga sin envolver.
  - Vehículo no encontrado: `VehiculoRepository.findById` retorna vacío → service lanza `VehiculoNotFoundException`.
- [ ] T032 [SC1] Unit tests para `SolicitarTransportistaController` con MockMvc:
  - POST con vehículo existente y transportista disponible → HTTP 200 con body correcto.
  - POST sin transportistas disponibles → HTTP 409.
  - POST con vehículo inexistente → HTTP 404.

**Checkpoint**: Scenario 1 funciona end-to-end. `POST /api/logistica/vehiculos/{idVehiculo}/transportista` retorna `idTransportista` válido y persiste la asignación. Tests T026–T032 pasan.

---

## Phase 4: Scenario 2 — Asignar idTransportista a vehículo (P1)

**Goal**: El `idTransportista` obtenido se asigna al vehículo actualizando su campo en base de datos, con validación previa de existencia. El 100% de las asignaciones se registran correctamente (SC-002).

**Independent Test**: Dado un `idVehiculo` válido, la operación `POST /api/logistica/vehiculos/{idVehiculo}/transportista` actualiza el campo `idTransportista` del vehículo en la base de datos y lo retorna en la respuesta.

### Tests para Scenario 2

- [ ] T033 [P] [SC2] Contract test en `SolicitarTransportistaApiContractTest` — asignación exitosa → HTTP 200, body contiene `idVehiculo` e `idTransportista` correctos (FR-003, SC-002).
- [ ] T034 [P] [SC2] Contract test en `SolicitarTransportistaApiContractTest` — `idTransportista` inválido (no existe en módulo externo) → HTTP 400 con mensaje de error claro (FR-004, SC-003, SC-004).
- [ ] T035 [P] [SC2] Integration test en `VehiculoRepositoryIntegrationTest` — tras la asignación, `findById` del vehículo retorna el `idTransportista` actualizado en PostgreSQL.

### Implementación de Scenario 2

- [ ] T036 [SC2] Extender `SolicitarTransportistaService.solicitar()` — tras obtener el `idTransportista` disponible, llamar a `TransportistaServicePort.validarExistencia(idTransportista)` antes de invocar `vehiculo.asignarTransportista()`. La validación garantiza que el id obtenido del módulo externo sigue siendo válido en el momento de la asignación (FR-004).
- [ ] T037 [SC2] Unit tests adicionales para `SolicitarTransportistaService`:
  - Validación de existencia lanza `TransportistaInvalidoException` → el service la propaga; `VehiculoRepository.save` nunca es llamado.
  - Happy path con validación exitosa: verificar orden de llamadas — primero `obtenerDisponible`, luego `validarExistencia`, luego `asignarTransportista`, luego `save`.
- [ ] T038 [SC2] Unit tests para `VehiculoTransportistaMapper` — verificar que `Vehiculo` con `idTransportista` asignado se mapea correctamente a `SolicitarTransportistaResponse`.

**Checkpoint**: Scenario 2 funciona. La asignación persiste en DB y es recuperable. El 100% de los intentos con `idTransportista` inválido son rechazados (SC-003). Tests T033–T038 pasan.

---

## Phase 5: Scenario 3 — Manejo de error: sin transportistas disponibles (P1)

**Goal**: Cuando no hay transportistas disponibles, el sistema retorna un error claro sin modificar el vehículo (FR-006, SC-004).

**Independent Test**: Dado que el módulo externo no tiene transportistas disponibles, `POST /api/logistica/vehiculos/{idVehiculo}/transportista` retorna HTTP 409 con mensaje descriptivo. El vehículo no es modificado.

### Tests para Scenario 3

- [ ] T039 [P] [SC3] Contract test en `SolicitarTransportistaApiContractTest` — módulo externo sin transportistas disponibles → HTTP 409, body con mensaje de error (FR-006, SC-004).
- [ ] T040 [P] [SC3] Integration test en `SolicitarTransportistaIntegrationTest` — cuando `TransportistaServicePort` lanza `TransportistaNoDisponibleException`, verificar que `VehiculoRepository.save` nunca es llamado y el vehículo conserva su `idTransportista` original en DB.

### Implementación de Scenario 3

- [ ] T041 [SC3] Verificar que `GlobalExceptionHandler` maneja `TransportistaNoDisponibleException` → HTTP 409 con body `ErrorResponse` descriptivo. Ya creado en T021; verificar en este punto con tests de contrato.
- [ ] T042 [SC3] Unit test para `GlobalExceptionHandler` — `TransportistaNoDisponibleException` produce HTTP 409 con mensaje correcto; `TransportistaInvalidoException` produce HTTP 400 con mensaje correcto.

**Checkpoint**: Los 3 escenarios del spec funcionan end-to-end. Tests T039–T042 pasan.

---

## Phase 6: Edge Cases

**Purpose**: Cubrir los casos edge explícitos del spec.

- [ ] T043 [EC] Edge case — `idTransportista` ya asignado a otro vehículo: `TransportistaServicePort.obtenerDisponible()` es responsable de retornar únicamente transportistas cuyo estado sea `DISPONIBLE` según el módulo externo. Verificar en unit tests de `SolicitarTransportistaService` que si el módulo externo retorna un transportista no disponible, `validarExistencia` lanza `TransportistaInvalidoException` → HTTP 400.
- [ ] T044 [EC] Edge case — reasignación: un vehículo con `idTransportista` existente puede ser reasignado a uno diferente. Verificar en `VehiculoTest` que `Vehiculo.asignarTransportista()` sobreescribe el valor anterior sin lanzar excepción (FR-005). Agregar contract test: vehículo con transportista previo + POST → HTTP 200 con nuevo `idTransportista`.
- [ ] T045 [EC] Edge case — `idVehiculo` inexistente: `VehiculoRepository.findById` retorna vacío → `VehiculoNotFoundException` → HTTP 404. Ya cubierto en T032; verificar con integration test que el vehículo no existe en DB antes del request.
- [ ] T046 [EC] Unit y integration tests para cada edge case no cubierto en fases anteriores.

**Checkpoint**: Todos los edge cases del spec cubiertos con tests automáticos.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Hardening, observabilidad, validaciones y documentación.

- [ ] T047 Logging estratégico con `@Slf4j` en `SolicitarTransportistaService`:
  - `INFO` al iniciar la solicitud (idVehiculo recibido).
  - `INFO` al asignar exitosamente (idVehiculo, idTransportista asignado).
  - `WARN` cuando no hay transportistas disponibles (idVehiculo solicitante).
  - `ERROR` en excepciones inesperadas con contexto completo.
- [ ] T048 Bean Validation en el controller — `@PathVariable String idVehiculo` con `@NotBlank`. Verificar HTTP 400 ante `idVehiculo` vacío.
- [ ] T049 Documentar API con `springdoc-openapi`: endpoint `POST /api/logistica/vehiculos/{idVehiculo}/transportista`, request/response schemas, todos los códigos HTTP posibles (200, 400, 404, 409).
- [ ] T050 `@ArchTest` con ArchUnit — verificar:
  - `domain/` sin imports de Spring, JPA ni web.
  - `application/` puede importar domain, no infrastructure.
  - `infrastructure/` puede importar todo.
  - Ninguna clase en `domain/` o `application/` importa de `infrastructure/`.
  - `client/` no es importado desde `application/` ni `domain/`.
- [ ] T051 Test de performance — `POST /api/logistica/vehiculos/{idVehiculo}/transportista` con mock del módulo externo responde en <1s (SC-001).
- [ ] T052 Code coverage con Jacoco — verificar ≥80% global, 100% domain layer.
- [ ] T053 README con ejemplos curl del endpoint, instrucciones de build/run, ejecución de tests (unit / integration / all).
- [ ] T054 Pre-deployment checklist: tests 100% passing, ArchUnit passing, coverage ≥80%, sin vulnerabilidades en dependencias.

**Checkpoint**: Código production-ready. SC-001, SC-002, SC-003 y SC-004 verificables con tests automáticos.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias — puede iniciar de inmediato.
- **Foundational (Phase 2)**: Depende de Phase 1 — **bloquea todos los escenarios**.
- **Scenario 1 (Phase 3)**: Depende de Phase 2. Sin dependencias de otros escenarios.
- **Scenario 2 (Phase 4)**: Depende de Phase 3 — extiende el service ya implementado.
- **Scenario 3 (Phase 5)**: Depende de Phase 2. Puede ejecutarse en paralelo con Phases 3 y 4 si hay equipo suficiente; en solitario, hacerla después de Phase 4.
- **Edge Cases (Phase 6)**: Depende de que los 3 escenarios estén completos.
- **Polish (Phase 7)**: Depende de Phase 6.

### Within Each Phase

- Domain models y métodos → Ports (interfaces) → Repository/Client Adapters → Service → Controller.
- Tests de contrato e integración (`[P]`) antes de la implementación del componente correspondiente.
- Unit tests inline con cada componente.
- Checkpoint al final antes de pasar a la siguiente fase.

---

## Notes

- **DTOs exclusivamente en infrastructure**: `SolicitarTransportistaResponse` y `TransportistaDisponibleClientDTO` viven en `infrastructure/web/dto/`. Nunca en `application/` ni en `domain/`.
- **Mapper exclusivamente en infrastructure**: `VehiculoTransportistaMapper` vive en `infrastructure/mapper/`. El controller es el único que lo invoca.
- **El service opera solo con domain objects**: `SolicitarTransportistaService` recibe `String idVehiculo` y retorna `Vehiculo`. No conoce `SolicitarTransportistaResponse` ni ningún otro DTO.
- **La lógica de negocio de la asignación vive en el domain model**: `Vehiculo.asignarTransportista(String idTransportista)` es el único lugar donde se modifica el campo `idTransportista`. El service orquesta, el model decide.
- **`TransportistaServicePort` desacopla el domain del HTTP externo**: en tests se mockea este puerto. El domain nunca sabe que el módulo externo existe como servicio HTTP.
- **`TransportistaServiceClient` convierte excepciones HTTP en excepciones de dominio**: ningún `HttpClientErrorException`, `RestClientException` ni código HTTP escapa del adaptador. Solo `TransportistaNoDisponibleException` y `TransportistaInvalidoException` salen de él.
- Etiqueta `[P]` para tests que deben escribirse antes de la implementación (test-first).
- Etiquetas `[SC1]`, `[SC2]`, `[SC3]`, `[EC]` mapean directamente a los escenarios del spec para trazabilidad.
- Commit después de cada tarea completada con tests verdes.
- Evitar tareas que modifiquen el mismo archivo desde distintas fases en paralelo.
