# Plan de Implementación: Consultar Paradas de Rutas

**Fecha**: 24 de abril de 2026  
**Rama**: `feature/consultar-paradas`

## Resumen

Dos endpoints REST para que el transportista consulte sus paradas:

1. **Lista** `GET /api/v1/logistics/routes/{routeId}/stops?carrierId={carrierId}` — retorna paradas básicas sin llamadas externas.
2. **Detalle** `GET /api/v1/logistics/routes/{routeId}/stops/{stopId}?carrierId={carrierId}` — retorna detalle con método de pago del Módulo Financiero.

**Autorización**: La ruta debe estar asignada a un vehículo cuyo `id_transportista = carrierId`. El rechazo (403) es idéntico para ruta inexistente y ruta de otro transportista (ambigüedad intencional, FR-003).  
**Stack**: Java 21, Spring Boot 3.x, JPA, Flyway, MapStruct, Testcontainers.  
**Arquitectura**: Hexagonal — dominio puro sin imports de Spring/JPA.

---

## Estructura del Código

```
co.edu.unimagdalena.storelogistic.consultar/
├── domain/
│   ├── exceptions/
│   │   └── AccessDeniedException.java          # "Acceso denegado" — sin revelar existencia
│   └── ports/
│       ├── in/
│       │   ├── QueryStopsUseCase.java          # List<Stop> query(routeId, carrierId)
│       │   └── GetStopDetailUseCase.java       # Stop get(routeId, stopId, carrierId)
│       └── out/
│           └── (usa RouteRepository y StopRepository del módulo route)
│
├── application/
│   └── services/
│       ├── AuthorizationService.java           # verifyCarrierHasAccess(routeId, carrierId)
│       ├── QueryStopsService.java              # implementa QueryStopsUseCase
│       └── GetStopDetailService.java           # implementa GetStopDetailUseCase — retorna solo Stop
│
└── infrastructure/
    ├── exception/
    │   └── ConsultarExceptionHandler.java      # AccessDeniedException → 403
    ├── mapper/
    │   └── QueryStopsMapper.java               # Stop → StopSummaryDTO, Stop+Payment → StopDetailDTO
    └── web/
        ├── controller/
        │   └── QueryStopsController.java       # GET /stops y GET /stops/{stopId}; ensambla Stop+Payment
        └── dto/
            ├── StopSummaryDTO.java             # idStop, sequence, deliveryAddress, orderId, status
            ├── StopDetailDTO.java              # todos los campos + paymentMethod + totalACobrar
            └── QueryStopsResponse.java         # routeId, carrierId, totalStops, stops
```

**Reusa del módulo `route`**: `RouteRepository`, `StopRepository`, `RouteJpaEntity`, `StopJpaEntity`, `RouteAssignmentMapper`, `Stop`, `Route`.  
**Reusa del módulo `paymentmethod`**: `ConsultPaymentMethodUseCase`, `OrderPaymentMethod`.  
**Sin tabla `carrier`**: el transportista es un módulo externo; su ID (`id_transportista BIGINT`) vive en `vehiculos`.

---

## Modelo de Datos

### Cambios en schema existente (Flyway V7)

```sql
-- Añadir customer_contact a stops (nullable — se llena al crear la parada)
ALTER TABLE stops ADD COLUMN IF NOT EXISTS customer_contact VARCHAR(200);

-- Índice para búsqueda por ruta y secuencia
CREATE INDEX IF NOT EXISTS idx_stops_sequence ON stops(id_route, sequence);
```

> No se crea tabla `carrier`. La autorización se resuelve con JOIN a `vehiculos`.

### Query de autorización

```sql
SELECT r.* FROM routes r
JOIN vehiculos v ON r.id_vehicle = v.id_vehiculo
WHERE r.id_route = :routeId
  AND v.id_transportista = :carrierId
```

> Se usa `nativeQuery = true` porque `RouteJpaEntity.vehicleId` es un `Long` plano sin `@ManyToOne` — el JOIN cross-entidad JPQL no aplica.

---

## Fase 1 — Configuración

- [x] T001 Verificar que `V1`–`V5` existen. Crear `V7__add_customer_contact_and_indexes.sql` (eliminar V6 si existe — la tabla carrier NO corresponde a este módulo).
- [x] T002 Verificar `build.gradle` — todas las dependencias ya están desde el módulo `route` y `paymentmethod`. Sin cambios.
- [x] T003 Verificar `application.yml` — perfiles `dev`, `test`, `prod`. Sin cambios.

**Checkpoint**: Migraciones V7 corren sin errores. ✅

---

## Fase 2 — Fundacional

- [x] T004 Verificar `AccessDeniedException.java` — mensaje `"Acceso denegado"`, sin revelar existencia.
- [x] T005 Crear `QueryStopsUseCase.java` — `List<Stop> query(Long routeId, Long carrierId)`.
- [x] T006 Crear `GetStopDetailUseCase.java` — `Stop get(Long routeId, Long stopId, Long carrierId)`. Retorna solo `Stop`; el ensamblado con `OrderPaymentMethod` ocurre en el controller.
- [x] T007 Extender `RouteRepository` (puerto existente):
  - Agregar `Optional<Route> findByIdAndCarrierId(Long routeId, Long carrierId)` — implementado con native SQL JOIN a `vehiculos` por `id_transportista`.
- [x] T008 Extender `StopRepository` (puerto existente):
  - Verificar que `findByRouteIdOrderBySequence(Long routeId)` ya existe.
  - Agregar `Optional<Stop> findByIdAndRouteId(Long stopId, Long routeId)`.
- [x] T009 Extender `RouteSpringRepository` — agregar query native SQL con JOIN a vehiculos:
  ```java
  @Query(value = """
      SELECT r.* FROM routes r
      JOIN vehiculos v ON r.id_vehicle = v.id_vehiculo
      WHERE r.id_route = :routeId
        AND v.id_transportista = :carrierId
      """, nativeQuery = true)
  Optional<RouteJpaEntity> findByRouteIdAndCarrierId(@Param("routeId") Long routeId,
                                                     @Param("carrierId") Long carrierId);
  ```
- [x] T010 Extender `StopSpringRepository` — agregar `Optional<StopJpaEntity> findByStopIdAndRouteId(Long stopId, Long routeId)`.
- [x] T011 Implementar nuevos métodos en `RouteRepositoryAdapter` y `StopRepositoryAdapter`.
- [x] T012 Crear DTOs:
  - `StopSummaryDTO` — `idStop`, `sequence`, `deliveryAddress`, `orderId`, `status`.
  - `StopDetailDTO` — todos los anteriores + `customerContact`, `paymentMethod`, `totalACobrar`.
  - `QueryStopsResponse` — `routeId`, `carrierId`, `totalStops`, `List<StopSummaryDTO> stops`.
- [x] T013 Crear `QueryStopsMapper` (abstract class — Stop usa fluent accessors, no JavaBean getters):
  - `toSummaryDTO(Stop) → StopSummaryDTO`.
  - `toDetailDTO(Stop, OrderPaymentMethod) → StopDetailDTO`.
  - `toResponse(Long, Long, List<Stop>) → QueryStopsResponse`.
- [x] T014 Crear `AuthorizationService` — `verifyCarrierHasAccess(routeId, carrierId)` lanza `AccessDeniedException` si `RouteRepository.findByIdAndCarrierId` retorna vacío.
- [x] T015 Mantener `ConsultarExceptionHandler` — `AccessDeniedException → HTTP 403`.
- [x] T016 Prueba unitaria `AccessDeniedException` — mensaje no revela existencia.
- [x] T017 Prueba unitaria `AuthorizationService` — 3 casos: asignada/otro transportista/inexistente.

**Checkpoint**: Compila. T016–T017 en verde. ✅

---

## Fase 3 — Endpoint Lista (QueryStopsService)

- [x] T018 [P] Prueba de contrato `QueryStopsApiContractTest` — `GET /stops?carrierId=X` con ruta asignada → 200, lista ordenada por sequence, campos presentes (FR-001, SC-001).
- [x] T019 [P] Prueba de contrato — ruta no asignada → 403 `ACCESO_DENEGADO` (FR-003).
- [x] T020 [P] Prueba de contrato — ruta sin paradas → 200 lista vacía.
- [x] T021 [P] Prueba de contrato — `carrierId` faltante → 400.
- [x] T022 Implementar `QueryStopsService`:
  - `@Transactional(readOnly = true)`, `@Slf4j`.
  - Llama a `AuthorizationService.verifyCarrierHasAccess` → lanza `AccessDeniedException` si falla.
  - Llama a `StopRepository.findByRouteIdOrderBySequence(routeId)`.
  - Logging INFO inicio/fin, WARN en acceso denegado.
- [x] T023 Implementar `QueryStopsController` — `GET /{routeId}/stops`.
- [x] T024 Prueba unitaria `QueryStopsService` — camino feliz y acceso denegado (StopRepository no llamado).
- [x] T025 Prueba unitaria `QueryStopsController` — 200/403/400.

**Checkpoint**: `GET /stops` funciona end-to-end. T018–T021 pasan con Testcontainers. ✅

---

## Fase 4 — Endpoint Detalle (GetStopDetailService)

- [x] T026 [P] Pruebas de contrato en `QueryStopsApiContractTest`:
  - Parada → 200 con `customerContact` y `status`.
  - Ruta no asignada al transportista → 403.
  - `stopId` que no pertenece a `routeId` → 403.
- [x] T027 Implementar `GetStopDetailService`:
  - Llama a `AuthorizationService.verifyCarrierHasAccess(routeId, carrierId)`.
  - Llama a `StopRepository.findByIdAndRouteId(stopId, routeId)` → `AccessDeniedException` si vacío.
  - Retorna solo `Stop` — no llama al Módulo Financiero (esa responsabilidad es del controller).
- [x] T028 Agregar `GET /{routeId}/stops/{stopId}` a `QueryStopsController`:
  - Llama a `GetStopDetailUseCase.get(routeId, stopId, carrierId)`.
  - Llama a `ConsultPaymentMethodUseCase.consult(stop.orderId())`.
  - Ensambla Stop + OrderPaymentMethod y delega al mapper.
- [x] T029 Prueba unitaria `GetStopDetailService` — 3 casos: camino feliz, acceso denegado, stopId incorrecto.
- [x] T030 Prueba unitaria controller — 200/403/400 (stopId = 0).

**Checkpoint**: `GET /stops/{stopId}` funciona end-to-end con MockFinanceModuleClient. ✅

---

## Fase 5 — Pulido

- [x] T031 Bean Validation: `@Positive` en `routeId`, `stopId`, `carrierId`. HTTP 400 con valores ≤ 0 verificado.
- [ ] T032 Verificar ausencia de N+1: `findByRouteIdOrderBySequence` usa índice en `(id_route, sequence)`.
- [ ] T033 Prueba de rendimiento: ruta con 20 paradas → lista < 3s, detalle < 3s (SC-001/SC-002).

---

## Notas Clave

- **Sin tabla `carrier`**: El transportista es externo. Solo usamos `vehiculos.id_transportista (BIGINT)` para autorizar.
- **Autorización por native SQL JOIN**: `routes JOIN vehiculos ON id_vehicle = id_vehiculo WHERE id_transportista = carrierId`. Retorna `Optional.empty()` para ruta inexistente Y para ruta de otro transportista — ambigüedad intencional (FR-003).
- **Lista sin pago**: `GET /stops` no llama al Módulo Financiero — rápido, sin dependencias externas.
- **Detalle con pago**: El controller llama a `ConsultPaymentMethodUseCase.consult(orderId)` una sola vez por request — usa `MockFinanceModuleClient` hasta que el módulo financiero esté disponible.
- **`GetStopDetailService` retorna solo `Stop`**: el ensamblado con `OrderPaymentMethod` es responsabilidad del controller, manteniendo el service puro.
- **`QueryStopsMapper` es abstract class**: necesario porque `Stop` usa fluent accessors (`stopId()`) en lugar de JavaBean getters (`getStopId()`), lo que impide el mapeo automático de MapStruct.
- **`totalACobrar: 0`** para CARTERA_COMERCIAL — el backend devuelve `BigDecimal(0)` (no null). El frontend debe mostrar "No cobrar", independientemente del valor numérico.
- **`@Transactional(readOnly = true)`** en ambos services de aplicación.
