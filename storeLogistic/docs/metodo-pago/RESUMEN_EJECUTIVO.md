# Resumen Ejecutivo - Feature Consultar Método de Pago de un Pedido
## Sistema de Gestión Logística - StoreLogistic

## 📋 Documento de Síntesis

**Proyecto:** Sistema de Gestión Logística - Feature Consultar Método de Pago  
**Empresa:** Universidad del Magdalena  
**Fecha:** 24 de Abril de 2026  
**Versión:** 0.0.1-SNAPSHOT  
**Estado:** ✅ Implementación Completa

---

## 🎯 Objetivo del Proyecto

Implementar la consulta síncrona del método de pago de un pedido al Módulo Financiero, permitiendo al módulo de Logística determinar las condiciones de pago antes de procesar la entrega. La consulta retorna en **una sola llamada HTTP** tanto la forma de pago como el total a cobrar en entrega, eliminando la necesidad de llamadas adicionales y simplificando la lógica del consumidor.

---

## ✨ Características Principales

| Característica | Descripción | Estado |
|---|---|---|
| **Endpoint REST** | GET /pedidos/{id_pedido}/forma-pago | ✅ Implementado |
| **Una sola llamada** | forma_pago y total_pedido en un único request al Módulo Financiero | ✅ Implementado |
| **null con significado** | totalPedido=null para CARTERA_COMERCIAL, valor para CONTRA_ENTREGA | ✅ Implementado |
| **Reintentos inteligentes** | Backoff exponencial solo ante 5xx/timeout, nunca ante 404/422 | ✅ Implementado |
| **Mock intercambiable** | MockFinanceModuleClient reemplazable sin tocar dominio ni aplicación | ✅ Implementado |
| **Manejo de errores** | 404, 422, 503, 400 con códigos de negocio explícitos | ✅ Implementado |
| **Testing completo** | Unitarios, integración WireMock y contrato de API | ✅ Implementado |

---

## 🏗️ Arquitectura Implementada

### Patrón: Hexagonal (Ports & Adapters) — Capa de Dominio Pura

```
CAPA EXTERNA
  HTTP (REST)
      ↓
  PaymentMethodController
      ↓
ConsultPaymentMethodService  ←→  FinanceGatewayPort (puerto)
                                        ↓
                             MockFinanceModuleClient (activo)
                             FinanceModuleClient     (disponible al activar)
                                        ↓
                              Módulo Financiero HTTP
```

**Regla estricta:** el dominio no importa ninguna clase de Spring, JPA ni Lombok. Todo el comportamiento de negocio vive en clases Java puras.

### Principios SOLID Aplicados

✅ **S**ingle Responsibility: `ConsultPaymentMethodService` solo consulta, `FinanceModuleClient` solo hace HTTP  
✅ **O**pen/Closed: activar el cliente real no modifica ninguna clase existente  
✅ **L**iskov Substitution: `MockFinanceModuleClient` y `FinanceModuleClient` intercambiables  
✅ **I**nterface Segregation: puerto de entrada y puerto de salida separados  
✅ **D**ependency Inversion: el servicio depende de `FinanceGatewayPort`, no de `FinanceModuleClient`

---

## 📊 Estadísticas del Código

### Composición

| Tipo | Cantidad | Ejemplos |
|------|----------|---------|
| **Domain Models** | 1 | OrderPaymentMethod |
| **Value Objects** | 1 | PaymentMethod (enum) |
| **Domain Exceptions** | 4 | OrderNotFoundException, PaymentMethodNotRegisteredException, FinanceServiceUnavailableException, LogisticsException |
| **Domain Ports (IN)** | 1 | ConsultPaymentMethodUseCase |
| **Domain Ports (OUT)** | 1 | FinanceGatewayPort |
| **Application Services** | 1 | ConsultPaymentMethodService |
| **HTTP Adapters** | 2 | FinanceModuleClient (real), MockFinanceModuleClient (stub) |
| **REST Endpoints** | 1 | GET /pedidos/{id_pedido}/forma-pago |
| **DTOs** | 2 | PaymentMethodResponse, FinancePaymentMethodResponse |
| **Tests** | 34+ | Unitarios, WireMock, contrato |

---

## 🔐 Validaciones Implementadas

### A Nivel de Entrada

- ✅ `id_pedido` debe ser numérico (`MethodArgumentTypeMismatchException` → 400)
- ✅ `orderId` no puede ser null en el dominio (`IllegalArgumentException`)
- ✅ `paymentMethod` no puede ser null en el dominio (`IllegalArgumentException`)

### A Nivel de Negocio

- ✅ Pedido debe existir en el Módulo Financiero (404 → `OrderNotFoundException`)
- ✅ Cliente debe tener forma de pago registrada (422 → `PaymentMethodNotRegisteredException`)
- ✅ `totalPedido` nunca se convierte a 0 — null preserva el significado de CARTERA_COMERCIAL

### A Nivel de Resiliencia

- ✅ Máximo 3 reintentos con backoff exponencial (500ms → 1000ms → 2000ms)
- ✅ Errores 404/422 no disparan reintentos (son errores de negocio, no de infraestructura)
- ✅ Agotados los reintentos → `FinanceServiceUnavailableException` → HTTP 503

---

## 🔄 Escenarios de Consulta

| Escenario | Condición | Resultado |
|---|---|---|
| **SC1** | Pedido con CONTRA_ENTREGA | 200 con paymentMethod y totalPedido (monto) |
| **SC2** | Pedido con CARTERA_COMERCIAL | 200 con paymentMethod y totalPedido: null |
| **EC1** | Pedido no existe | 404 PEDIDO_NO_ENCONTRADO |
| **EC2** | Cliente sin forma de pago | 422 FORMA_PAGO_NO_REGISTRADA |
| **EC3** | Módulo Financiero caído (3 intentos) | 503 SERVICIO_FINANCIERO_NO_DISPONIBLE |
| **EC4** | Módulo Financiero recupera en 2do intento | 200 — reintento exitoso transparente |
| **EC5** | id_pedido no numérico | 400 VALIDACION_FALLIDA |

---

## 📈 Decisiones de Diseño Clave

### ¿Por qué una sola llamada al Módulo Financiero?

El Módulo Financiero adaptó su endpoint `GET /pedidos/{id}/forma-pago` para retornar `total_pedido` junto con `forma_pago`. Esto elimina la necesidad de una segunda llamada a `GET /pedidos/{id}/total`, reduciendo:
- Latencia (1 RTT en lugar de 2)
- Puntos de fallo (1 en lugar de 2)
- Complejidad en la lógica de coordinación entre llamadas

### ¿Por qué totalPedido es null y no 0 para CARTERA_COMERCIAL?

| Valor | Significado |
|-------|-------------|
| `null` | No aplica — el pago se gestiona por cartera comercial, no hay monto a cobrar en entrega |
| `0` | El pedido tiene valor cero (semánticamente incorrecto) |

El frontend debe renderizar `null` como "No cobrar" o "Cartera Comercial", nunca como "$0".

### ¿Por qué Mock y no BD local?

La feature es **stateless**: no persiste ningún dato propio. Toda la información vive en el Módulo Financiero. El mock permite desarrollar y probar sin dependencia del servicio externo, y el cambio al cliente real no requiere modificar ninguna clase del dominio o la aplicación.

---

## 🔧 Stack Tecnológico

### Lenguaje & Runtime
- **Java 21 LTS** — Records, switch expressions

### Framework
- **Spring Boot 3.5.13** — REST controllers, validación, DI
- **Spring WebFlux (WebClient)** — cliente HTTP no bloqueante (usado en modo bloqueante)
- **Spring Retry** — política de reintentos con backoff exponencial
- **Spring AOP** — requerido por Spring Retry

### Mapping
- **MapStruct 1.6.3** — interface mapper con métodos nombrados para conversiones personalizadas

### Testing
- **JUnit 5** — framework de pruebas
- **Mockito** — mocking de puertos de dominio
- **AssertJ** — aserciones fluidas
- **WireMock 3.13.0** — servidor HTTP stub para tests de integración del cliente

---

## ✅ Requisitos Cumplidos

### Requisitos Funcionales

| RF | Descripción | Estado |
|----|-------------|--------|
| **FR-001** | Enviar GET al Módulo Financiero con solo el idPedido | ✅ Implementado |
| **FR-002** | Procesar respuesta con forma_pago y total_pedido (nullable) | ✅ Implementado |
| **FR-003** | Manejar errores: pedido no encontrado, sin forma de pago | ✅ Implementado |
| **FR-004** | Reintentar ante timeout/indisponibilidad (máx 3, backoff exponencial) | ✅ Implementado |

### Criterios de Calidad

| Criterio | Objetivo | Estado |
|----------|----------|--------|
| Dominio libre de frameworks | 0 imports de Spring/JPA/Lombok en domain | ✅ Cumplido |
| DIP garantizado | Servicio depende de puerto, no del adaptador | ✅ Cumplido |
| Reintentos diferenciados | 404/422 no reintentables, 5xx/timeout sí | ✅ Cumplido |
| null preservado | totalPedido=null nunca convertido a 0 | ✅ Cumplido |
| Manejador aislado | basePackages en @RestControllerAdvice | ✅ Sin conflictos |

---

## 🎓 Valor Educativo

Esta feature demuestra:

✅ **Arquitectura:** Hexagonal con dominio verdaderamente puro  
✅ **Patrones:** Port & Adapter, Strategy (reintentos), Null Object implícito  
✅ **Resiliencia:** Spring Retry con clasificación de errores reintentables vs no reintentables  
✅ **Diseño de API:** null con significado de negocio explícito vs conversión errónea a 0  
✅ **Testing:** WireMock para contratos HTTP, escenarios de retry con estados  
✅ **Decisión de diseño documentada:** por qué una sola llamada vs dos llamadas separadas

---

## 🔮 Próximos Pasos

### Cuando el Módulo Financiero esté disponible

- [ ] Agregar `@Component` a `FinanceModuleClient`
- [ ] Eliminar `@Component` de `MockFinanceModuleClient`
- [ ] Configurar `FINANCE_MODULE_BASE_URL` en `application.yml`
- [ ] Ejecutar `FinanceModuleClientIntegrationTest` contra WireMock para verificar contrato

### Mejoras Futuras

- [ ] Circuit Breaker (Resilience4j) como complemento al retry para fallos prolongados
- [ ] Caché de forma de pago (Redis) para reducir llamadas repetidas al mismo pedido
- [ ] Health indicator personalizado para verificar disponibilidad del Módulo Financiero

---

## 💡 Puntos Destacables

✅ **`totalPedido` nullable con semántica clara** — el `null` no es un fallo de diseño sino el contrato explícito del negocio  
✅ **`PaymentMethod.fromString()` como única fuente de verdad** — ni el mapper ni el cliente duplican la lógica de parseo  
✅ **Recovery function en RetryTemplate** — los errores no reintentables (404/422) se propagan sin envolver, evitando pérdida de tipo de excepción  
✅ **`basePackages` en `@RestControllerAdvice`** — cada módulo maneja sus propias excepciones sin interferencia  

---

## 📊 Matrices de Decisión

### ¿Una sola llamada o dos?

| Aspecto | Dos llamadas | Una sola llamada |
|--------|-------------|-----------------|
| Latencia | 2 RTT | 1 RTT |
| Puntos de fallo | 2 | 1 |
| Lógica condicional | Sí (if CONTRA_ENTREGA → 2da llamada) | No |
| Coordinación de errores | Compleja | Simple |
| Requiere adaptación del Módulo Financiero | No | Sí |
| **Elección** | — | ✅ Elegida: el Módulo Financiero se adaptó |

### ¿null o 0 para CARTERA_COMERCIAL?

| Aspecto | 0 | null |
|--------|---|------|
| Semántica | "El pedido vale $0" (incorrecto) | "No aplica cobro en entrega" (correcto) |
| Comportamiento en frontend | Muestra "$0" (confuso) | Muestra "No cobrar" (claro) |
| **Elección** | — | ✅ null con significado explícito |

---

## 🏆 Conclusión

**StoreLogistic - Feature Consultar Método de Pago** es una implementación que combina:

✅ Arquitectura hexagonal con dominio verdaderamente puro  
✅ Integración síncrona con servicio externo con resiliencia correctamente diseñada  
✅ Decisión de diseño documentada y defendible (una llamada vs dos, null vs cero)  
✅ Testing exhaustivo incluyendo escenarios de retry con WireMock  

**Está lista para:**
- 📚 Evaluación académica de arquitectura hexagonal e integración de microservicios
- 🔗 Integración con el Módulo Financiero real al activar `FinanceModuleClient`
- 🚀 Extensión con circuit breaker o caché sin modificar el dominio

---

**Documento compilado:** 24 de Abril de 2026  
**Versión:** 1.0  
**Rama:** feature/consultar-metodo-pago  
**Estado:** ✅ COMPLETADO