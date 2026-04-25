# Especificación de Funcionalidad: Actualizar Estado de Pedidos en Entrega

**Fecha**: 24 de abril de 2026
**Estado**: Revisado — alineado con `recibir_estado_final_modulo_transporte.md`

---

## Historia de Usuario

### Actualizar Estado de Pedido y Notificar al Módulo Financiero (Prioridad: P1)

Como transportista, necesito registrar el estado final de cada pedido durante su entrega (o intento de entrega) junto con la tasa de efectividad, para que el módulo financiero pueda recibir esta información de forma asíncrona y generar automáticamente la liquidación del cliente y del transportista.

**Por qué esta prioridad**: Sin la capacidad de registrar el `estado_final` y la `tasa_efectividad` de los pedidos de forma confiable, el módulo financiero no puede generar liquidaciones correctas. Esta es la funcionalidad base imprescindible para la conciliación financiera del sistema completo.

**Prueba Independiente**: Puede ser testeada completamente por un transportista que: (1) Inicia sesión en el sistema, (2) Accede a un pedido pendiente de entrega, (3) Registra el `estado_final` correspondiente, (4) Confirma que el sistema persiste los datos y los publica de forma asíncrona al módulo financiero con la estructura correcta: `{ id_pedido, tasa_efectividad, id_transportista }`.

---

## Escenarios de Aceptación

**Nota sobre `tasa_efectividad`**: Los valores de efectividad están predefinidos por estado final y son asignados automáticamente por el sistema. El transportista no introduce este valor manualmente.

1. **Escenario**: Actualizar pedido a "Entregado Completo"
   - **Dado** que un transportista completó exitosamente la entrega de un pedido
   - **Cuando** registra en el sistema el resultado de la entrega
   - **Entonces** el sistema persiste: `estado_final="Entregado Completo"`, `tasa_efectividad=100`, `id_pedido`, `id_transportista`; y publica al módulo financiero el evento `{ id_pedido, tasa_efectividad: 100, id_transportista }`

2. **Escenario**: Actualizar pedido a "Rechazo Parcial"
   - **Dado** que un transportista entrega algunos productos pero el cliente rechaza otros
   - **Cuando** registra el estado final del pedido
   - **Entonces** el sistema persiste: `estado_final="Rechazo Parcial"`, `tasa_efectividad=80`, `id_pedido`, `id_transportista`; y publica al módulo financiero el evento `{ id_pedido, tasa_efectividad: 80, id_transportista }`

3. **Escenario**: Actualizar pedido a "No Entregado"
   - **Dado** que un transportista no puede completar una entrega
   - **Cuando** registra el estado final del pedido
   - **Entonces** el sistema persiste: `estado_final="No Entregado"`, `tasa_efectividad=0`, `id_pedido`, `id_transportista`; y publica al módulo financiero el evento `{ id_pedido, tasa_efectividad: 0, id_transportista }`

4. **Escenario**: Actualizar pedido a "Devolución (Error Empresa)"
   - **Dado** que un transportista identifica un error de inventario del remitente
   - **Cuando** registra el estado final del pedido
   - **Entonces** el sistema persiste: `estado_final="Devolución (Error Empresa)"`, `tasa_efectividad=0`, `id_pedido`, `id_transportista`; y publica al módulo financiero el evento `{ id_pedido, tasa_efectividad: 0, id_transportista }`

5. **Escenario**: Actualizar pedido a "Faltante de Inventario"
   - **Dado** que un transportista encuentra faltante de inventario en el pedido
   - **Cuando** registra el estado final del pedido
   - **Entonces** el sistema persiste: `estado_final="Faltante de Inventario"`, `tasa_efectividad=-100`, `id_pedido`, `id_transportista`; y publica al módulo financiero el evento `{ id_pedido, tasa_efectividad: -100, id_transportista }`

---

## Casos Borde

- **¿Qué sucede si el transportista intenta actualizar un pedido que ya tiene `estado_final` registrado?**
  El sistema alerta sobre duplicación pero permite la corrección. Persiste el nuevo estado con auditoría completa que documenta: valor anterior, valor nuevo, `id_transportista` que realizó el cambio, y timestamp. El módulo financiero recibirá el evento corregido.

- **¿Qué sucede si el módulo financiero no está disponible cuando se publica el evento?**
  El sistema persiste el estado en la base de datos primero. La publicación al módulo financiero es asíncrona; si falla, el mensaje queda en cola para reintento. El estado del pedido no se revierte.

---

## Requisitos

### Requisitos Funcionales

- **FR-001**: El sistema DEBE permitir al transportista actualizar un pedido especificando `id_pedido`, `id_transportista`, y `estado_final` válido (`Entregado Completo` / `Rechazo Parcial` / `Devolución (Error Empresa)` / `Faltante de Inventario` / `No Entregado`).

- **FR-002**: El sistema DEBE asignar automáticamente la `tasa_efectividad` según el `estado_final` seleccionado, conforme a la tabla de equivalencias:

  | `estado_final`               | `tasa_efectividad` |
  |------------------------------|--------------------|
  | Entregado Completo           | 100                |
  | Rechazo Parcial              | 80                 |
  | No Entregado                 | 0                  |
  | Devolución (Error Empresa)   | 0                  |
  | Faltante de Inventario       | -100               |

- **FR-003**: El sistema DEBE persistir los datos del pedido: `id_pedido`, `id_transportista`, `estado_final`, y `tasa_efectividad`.

- **FR-004**: El sistema DEBE publicar al módulo financiero el evento vía RabbitMQ (binding `estado-pedido.event`) con la siguiente estructura exacta, alineada con `recibir_estado_final_modulo_transporte.md`:

  ```json
  {
    "id_pedido": 123,
    "tasa_efectividad": 100,
    "id_transportista": 50
  }
  ```

  **Nota**: `estado_final` fue excluido del payload por acuerdo con el equipo del módulo financiero — solo requieren la tasa para sus cálculos de liquidación.

- **FR-005**: El sistema DEBE permitir la corrección de un `estado_final` registrado previamente (cambio a estado diferente), generando un comprobante de auditoría que documente: valor anterior, valor nuevo, `id_transportista` realizador, y timestamp de la corrección. El evento corregido DEBE ser re-publicado al módulo financiero.

- **FR-006**: El sistema DEBE generar una Alerta cuando el `estado_final` sea `No Entregado`, `Rechazo Parcial`, `Faltante de Inventario`, o `Devolución (Error Empresa)`, asignada a supervisión.

### Entidades Clave

- **Pedido**: Entidad principal de este módulo.
  - Campos: `id_pedido` (PK), `id_cliente`, `id_transportista`, `estado_final`, `tasa_efectividad`, `fecha_creacion`, `fecha_actualizacion`.

- **Transportista**: Usuario responsable de actualizar estados.
  - Campos: `id_transportista`.

- **Alerta**: Notificación generada automáticamente ante estados problemáticos.
  - Campos: `id_alerta`, `id_pedido`, `id_transportista`, `estado_final_registrado`, `tipo` (`no_entregado` / `rechazo` / `faltante` / `devolucion`), `descripcion`, `estado` (`pendiente` / `resuelta`), `asignado_a_supervisor`.

- **EventoEstadoPedido** *(salida vía RabbitMQ)*: Evento publicado al módulo financiero en la cola `estado-pedido.event`.
  - Campos: `id_pedido`, `tasa_efectividad`, `id_transportista`.
  - Restricción: estos tres campos son los únicos que recibe el módulo financiero. `estado_final` fue excluido por acuerdo de integración. El `id_cliente` y otros datos del pedido ya fueron entregados al módulo financiero por el módulo de inventario en un flujo previo.

---

## Criterios de Éxito

- **SC-001**: El transportista puede registrar el `estado_final` de un pedido en menos de 1 minuto desde que abre el formulario de actualización.

- **SC-002**: El 100% de las actualizaciones de `estado_final` persisten correctamente los cuatro campos del evento (`id_pedido`, `estado_final`, `tasa_efectividad`, `id_transportista`) antes de publicar al módulo financiero.

- **SC-003**: El 100% de los eventos publicados al módulo financiero contienen exactamente los tres campos definidos en FR-004 (`id_pedido`, `tasa_efectividad`, `id_transportista`), sin campos adicionales ni faltantes.

- **SC-004**: Ante fallo temporal del broker RabbitMQ, el estado del pedido permanece persistido en BD. El error es registrado y el pedido no se revierte. La re-entrega del evento se delega a la infraestructura de mensajería cuando la conectividad se restablezca.

- **SC-005**: El 100% de los estados con corrección previa generan un registro de auditoría con valor anterior, valor nuevo, ejecutor, y timestamp.
