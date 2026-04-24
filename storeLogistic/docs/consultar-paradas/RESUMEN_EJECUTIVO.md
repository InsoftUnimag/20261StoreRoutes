# Resumen Ejecutivo - Feature Consultar Paradas de Rutas
## Sistema de Gestión Logística - StoreLogistic

## 📋 Documento de Síntesis

**Proyecto:** Sistema de Gestión Logística - Feature Consultar Paradas  
**Empresa:** Universidad del Magdalena  
**Fecha:** 24 de Abril de 2026  
**Versión:** 0.0.1-SNAPSHOT  
**Estado:** ✅ Implementación Completa

---

## 🎯 Objetivo del Proyecto

Implementar los endpoints REST que permiten al transportista (conductor) consultar las paradas asignadas a su ruta desde su dispositivo. La autorización se resuelve verificando que la ruta esté asignada a un vehículo cuyo `id_transportista` coincida con el `carrierId` del request, sin necesidad de una tabla propia de transportistas (módulo externo). Se ofrecen dos endpoints: una **lista rápida** sin llamadas externas y un **detalle por parada** con método de pago incluido.

---

## ✨ Características Principales

| Característica | Descripción | Estado |
|---|---|---|
| **Lista de paradas** | GET /logistics/routes/{routeId}/stops — sin llamadas al Módulo Financiero | ✅ Implementado |
| **Detalle de parada** | GET /logistics/routes/{routeId}/stops/{stopId} — incluye método de pago | ✅ Implementado |
| **Autorización por JOIN** | Verificación vía `routes JOIN vehiculos WHERE id_transportista = carrierId` | ✅ Implementado |
| **403 ambiguo intencional** | Ruta inexistente y ruta de otro transportista devuelven el mismo 403 | ✅ Implementado |
| **null con significado** | `totalACobrar = null` para CARTERA_COMERCIAL, monto para CONTRA_ENTREGA | ✅ Implementado |
| **Paradas ordenadas** | Lista siempre ordenada por `sequence` ASC | ✅ Implementado |
| **Testing completo** | Unitarios, integración con Testcontainers y contrato de API | ✅ Implementado |

---

## 🏗️ Arquitectura Implementada

### Patrón: Hexagonal (Ports & Adapters) — Capa de Dominio Pura

```
CAPA EXTERNA
  HTTP (REST)
      ↓
  QueryStopsController
      ↓                           ↓
QueryStopsUseCase          GetStopDetailUseCase    ConsultPaymentMethodUseCase
      ↓                           ↓                          ↓
QueryStopsService      GetStopDetailService         (módulo paymentmethod)
      ↓                      ↓
AuthorizationService ←────────┘
      ↓
RouteRepository (puerto)
      ↓
RouteSpringRepository (native SQL JOIN a vehiculos)
```

**Regla estricta:** el dominio no importa ninguna clase de Spring, JPA ni Lombok. El controller ensambla `Stop + OrderPaymentMethod` y delega al mapper — los services permanecen puros.

### Principios SOLID Aplicados

✅ **S**ingle Responsibility: `QueryStopsService` lista, `GetStopDetailService` detalla, `AuthorizationService` autoriza  
✅ **O**pen/Closed: agregar nuevos endpoints no modifica los servicios existentes  
✅ **L**iskov Substitution: `MockFinanceModuleClient` y `FinanceModuleClient` intercambiables  
✅ **I**nterface Segregation: `QueryStopsUseCase` y `GetStopDetailUseCase` separados  
✅ **D**ependency Inversion: servicios dependen de puertos (`RouteRepository`, `StopRepository`), no de adapters JPA

---

## 📊 Estadísticas del Código

### Composición

| Tipo | Cantidad | Ejemplos |
|------|----------|---------|
| **Domain Exceptions** | 1 | AccessDeniedException |
| **Domain Ports (IN)** | 2 | QueryStopsUseCase, GetStopDetailUseCase |
| **Application Services** | 3 | QueryStopsService, GetStopDetailService, AuthorizationService |
| **REST Endpoints** | 2 | GET /stops, GET /stops/{stopId} |
| **DTOs** | 3 | StopSummaryDTO, StopDetailDTO, QueryStopsResponse |
| **Mapper** | 1 | QueryStopsMapper (abstract class — fluent accessors) |
| **Flyway Migration** | 1 | V7 — customer_contact + índice de secuencia |
| **Tests unitarios** | 20+ | AccessDeniedException, AuthorizationService, QueryStopsService, GetStopDetailService, mapper, controller |
| **Tests de integración** | 9 | QueryStopsApiContractTest — lista y detalle |

---

## 🔐 Validaciones Implementadas

### A Nivel de Entrada

- ✅ `routeId`, `stopId`, `carrierId` deben ser positivos (`@Positive` → 400 `VALIDACION_FALLIDA`)
- ✅ `carrierId` es requerido como query param (faltante → 400)
- ✅ IDs no numéricos en path → 400

### A Nivel de Autorización

- ✅ La ruta debe pertenecer a un vehículo con `id_transportista = carrierId` (JOIN nativo)
- ✅ El stop debe pertenecer a la ruta indicada
- ✅ Ruta inexistente y ruta de otro transportista: misma respuesta 403 (sin revelar existencia)

### A Nivel de Negocio

- ✅ `totalACobrar: null` para CARTERA_COMERCIAL — nunca se convierte a 0
- ✅ Logs de auditoría: INFO en consulta exitosa, WARN en acceso denegado

---

## 🔄 Escenarios de Consulta

| Escenario | Condición | Resultado |
|---|---|---|
| **SC1** | Lista de paradas — ruta asignada al transportista | 200 con lista ordenada por sequence |
| **SC2** | Detalle de parada — CONTRA_ENTREGA | 200 con customerContact, paymentMethod, totalACobrar |
| **SC3** | Detalle de parada — CARTERA_COMERCIAL | 200 con totalACobrar: null |
| **EC1** | Ruta no asignada al transportista | 403 ACCESO_DENEGADO |
| **EC2** | Ruta inexistente | 403 ACCESO_DENEGADO (misma respuesta, ambigüedad intencional) |
| **EC3** | Stop no pertenece a la ruta | 403 ACCESO_DENEGADO |
| **EC4** | carrierId faltante | 400 VALIDACION_FALLIDA |
| **EC5** | routeId o stopId = 0 o negativo | 400 VALIDACION_FALLIDA |

---

## 📈 Decisiones de Diseño Clave

### ¿Por qué dos endpoints separados?

| Aspecto | Endpoint único con pago | Lista + Detalle separados |
|--------|------------------------|--------------------------|
| Llamadas al Módulo Financiero | N (una por parada) | 0 en lista, 1 en detalle |
| Latencia de la lista | Alta (N RTTs) | Baja (0 RTTs externos) |
| Uso en pantalla principal | Innecesario mostrar pago en lista | Solo se consulta cuando el conductor toca una parada |
| **Elección** | — | ✅ Elegida: separación de responsabilidades y rendimiento |

### ¿Por qué `GetStopDetailService` retorna solo `Stop`?

El ensamblado de `Stop + OrderPaymentMethod` ocurre en el controller, no en el service. Esto mantiene el service puro (sin dependencia de `ConsultPaymentMethodUseCase`) y permite testear el service de forma aislada sin mockear el módulo financiero.

### ¿Por qué native SQL para la autorización?

`RouteJpaEntity.vehicleId` es un `Long` plano sin `@ManyToOne` — el JOIN cross-entidad JPQL no aplica entre módulos. La query nativa con `nativeQuery = true` resuelve el JOIN a `vehiculos.id_transportista` sin violar el aislamiento entre módulos.

### ¿Por qué el mismo 403 para ruta inexistente y no autorizada?

Seguridad por opacidad (FR-003): revelar si la ruta existe permitiría a un transportista malicioso enumerar rutas del sistema. El 403 uniforme no distingue ambos casos.

---

## 🔧 Stack Tecnológico

| Componente | Tecnología | Versión |
|-----------|-----------|---------|
| **JDK** | Java | 21 LTS |
| **Framework** | Spring Boot | 3.5.13 |
| **Persistencia** | Spring Data JPA + native SQL | 3.5.13 |
| **Migración** | Flyway | 10.x |
| **Mapping** | MapStruct (abstract class) | 1.6.3 |
| **Utilidades** | Lombok (solo app/infra) | 1.18.x |
| **Testing** | JUnit 5, Mockito, AssertJ, Testcontainers | 5.x / 1.19.x |
| **Base de datos test** | PostgreSQL 17.6-alpine3.22 (Testcontainers) | 17.6 |

---

## ✅ Requisitos Cumplidos

### Requisitos Funcionales

| RF | Descripción | Estado |
|----|-------------|--------|
| **FR-001** | Listar paradas de una ruta verificando que el vehículo pertenece al transportista | ✅ Implementado |
| **FR-002** | Ver detalle de parada con customerContact, paymentMethod y totalACobrar | ✅ Implementado |
| **FR-003** | 403 idéntico para ruta inexistente y ruta de otro transportista | ✅ Implementado |
| **FR-004** | Logs de auditoría INFO/WARN en consulta/denegación | ✅ Implementado |

### Criterios de Calidad

| Criterio | Objetivo | Estado |
|----------|----------|--------|
| Dominio libre de frameworks | 0 imports de Spring/JPA/Lombok en domain | ✅ Cumplido |
| Sin tabla carrier | Transportista identificado solo por id_transportista en vehiculos | ✅ Cumplido |
| null preservado | totalACobrar=null para CARTERA_COMERCIAL | ✅ Cumplido |
| Manejador aislado | basePackages en @RestControllerAdvice | ✅ Sin conflictos |
| Services puros | GetStopDetailService no depende de ConsultPaymentMethodUseCase | ✅ Cumplido |

---

## 🎓 Valor Educativo

Esta feature demuestra:

✅ **Arquitectura:** Hexagonal con dominio verdaderamente puro  
✅ **Autorización cross-módulo:** JOIN nativo entre tablas de módulos distintos sin violar aislamiento  
✅ **Separación de responsabilidades:** lista sin pago vs detalle con pago  
✅ **Seguridad por opacidad:** 403 ambiguo intencional para no revelar existencia de recursos  
✅ **MapStruct con fluent accessors:** abstract class como solución cuando Stop no tiene JavaBean getters  
✅ **Testing con Testcontainers:** setup real de BD con vehiculos + routes + stops  

---

## 🔮 Próximos Pasos

### Siguiente Feature

- [ ] Actualizar estado de paradas/pedidos (siguiente feature planificada)

### Cuando el Módulo Financiero esté disponible

- [ ] Activar `FinanceModuleClient` reemplazando `MockFinanceModuleClient`
- [ ] Configurar `FINANCE_MODULE_BASE_URL` en `application.yml`

---

**Documento compilado:** 24 de Abril de 2026  
**Versión:** 1.0  
**Rama:** feature/consultar-paradas  
**Estado:** ✅ COMPLETADO
