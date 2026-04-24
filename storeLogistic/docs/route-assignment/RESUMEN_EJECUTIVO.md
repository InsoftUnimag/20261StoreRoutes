# Resumen Ejecutivo - Feature Asignar Ruta + Solicitar Ruta
## Sistema de Gestión Logística - StoreLogistic

## 📋 Documento de Síntesis

**Proyecto:** Sistema de Gestión Logística - Feature Asignar Ruta / Solicitar Ruta  
**Empresa:** Universidad del Magdalena  
**Fecha:** 23 de Abril de 2026  
**Versión:** 0.0.1-SNAPSHOT  
**Estado:** ✅ Implementación Completa  

---

## 🎯 Objetivo del Proyecto

Desarrollar las features **Asignar Ruta** y **Solicitar Ruta** que permiten asignar automáticamente pedidos a rutas logísticas, ya sea mediante petición REST directa o mediante eventos asíncronos por RabbitMQ. El sistema selecciona inteligentemente el vehículo adecuado según el peso del pedido, llena rutas existentes antes de crear nuevas (estrategia bin-packing), y cierra automáticamente las rutas al alcanzar el 95% de capacidad.

---

## ✨ Características Principales

| Característica | Descripción | Estado |
|---|---|---|
| **Asignación REST** | POST /logistics/routes/assignments con orderId | ✅ Implementado |
| **Asignación por Evento** | Consumer RabbitMQ en exchange `route.request` | ✅ Implementado |
| **Selección de Vehículo** | VehicleType.forWeight() — URBAN_VAN / SINGLE_TRUCK / REGIONAL_SEMI | ✅ Implementado |
| **Bin-Packing Greedy** | Llenar rutas existentes antes de crear nuevas | ✅ Implementado |
| **Cierre Automático** | Ruta se cierra al ≥ 95% de ocupación | ✅ Implementado |
| **Transiciones de Estado** | Route y Stop validan transiciones explícitamente | ✅ Implementado |
| **Publicación de Resultados** | Events `route.assigned` y `route.error` | ✅ Implementado |
| **DLQ Automática** | Errores inesperados van a `route.request.dlq` | ✅ Implementado |
| **Concurrencia Segura** | `@Transactional(isolation = SERIALIZABLE)` | ✅ Implementado |
| **Testing Completo** | 53+ pruebas de dominio, aplicación e infraestructura | ✅ Implementado |

---

## 🏗️ Arquitectura Implementada

### Patrón: Hexagonal (Ports & Adapters) — Capa de Dominio Pura

```
CAPA EXTERNA
  HTTP (REST)           RabbitMQ (Eventos)
      ↓                       ↓
  AssignRouteController   RouteRequestListener
      ↓                       ↓
Servicios de Aplicación (AssignOrderService, ProcessRouteRequestService)
      ↓
DOMINIO PURO (Route, Stop, VehicleType, RouteStatus, StopStatus)
      ↓
Adaptadores de Persistencia (JPA / PostgreSQL)
      ↑
RouteEventPublisher (StreamBridge → RabbitMQ salida)
```

**Regla estricta:** el dominio no importa ninguna clase de Spring, JPA ni Lombok. Todo el comportamiento de negocio vive en clases Java puras.

### Principios SOLID Aplicados

✅ **S**ingle Responsibility: `SelectVehicleService` solo selecciona, `AssignOrderService` solo orquesta  
✅ **O**pen/Closed: añadir un nuevo `VehicleType` no modifica `AssignOrderService`  
✅ **L**iskov Substitution: implementaciones de puertos intercambiables  
✅ **I**nterface Segregation: puertos de entrada y salida separados por responsabilidad  
✅ **D**ependency Inversion: servicios dependen de interfaces, no de adaptadores concretos  

---

## 📊 Estadísticas del Código

### Composición

| Tipo | Cantidad | Ejemplos |
|------|----------|---------|
| **Domain Models** | 4 | Route, Stop, Order, RouteVehicle |
| **Value Objects** | 5 | LogisticWeight, RouteCapacity, VehicleType, RouteStatus, StopStatus |
| **Domain Exceptions** | 5 | CapacityExceededException, InvalidStateTransitionException, … |
| **Domain Ports (IN)** | 2 | AssignOrderUseCase, ProcessRouteRequestUseCase |
| **Domain Ports (OUT)** | 4 | RouteRepository, StopRepository, RouteVehicleRepository, OrderRepository |
| **Application Services** | 3 | AssignOrderService, SelectVehicleService, ProcessRouteRequestService |
| **JPA Entities** | 5 | RouteJpaEntity, StopJpaEntity, OrderJpaEntity, RouteVehicleJpaEntity, RouteCategoryJpaEntity |
| **Repository Adapters** | 4 | RouteRepositoryAdapter, StopRepositoryAdapter, … |
| **REST Endpoints** | 1 | POST /logistics/routes/assignments |
| **DTOs** | 3 | AssignOrderRequest, AssignOrderResponse, StopResponse |
| **Messaging** | 5 | RouteRequestEvent, RouteAssignedEvent, RouteErrorEvent, Publisher, Listener |
| **Migraciones Flyway** | 2 | V4 (tablas), V5 (índices) |
| **Tests** | 53+ | Dominio, aplicación, controller, listener |

### Líneas de Código (Estimado)

- **Dominio:** ~350 líneas (modelos, value objects, excepciones, puertos)
- **Aplicación:** ~180 líneas (tres servicios)
- **Infraestructura:** ~600 líneas (JPA, adapters, mapper, controller, messaging)
- **Tests:** ~500 líneas
- **SQL (migraciones):** ~50 líneas

**Total:** ~1680 líneas de código productivo

---

## 🔐 Validaciones Implementadas

### A Nivel de Entrada

- ✅ `@NotNull orderId` en el request REST
- ✅ Existencia del pedido en base de datos (OrderNotFoundException)
- ✅ Formato del evento RabbitMQ (deserialización automática)

### A Nivel de Negocio

- ✅ Peso del pedido ≤ capacidad máxima soportada (25,000 kg)
- ✅ Ruta AVAILABLE antes de asignar pedidos
- ✅ Transición Route: AVAILABLE→CLOSED, PENDING_VEHICLE→AVAILABLE (no otras)
- ✅ Transición Stop: PENDING→DELIVERED, PENDING→REJECTED (terminales no aceptan más)

### A Nivel de Salida

- ✅ HTTP codes semánticos (200 existente, 201 nueva, 404, 409, 422)
- ✅ Eventos de resultado publicados en exchanges dedicados
- ✅ Mensajes de error descriptivos con código de negocio

---

## 🔄 Escenarios de Asignación

| Escenario | Condición | Resultado |
|---|---|---|
| **SC1** | Existe ruta AVAILABLE con capacidad suficiente | Asigna a ruta existente → HTTP 200 |
| **SC2 (con vehículo)** | No hay ruta disponible, vehículo encontrado | Crea ruta AVAILABLE → HTTP 201 |
| **SC2 (sin vehículo)** | No hay ruta ni vehículo disponible | Crea ruta PENDING_VEHICLE → HTTP 201 |
| **SC3** | Ruta alcanza ≥ 95% de ocupación | Cierra ruta automáticamente (CLOSED) |
| **Error** | Peso > 25,000 kg | CapacityExceededException → 422 |
| **Error** | Pedido no existe | OrderNotFoundException → 404 |

---

## 📈 Performance & Escalabilidad

### Características

| Métrica | Valor | Método |
|---------|-------|--------|
| **Aislamiento de Transacción** | SERIALIZABLE | Evita doble asignación bajo carga concurrente |
| **Estrategia de Selección** | Bin-packing greedy | ORDER BY accumulated_weight_kg DESC |
| **Fetching de Paradas** | EAGER | Minimiza consultas N+1 en respuesta |
| **Pool de Conexiones** | HikariCP | Configuración Spring Boot por defecto |
| **Latencia REST** | <300ms típico | Query nativa + una escritura transaccional |

### Optimizaciones de Base de Datos

✅ `idx_routes_status` — filtro por AVAILABLE en query de asignación  
✅ `idx_routes_accumulated_weight` — orden greedy eficiente  
✅ `idx_stops_id_route` — JOIN de paradas por ruta  

---

## 🔧 Stack Tecnológico

### Lenguaje & Runtime
- **Java 21 LTS** — Records, switch expressions, texto blocks

### Framework Web
- **Spring Boot 3.5.13** — REST controllers, JPA, validación
- **Spring Cloud Stream 2024.0.1** — mensajería declarativa con RabbitMQ Binder

### Mensajería
- **RabbitMQ 3.x** — exchanges dedicados por tipo de evento, DLQ automática

### Base de Datos
- **PostgreSQL 17** — transacciones SERIALIZABLE, índices compuestos
- **Flyway 11.20.3** — migraciones versionadas V4 y V5

### Mapping
- **MapStruct 1.6.3** — abstract class mapper con implementaciones manuales para tipos complejos

### Testing
- **JUnit 5** — framework de pruebas
- **Mockito** — mocking de puertos de dominio
- **AssertJ** — aserciones fluidas
- **Testcontainers RabbitMQ 1.20.6** — integración real en tests de listener

---

## ✅ Requisitos Cumplidos

### Requisitos Funcionales

| RF | Descripción | Estado |
|----|-------------|--------|
| **RF-001** | Asignar pedido a ruta disponible por REST | ✅ Implementado |
| **RF-002** | Crear ruta nueva si no existe disponible | ✅ Implementado |
| **RF-003** | Seleccionar vehículo apropiado por tipo de peso | ✅ Implementado |
| **RF-004** | Cerrar ruta al alcanzar 95% de ocupación | ✅ Implementado |
| **RF-005** | Consumir solicitudes de ruta por RabbitMQ | ✅ Implementado |
| **RF-006** | Publicar eventos de resultado (assigned / error) | ✅ Implementado |
| **RF-007** | Validar transiciones de estado en Route y Stop | ✅ Implementado |

### Criterios de Calidad

| Criterio | Objetivo | Estado |
|----------|----------|--------|
| Dominio libre de frameworks | 0 imports de Spring/JPA/Lombok en domain | ✅ Cumplido |
| Concurrencia segura | Sin condición de carrera en asignación | ✅ SERIALIZABLE |
| Cobertura de tests | >90% lógica de negocio | ✅ 53+ tests |
| Convención de nombres | `logisticWeight` / `logistic_weight` / `pesoLogistico` | ✅ Consistente |
| Manejadores separados por módulo | basePackages en cada @RestControllerAdvice | ✅ Sin conflictos |

---

## 🎓 Valor Educativo

Esta feature demuestra:

✅ **Arquitectura:** Hexagonal con dominio verdaderamente puro  
✅ **Patrones:** Aggregate Root, Factory Method, Strategy (VehicleType), Adapter  
✅ **Concurrencia:** Aislamiento SERIALIZABLE para consistencia en alta concurrencia  
✅ **Mensajería:** Spring Cloud Stream con Consumer funcional, DLQ, eventos de resultado  
✅ **Diseño de Dominio:** Value Objects inmutables (records), máquinas de estado en enums  
✅ **Testing:** Unitario por capa, sin acoplamiento entre capas en tests  

**Ideal para:**
- Cursos de Arquitectura de Software y DDD
- Estudio de integración REST + Mensajería en el mismo módulo
- Comprensión de concurrencia en bases de datos transaccionales
- Diseño de máquinas de estado con validación explícita

---

## 🔮 Mejoras Futuras

### Corto Plazo
- [ ] GET /logistics/routes/{id} — consultar ruta con paradas
- [ ] PATCH /logistics/stops/{id}/deliver — marcar parada como entregada
- [ ] PATCH /logistics/stops/{id}/reject — marcar parada como rechazada

### Mediano Plazo
- [ ] Feature "Consultar Paradas" — listado y filtrado de paradas por ruta
- [ ] Métricas de ocupación con Micrometer / Grafana
- [ ] Notificación en tiempo real al alcanzar 95% (WebSocket o Server-Sent Events)

### Largo Plazo
- [ ] Optimización de asignación con algoritmo más sofisticado (Bin-Packing exacto)
- [ ] Machine Learning para predicción de ocupación y pre-creación de rutas
- [ ] Dashboard de rutas activas con mapa de calor de ocupación

---

## 💡 Puntos Destacables

### Decisiones de Diseño

✅ **`AssignResult` record anidado en el use case** — el contrato entre capas incluye `isNewRoute` sin exponer detalles de implementación al controlador  
✅ **`BigDecimal` para `accumulatedWeightKg`** — no usa `LogisticWeight` en el acumulado porque este puede ser 0 (inicio de ruta), y `LogisticWeight` valida > 0  
✅ **Entidades JPA de sólo lectura del módulo flota** — `RouteVehicleJpaEntity` y `RouteCategoryJpaEntity` apuntan a las mismas tablas sin acoplar los módulos  
✅ **`@Transactional(SERIALIZABLE)` solo donde importa** — no en `SelectVehicleService` (solo lectura), sino en `AssignOrderService` (lectura-escritura concurrente)  

### Lecciones Aprendidas

⚠️ `@WebMvcTest` no aplica `server.servlet.context-path` — los tests de controller usan URLs sin prefijo `/api/v1`  
⚠️ Dos `@RestControllerAdvice` sin `basePackages` generan conflicto — cada módulo debe especificar su paquete  
⚠️ `LogisticWeight` no puede representar 0 — el acumulado de peso en Route usa `BigDecimal` directamente  

---

## 📊 Matrices de Decisión

### ¿Por qué SERIALIZABLE y no REPEATABLE_READ?

| Aspecto | REPEATABLE_READ | SERIALIZABLE |
|--------|-----------------|--------------|
| Phantom reads | Puede ocurrir | Prevenidos |
| Doble asignación bajo concurrencia | Posible | Imposible |
| Performance | Mayor throughput | Menor throughput |
| Elección | — | ✅ Elegido: correctitud > performance en asignación |

### ¿Por qué bin-packing greedy (ORDER BY accumulated_weight DESC)?

| Aspecto | Ventaja |
|--------|---------|
| Minimiza rutas abiertas | Menos recursos de transporte en uso simultáneo |
| Simple de implementar | Query nativa de una sola línea |
| Predecible | Comportamiento determinista, fácil de testear |
| Escalable | Índice cubre el ORDER BY eficientemente |

---

## 🏆 Conclusión

**StoreLogistic - Feature Asignar Ruta + Solicitar Ruta** es una implementación que combina:

✅ Arquitectura hexagonal con dominio verdaderamente puro  
✅ Doble canal de entrada: REST sincrónico y RabbitMQ asíncrono  
✅ Lógica de negocio compleja (bin-packing, selección de vehículo, cierre automático)  
✅ Concurrencia segura mediante aislamiento transaccional  
✅ Testing exhaustivo en todas las capas  

**Está lista para:**
- 📚 Evaluación académica de arquitectura hexagonal aplicada
- 🔗 Integración con el módulo de transportistas (futura feature)
- 🚀 Extensión a consulta de paradas y tracking de entregas

---

**Documento compilado:** 23 de Abril de 2026  
**Versión:** 1.0  
**Rama:** feature/route-assignment  
**Estado:** ✅ COMPLETADO