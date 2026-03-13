# Feature Specification: Actualizar Estado de Pedidos en Entrega

## User Story 

### Actualizar Estado de Pedidos con Información de Liquidación (Priority: P1)

Como transportista, necesito registrar el estado final de cada pedido durante su entrega (o intento de entrega) junto con la tasa de efectividad, para generar información confiable de liquidación que el sistema financiero pueda procesar automáticamente.

**Why this priority**: Sin la capacidad de registrar el estado_final y tasa_efectividad de los pedidos de forma confiable, el sistema no puede generar liquidaciones correctas para transportistas. Esta es la funcionalidad base imprescindible para la conciliación financiera.

**Independent Test**: Puede ser testeado completamente por un transportista que: (1) Inicia sesión en el sistema, (2) Accede a un pedido pendiente de entrega, (3) Registra el estado_final correspondiente, (4) Captura la tasa_efectividad apropiada, (5) Confirma que los datos quedan disponibles para el módulo de liquidación.

**Acceptance Scenarios**:

1. **Scenario**: Actualizar pedido a estado "Entregado Completo"
   - **Given** un transportista que ha completado exitosamente la entrega de un pedido
   - **When** registra en el sistema el resultado de la entrega con confirmación del cliente 
   - **Then** el sistema actualiza: estado_final="Entregado Completo", id_pedido, id_transportista, y establece tasa_efectividad=100 para liquidación

2. **Scenario**: Actualizar pedido a estado "Rechazo Parcial"
   - **Given** un transportista que entrega algunos productos pero el cliente rechaza otros
   - **When** registra el estado final del pedido
   - **Then** el sistema actualiza: estado_final="Rechazo Parcial", id_pedido, id_transportista, y establece tasa_efectividad=80

3. **Scenario**: Actualizar pedido a estado "No Entregado"
   - **Given** un transportista que no puede completar una entrega
   - **When** registra el estado final del pedido
   - **Then** el sistema actualiza: estado_final="No Entregado", id_pedido, id_transportista, y establece tasa_efectividad=0

4. **Scenario**: Actualizar pedido a estado "Devolución (Error Empresa)"
   - **Given** un transportista que identifica un error de inventario del remitente
   - **When** registra el estado final del pedido
   - **Then** el sistema actualiza: estado_final="Devolución (Error Empresa)", id_pedido, id_transportista, y establece tasa_efectividad=0

5. **Scenario**: Actualizar pedido a estado "Faltante de Inventario"
   - **Given** un transportista que encuentra faltante de inventario en el pedido
   - **When** registra el estado final del pedido
   - **Then** el sistema actualiza: estado_final="Faltante de Inventario", id_pedido, id_transportista, y establece tasa_efectividad=-100



### Edge Cases


- ¿Qué sucede si el transportista intenta actualizar un pedido que ya tiene estado_final registrado? 
El sistema debe alertar sobre duplicación pero permitir correcciones con auditoría completa.
- ¿Qué sucede si la tasa_efectividad no encaja en los rangos esperados (-100 a 100)? 
El sistema debe validar que esté entre -100 a 100 y rechazar valores inválidos con mensaje de error.


---

## Requirements 

### Functional Requirements

- **FR-001**: System MUST permitir al transportista actualizar un pedido especificando: id_pedido, id_transportista, estado_final válido (Entregado Completo/Rechazo Parcial/Devolución (Error Empresa)/Faltante de Inventario/No Entregado), y tasa_efectividad

- **FR-002**: System MUST registrar los datos del pedido: id_pedido, id_transportista, estado_final, y tasa_efectividad

- **FR-003**: System MUST validar que tasa_efectividad esté en rango válido (-100 a 100) y rechazar actualizaciones con valores fuera de rango, mostrando mensaje de error específico

- **FR-004**: System MUST permitir corrección de estado_final registrado previamente (cambiar a estado diferente) con comprobante de auditoría que documenta valor anterior, nuevo, y usuario realizador

### Key Entities 

- **Pedido**: Entidad de documento de entrega que requiere actualización de estado, incluye: id_pedido (PK), id_cliente, id_transportista, estado_final, tasa_efectividad, timestamp de creación, última actualización.

- **Transportista**: Usuario responsable de actualizar estados de pedidos, incluye: IdTransportista

- **Alerta**: Notificación generada por actualización de estado, incluye: IdAlerta, id_pedido, id_transportista, estado_final_registrado, Tipo (no entregado/rechazo/faltante/devolución), Descripción, Estado (pendiente/resuelta), Asignado a supervisor

## Success Criteria 

### Measurable Outcomes

- **SC-001**: El transportista puede actualizar el estado_final de un pedido con tasa_efectividad en menos de 1 minuto desde que abre el formulario de actualización

- **SC-002**: El 100% de las actualizaciones de estado_final (incluyendo todos los valores válidos) se registran con id_pedido, estado_final, tasa_efectividad (-100 a 100), id_transportista

- **SC-003**: El sistema rechaza el 100% de intentos de actualizar con tasa_efectividad fuera del rango -100 a 100 con mensaje de error claro y no permite guardar datos inválidos

- **SC-004**: El sistema permite operación offline y sincroniza automáticamente 100% de actualizaciones (id_pedido, estado_final, tasa_efectividad, id_transportista) cuando recupera conectividad sin pérdida de datos ni corrupción de transaccionalidad

