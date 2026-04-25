# Resumen Ejecutivo - Feature Actualizar Estado de Pedidos en Entrega
## Sistema de Gestión Logística - StoreLogistic

## Documento de Síntesis

**Proyecto:** Sistema de Gestión Logística - Feature Actualizar Estado de Pedidos en Entrega  
**Empresa:** Universidad del Magdalena  
**Fecha:** 24 de Abril de 2026  
**Versión:** 0.0.1-SNAPSHOT  
**Estado:** Implementación Completa

---

## Objetivo del Proyecto

Implementar el registro y actualización del estado final de entrega de un pedido, permitiendo al transportista indicar el resultado de la entrega una vez completado el recorrido. La operación persiste el estado, calcula automáticamente la tasa de efectividad, registra auditoría de cambios de estado, genera alertas operativas cuando aplica, y publica un evento asíncrono al Módulo Financiero.

---

## Características Principales

| Característica | Descripción | Estado |
|---|---|---|
| **Endpoint REST** | PUT /logistics/orders/{idPedido}/status | Implementado |
| **5 estados finales** | ENTREGADO_COMPLETO, RECHAZO_PARCIAL, NO_ENTREGADO, DEVOLUCION_ERROR_EMPRESA, FALTANTE_INVENTARIO | Implementado |
| **Tasa de efectividad** | Calculada automáticamente por el dominio según el estado registrado | Implementado |
| **Auditoría de cambios** | Registro histórico cuando un estado ya existente es corregido | Implementado |
| **Alertas operativas** | Generadas automáticamente ante estados problemáticos | Implementado |
| **Evento asíncrono** | Publicación a Módulo Financiero con `@Async` sin bloquear ni hacer rollback | Implementado |
| **Preservación de datos** | Columnas del módulo de rutas (NOT NULL) se conservan en el update | Implementado |
| **Testing completo** | Unitarios, integración con Testcontainers PostgreSQL + RabbitMQ | Implementado |

---

## Arquitectura Implementada

### Patrón: Hexagonal (Ports & Adapters) — Capa de Dominio Pura

```
CAPA EXTERNA
  HTTP (REST)
      ↓
  UpdateOrderStatusController
      ↓
UpdateOrderStatusService  ←→  OrderRepository (puerto)
                          ←→  CarrierRepository (puerto)
                          ←→  AlertRepository (puerto)
                          ←→  OrderStatusAuditRepository (puerto)
                          ←→  OrderStatusEventPublisher (puerto)
                                      ↓
                         OrderStatusRepositoryAdapter (JPA)
                         CarrierRepositoryAdapter (JPA)
                         AlertRepositoryAdapter (JPA)
                         OrderStatusAuditRepositoryAdapter (JPA)
                         OrderStatusEventPublisherAdapter (@Async)
                                      ↓
                          PostgreSQL + [Módulo Financiero]
```

**Regla estricta:** el dominio no importa ninguna clase de Spring, JPA ni Lombok. Todo el comportamiento de negocio — cálculo de tasa, decisión de alerta, validación de rango — vive en clases Java puras.

### Principios SOLID Aplicados

**S**ingle Responsibility: `UpdateOrderStatusService` orquesta, `Order` calcula, `Alert.createFor()` genera alertas  
**O**pen/Closed: agregar un nuevo estado final no requiere modificar el servicio ni la infraestructura  
**L**iskov Substitution: todos los adaptadores de repositorio son intercambiables por sus puertos  
**I**nterface Segregation: cinco puertos de salida separados con responsabilidades distintas  
**D**ependency Inversion: el servicio depende de puertos (abstracciones), nunca de adaptadores JPA

---

## Estadísticas del Código

### Composición

| Tipo | Cantidad | Ejemplos |
|------|----------|---------|
| **Domain Models** | 4 | Order, Alert, Carrier, OrderStatusAudit |
| **Value Objects** | 3 | FinalStatus (enum), EffectivenessRate, AlertType, AlertStatus |
| **Domain Exceptions** | 4 | OrderNotFoundException, CarrierNotFoundException, InvalidFinalStatusException, LogisticsException |
| **Domain Ports (IN)** | 1 | UpdateOrderStatusUseCase |
| **Domain Ports (OUT)** | 5 | OrderRepository, CarrierRepository, AlertRepository, OrderStatusAuditRepository, OrderStatusEventPublisher |
| **Application Services** | 1 | UpdateOrderStatusService |
| **JPA Entities** | 4 | OrderStatusJpaEntity, CarrierJpaEntity, OrderAlertJpaEntity, OrderStatusAuditJpaEntity |
| **Adapters** | 5 | OrderStatusRepositoryAdapter, CarrierRepositoryAdapter, AlertRepositoryAdapter, OrderStatusAuditRepositoryAdapter, OrderStatusEventPublisherAdapter |
| **REST Endpoints** | 1 | PUT /logistics/orders/{idPedido}/status |
| **DTOs** | 2 | UpdateOrderStatusRequest, UpdateOrderStatusResponse |
| **Flyway Migrations** | 4 | V8–V11 |
| **Tests** | 35+ | Unitarios + Integración Testcontainers |

---

## Reglas de Negocio Implementadas

### Estados Finales y Tasa de Efectividad

| Estado Final | Display Name | Tasa de Efectividad | Genera Alerta |
|---|---|---|---|
| `ENTREGADO_COMPLETO` | `ENTREGADO_COMPLETO` | +100 | No |
| `RECHAZO_PARCIAL` | `RECHAZO_PARCIAL` | +80 | Sí |
| `NO_ENTREGADO` | `NO_ENTREGADO` | 0 | Sí |
| `DEVOLUCION_ERROR_EMPRESA` | `DEVOLUCION_ERROR_EMPRESA` | 0 | Sí |
| `FALTANTE_INVENTARIO` | `FALTANTE_INVENTARIO` | -100 | Sí |

### Auditoría

- Primera vez que se registra un estado: no se genera auditoría
- Corrección de un estado ya existente: se guarda un `OrderStatusAudit` con `estadoAnterior` y `estadoNuevo`

### Evento al Módulo Financiero

- Se publica con `@Async` (no bloquea el hilo principal)
- Si falla la publicación, **no se hace rollback** — el pedido queda persistido
- Payload: `{ id_pedido, tasa_efectividad, id_transportista }` (estado_final excluido por decisión de integración)

---

## Validaciones Implementadas

### A Nivel de Entrada

- `idPedido` debe ser numérico (400 `MethodArgumentTypeMismatchException`)
- `estadoFinal` no puede ser null ni vacío (`@NotBlank` → 400)
- `idTransportista` no puede ser null (`@NotNull` → 400)
- `estadoFinal` debe ser un valor reconocido (`FinalStatus.fromDisplayName()` → 422)

### A Nivel de Negocio

- Transportista debe existir en `vehiculos` (404 `CarrierNotFoundException`)
- Pedido debe existir en `orders` (404 `OrderNotFoundException`)
- Tasa de efectividad validada en rango -100..100 (invariante del dominio)

### A Nivel de Persistencia

- Columnas `logistic_weight` y `delivery_address` (propiedad del módulo de rutas, NOT NULL) se preservan mediante merge-save (fetch + update de campos propios)

---

## Escenarios

| Escenario | Condición | Resultado |
|---|---|---|
| **SC1** | Primera entrega con ENTREGADO_COMPLETO | 200, tasa=100, sin alerta, evento publicado |
| **SC1** | Primera entrega con NO_ENTREGADO | 200, tasa=0, alerta generada, evento publicado |
| **SC1** | Primera entrega con RECHAZO_PARCIAL | 200, tasa=80, alerta generada |
| **SC1** | Primera entrega con DEVOLUCION_ERROR_EMPRESA | 200, tasa=0, alerta generada |
| **SC1** | Primera entrega con FALTANTE_INVENTARIO | 200, tasa=-100, alerta generada |
| **SC2** | Pedido con estado previo → corrección | 200, auditoría guardada, nuevo estado persistido |
| **EC** | Estado no reconocido | 422 ESTADO_INVALIDO |
| **EC** | Pedido no existe | 404 PEDIDO_NO_ENCONTRADO |
| **EC** | Transportista no existe | 404 TRANSPORTISTA_NO_ENCONTRADO |
| **EC** | `idTransportista` null | 400 VALIDACION_FALLIDA |

---

## Decisiones de Diseño Clave

### ¿Por qué la tasa de efectividad no se pasa por parámetro?

La tasa es una consecuencia directa del estado final — no una entrada del transportista. Calcularla en el dominio (`FinalStatus.effectivenessRate()`) garantiza que nunca pueda quedar inconsistente: el estado y la tasa siempre coinciden, sin importar qué adaptador llame al servicio.

### ¿Por qué el evento de fallo no hace rollback?

El evento va al Módulo Financiero como notificación posterior a la entrega. El estado del pedido ya fue registrado en base de datos dentro de la transacción. Si el broker de mensajes no está disponible, la entrega ya ocurrió — hacer rollback del estado sería incorrecto desde el punto de vista del negocio. El módulo financiero puede obtener los datos vía consulta directa si necesita reconciliar.

### ¿Por qué merge-save en el repositorio?

Las columnas `logistic_weight` y `delivery_address` son NOT NULL y pertenecen al módulo de rutas. Este módulo solo toca columnas de estado. Hacer `mapper.toEntity(order)` desde cero perdería los valores de rutas. El merge-save (fetch existing → update status fields → save) preserva esas columnas sin acoplar los módulos a nivel de dominio.

### ¿Por qué dos entidades JPA sobre la misma tabla `orders`?

El módulo de rutas tiene su propia `OrderJpaEntity`. Para evitar conflictos de nombre de entidad JPA y de bean Spring, la entidad de este módulo usa `@Entity(name = "OrderStatusEntity")` con su propio `@Table(name = "orders")`. Las dos entidades coexisten sin conflicto porque mapean columnas distintas con nombres JPA distintos.

---

## Stack Tecnológico

### Lenguaje & Runtime
- **Java 21 LTS** — Records, switch expressions, sealed patterns

### Framework
- **Spring Boot 3.5.13** — REST controllers, validación, DI, `@Async`
- **Spring Data JPA + Hibernate** — persistencia
- **Flyway 11.x** — migraciones V8–V11

### Persistencia
- **PostgreSQL 17** — tabla `orders` + nuevas: `order_alerts`, `order_status_audit`

### Mapping
- **MapStruct 1.6.3** — mapper abstracto con métodos manuales para conversiones complejas

### Mensajería
- **Spring Cloud Stream + RabbitMQ** — publicación de eventos al Módulo Financiero via `StreamBridge` (cola: `estado-pedido.event`)

### Testing
- **JUnit 5** — framework de pruebas
- **Mockito** — mocking de puertos de dominio
- **AssertJ** — aserciones fluidas
- **Testcontainers 1.20** — PostgreSQL + RabbitMQ reales para integración

---

## Requisitos Cumplidos

### Requisitos Funcionales

| RF | Descripción | Estado |
|----|-------------|--------|
| **FR-001** | Registrar estado final de entrega de un pedido | Implementado |
| **FR-002** | Calcular y persistir tasa de efectividad por estado | Implementado |
| **FR-003** | Generar alerta ante estados de entrega problemáticos | Implementado |
| **FR-004** | Registrar auditoría al corregir un estado ya existente | Implementado |
| **FR-005** | Publicar evento asíncrono al Módulo Financiero | Implementado |
| **FR-006** | Preservar datos de otros módulos en la tabla compartida | Implementado |

### Criterios de Calidad

| Criterio | Objetivo | Estado |
|----------|----------|--------|
| Dominio libre de frameworks | 0 imports de Spring/JPA/Lombok en domain | Cumplido |
| DIP garantizado | Servicio depende de puertos, no de adaptadores | Cumplido |
| Tasa calculada en dominio | FinalStatus.effectivenessRate() como única fuente de verdad | Cumplido |
| Rollback correcto | Fallo del publisher no deshace la persistencia | Cumplido |
| Entidades JPA aisladas | `@Entity(name)` distinto por módulo sobre misma tabla | Sin conflictos |
| Manejador aislado | `basePackages` en `@RestControllerAdvice` | Sin conflictos |

---

## Valor Educativo

Esta feature demuestra:

**Arquitectura:** Hexagonal con múltiples puertos de salida para una sola operación de negocio  
**Patrones:** Port & Adapter, Factory Method (`Alert.createFor()`), Merge-Save para multi-módulo sobre tabla compartida  
**Diseño de dominio:** invariantes en el dominio (tasa calculada, alerta decidida), comandos con estado mutable  
**Consistencia eventual:** evento asíncrono sin rollback — correcto para notificaciones post-entrega  
**Persistencia compartida:** dos módulos sobre la misma tabla SQL sin acoplamiento de dominio  
**Testing:** Testcontainers para contratos de BD reales, Mockito para lógica de servicio aislada

---

## Próximos Pasos

### Cuando el broker de mensajes esté conectado

- [ ] Acordar con el equipo del módulo financiero el nombre final de la cola (actualmente `estado-pedido.event`)
- [ ] Actualizar `destination` en `application.yml` con el nombre acordado
- [ ] Agregar DLQ (Dead Letter Queue) en `application.yml` para eventos que fallen (igual que `procesarSolicitudRuta-in-0`)

### Mejoras Futuras

- [ ] Endpoint `GET /logistics/orders/{idPedido}/status` para consultar el estado actual
- [ ] Endpoint `GET /logistics/orders/{idPedido}/audit` para consultar historial de cambios
- [ ] Dashboard de alertas pendientes para supervisores
- [ ] Circuit Breaker en el publisher para fallos prolongados del broker

---

## Puntos Destacables

**`FinalStatus.effectivenessRate()` como única fuente de verdad** — ni el controlador, ni el servicio, ni la BD calculan la tasa: fluye del dominio hacia afuera  
**`Alert.createFor(Order)` como factory** — encapsula el switch completo de tipos de alerta; agregar un estado nuevo solo requiere agregar un case  
**Merge-save con comentario justificado** — el único comentario en todo el código de producción documenta la restricción NOT NULL inter-módulo que no es obvia desde el código  
**Publisher sin rollback documentado en test** — el test `update_publisherFails_orderRemainsPersistedNoException` codifica la decisión de diseño como contrato verificable

---

## Matrices de Decisión

### ¿Calcular tasa en dominio o en BD?

| Aspecto | Calcular en BD (trigger/función) | Calcular en dominio |
|--------|----------------------------------|---------------------|
| Consistencia | Solo si se usa la BD | Siempre, cualquier entrada |
| Testabilidad | Requiere BD real | Unit test puro |
| Transparencia | Oculto en migración SQL | Explícito en FinalStatus enum |
| **Elección** | — | Dominio |

### ¿Rollback si el publisher falla?

| Aspecto | Rollback | Sin rollback |
|--------|----------|--------------|
| Consistencia BD | Estado no persiste si falla broker | Estado persiste siempre |
| Correctitud de negocio | La entrega ocurrió aunque el broker falle | Correcto — la entrega es un hecho |
| Disponibilidad | Depende del broker para registrar | Independiente del broker |
| **Elección** | — | Sin rollback (try-catch en servicio) |

---

## Conclusión

**StoreLogistic - Feature Actualizar Estado de Pedidos en Entrega** es una implementación que combina:

Arquitectura hexagonal con múltiples puertos de salida en una sola operación  
Lógica de negocio encapsulada en el dominio (tasa, alerta, auditoría)  
Persistencia segura sobre tabla compartida con módulo de rutas  
Consistencia eventual correctamente diseñada (evento asíncrono sin rollback)  
Testing exhaustivo con Testcontainers para verificar contratos de BD

**Está lista para:**
- Evaluación académica de arquitectura hexagonal y diseño de dominio
- Integración con el Módulo Financiero real al reemplazar el publisher stub
- Extensión con endpoints de consulta de estado e historial de auditoría

---

**Documento compilado:** 24 de Abril de 2026  
**Versión:** 1.0  
**Rama:** feature/actualizar-estado-pedido  
**Estado:** COMPLETADO