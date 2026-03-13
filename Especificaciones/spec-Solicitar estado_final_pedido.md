# Feature Specification: Enviar Estado Final de Pedido al Módulo de Conciliación

## User Story 

### Envío Asíncrono de Estado Final de Pedido para Liquidación (Priority: P1)

Como módulo de Logística de Despacho y Distribución, actualizo el estado final de un pedido con información de efectividad del transportista y lo envío de forma asíncrona al módulo de Conciliación Financiera, con el fin de proporcionar datos confiables para la liquidación.

**Why this priority**: Sin la capacidad de enviar de forma confiable el estado final y la tasa de efectividad de los pedidos al módulo financiero, el sistema no puede generar liquidaciones correctas. Esta es la funcionalidad base imprescindible para la conciliación financiera.

**Independent Test**: Puede ser testeado completamente cuando: (1) El transportista reporta un estado de entrega, (2) El módulo de logística actualiza el estado final del pedido, (3) El módulo de logística publica evento "PedidoEntregado" con estado_final y tasa_efectividad, (4) El módulo financiero recibe y procesa el evento.

**Communication Pattern**: Asíncrono vía queue/evento. El módulo de logística publica un evento "PedidoEntregado" que el módulo financiero se suscribe y consume para ejecutar la liquidación.

**Acceptance Scenarios**:

1. **Scenario**: Envía pedido con estado "Entregado Completo" al módulo financiero
   - **Given** el transportista reporta entrega completada exitosamente
   - **When** el módulo de logística actualiza el estado final del pedido
   - **Then** publica evento "PedidoEntregado" con: id_pedido, id_transportista, estado_final="Entregado Completo", tasa_efectividad=100

2. **Scenario**: Envía pedido con estado "Rechazo Parcial" al módulo financiero
   - **Given** el transportista reporta rechazo parcial de productos
   - **When** el módulo de logística actualiza el estado final del pedido
   - **Then** publica evento "PedidoEntregado" con: id_pedido, id_transportista, estado_final="Rechazo Parcial", tasa_efectividad=80

3. **Scenario**: Envía pedido con estado "No Entregado" al módulo financiero
   - **Given** el transportista reporta que la entrega no fue completada
   - **When** el módulo de logística actualiza el estado final del pedido
   - **Then** publica evento "PedidoEntregado" con: id_pedido, id_transportista, estado_final="No Entregado", tasa_efectividad=0

4. **Scenario**: Envía pedido con estado "Devolución (Error Empresa)" al módulo financiero
   - **Given** el transportista reporta error de inventario del remitente
   - **When** el módulo de logística actualiza el estado final del pedido
   - **Then** publica evento "PedidoEntregado" con: id_pedido, id_transportista, estado_final="Devolución (Error Empresa)", tasa_efectividad=0

5. **Scenario**: Envía pedido con estado "Faltante de Inventario" al módulo financiero
   - **Given** el transportista reporta entrega incompleta por faltante
   - **When** el módulo de logística actualiza el estado final del pedido
   - **Then** publica evento "PedidoEntregado" con: id_pedido, id_transportista, estado_final="Faltante de Inventario", tasa_efectividad=-100

---

### Edge Cases

¿Qué sucede si el transportista reporta un estado_final inválido?
El módulo de logística debe validar y rechazar el estado, no publicando el evento al módulo financiero.

¿Qué sucede si la tasa_efectividad está fuera del rango válido?
El módulo de logística debe validar que esté en rango y rechazar la actualización sin publicar el evento.


## Requirements 

### Functional Requirements

- **FR-001**: System MUST [actualizar el estado final de un pedido con los datos: id_pedido, id_transportista, estado_final y tasa_efectividad.]
- **FR-002**: System MUST [aceptar los estados finales válidos: Entregado Completo, Rechazo Parcial, Devolución (Error Empresa), Faltante de Inventario, No Entregado.]
- **FR-003**: System MUST [validar que tasa_efectividad esté en rango válido (-100 a 100) y rechazar actualizaciones fuera de rango.]
- **FR-004**: System MUST [permitir la actualización del estado final de un pedido.]
- **FR-005**: System MUST [publicar evento "PedidoEntregado" de forma asíncrona al módulo financiero con: id_pedido, id_transportista, estado_final, tasa_efectividad.]
- **FR-006**: System MUST [validar que id_pedido e id_transportista existan antes de procesar la actualización.]

### Key Entities 

- **Pedido**: Documento de entrega que requiere actualización de estado, incluye: id_pedido (PK), id_transportista, estado_final, tasa_efectividad.

- **Actualización de Estado**: Registro de cambio de estado final, incluye: id_pedido, id_transportista, estado_final (Entregado Completo / Rechazo Parcial / Devolución (Error Empresa) / Faltante de Inventario / No Entregado), tasa_efectividad (0-100 o -100).

---

## Event Contracts (Comunicación Asíncrona)

El módulo de logística publica eventos que el módulo financiero consume para ejecutar liquidaciones.

**Evento Salida - PedidoEntregado (del Módulo de Logística al Módulo Financiero)**:

El módulo de logística publica este evento cuando actualiza el estado final de un pedido:

```json
{
  "id_pedido": 123,
  "id_transportista": 45,
  "estado_final": "Entregado Completo",
  "tasa_efectividad": 100
}


## Success Criteria 

### Measurable Outcomes

- **SC-001**: El 100% de las actualizaciones de estado final se registran correctamente localmente con id_pedido, id_transportista, estado_final y tasa_efectividad.

- **SC-002**: El sistema rechaza el 100% de intentos de actualización con estado_final inválido o tasa_efectividad fuera de rango (-100 a 100).

- **SC-003**: El sistema publica el 100% de eventos "PedidoEntregado" de forma asíncrona al módulo financiero con todos los datos requeridos.
