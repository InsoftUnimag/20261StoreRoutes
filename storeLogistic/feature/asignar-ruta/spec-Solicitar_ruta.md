# Feature Specification: Recibir y Procesar Solicitud de Ruta para Entrega

## User Story 

### Recepción Asíncrona de Solicitud de Ruta del Módulo de Inventario (Priority: P1)

Como módulo de Logística de Despacho y Distribución, recibo solicitudes de ruta de forma asíncrona desde el módulo de Inventario con la información del pedido (idPedido, idCliente, pesoTotal, dirección de entrega), con el fin de asignar o crear una ruta y planificar la entrega.

**Why this priority**: El módulo de Inventario requiere que el módulo de Logística procese solicitudes de ruta de forma asíncrona para poder ejecutar entregas. Sin esta comunicación, no es posible planificar las entregas.

**Independent Test**: Puede probarse de forma independiente cuando el módulo de Inventario envía un evento "SolicitudRutaRequerida" con idPedido, idCliente, pesoTotal y dirección de entrega, y el módulo de Logística: (1) Recibe la solicitud, (2) Asigna o crea una ruta, (3) Responde con los datos de la ruta asignada.

**Communication Pattern**: Asíncrono vía queue/evento. El módulo de Inventario publica un evento "SolicitudRutaRequerida" y el módulo de Logística responde publicando un evento "RutaAsignada".

**Acceptance Scenarios**:

1. **Scenario**: Recibe solicitud de ruta y asigna ruta existente con capacidad
   - **Given** el módulo de Inventario envía evento "SolicitudRutaRequerida" con idPedido, idCliente, pesoTotal, dirección de entrega, y existe ruta con capacidad disponible
   - **When** el módulo de Logística procesa la solicitud asincronamente
   - **Then** asigna el pedido a la ruta existente y publica evento "RutaAsignada" con idRuta y fecha_despacho
   
2. **Scenario**: Recibe solicitud de ruta sin disponibilidad
   - **Given** el módulo de Inventario envía evento "SolicitudRutaRequerida" y no hay ruta disponible
   - **When** el módulo de Logística procesa la solicitud asincronamente
   - **Then** crea una nueva ruta y publica evento "RutaAsignada" con idRuta de la nueva ruta

3. **Scenario**: Recibe solicitud de ruta y cierra ruta al alcanzar 95% capacidad
   - **Given** el módulo de Inventario envía evento "SolicitudRutaRequerida" y la asignación alcanza el 95% de capacidad
   - **When** el módulo de Logística procesa la solicitud asincronamente
   - **Then** asigna el pedido, marca la ruta como cerrada y publica evento "RutaAsignada" con idRuta y fecha_despacho
---


### Edge Cases

¿Qué sucede cuando la capacidad restante de la ruta es exactamente igual al pesoTotal del pedido?
El módulo debe permitir la asignación, marcar la ruta como completa y publicar evento "RutaAsignada" con estado=cerrada.

¿Qué sucede cuando el pesoTotal del pedido supera la capacidad máxima permitida por una ruta?
El módulo debe crear una nueva ruta y publicar evento "RutaAsignada" con la información de la nueva ruta.

¿Qué sucede si el evento llega con datos incompletos o inválidos?
El módulo debe publicar evento "ErrorSolicitudRuta" con descripción del error para que el módulo de Inventario lo procese.



## Requirements 

### Functional Requirements

- **FR-001**: System MUST [permitir que el módulo de Logística reciba de forma asíncrona solicitudes de ruta desde el módulo de Inventario mediante eventos/queue.]
- **FR-002**: System MUST [procesar la solicitud de ruta asíncrona con los datos: idPedido, idCliente, pesoTotal del pedido, dirección de entrega.]
- **FR-003**: System MUST [asignar el pedido a una ruta existente si tiene capacidad disponible para el pesoTotal.]
- **FR-004**: System MUST [crear una nueva ruta cuando no exista ninguna con capacidad disponible para el pesoTotal.]
- **FR-005**: System MUST [publicar evento "RutaAsignada" de respuesta con los datos: idPedido, idRuta, fecha_despacho, para que el módulo de Inventario la consume.]
- **FR-006**: System MUST [publicar evento "ErrorSolicitudRuta" cuando hay error en el procesamiento de la solicitud.]
- **FR-007**: System MUST [marcar ruta como cerrada cuando la carga alcance el 95% de capacidad.]


### Key Entities 

- **[Ruta]**: Una serie de paradas asignada a un vehículo. Atributos: idRuta, lista de paradas, lista de pedidos, vehículo asignado (idVehículo), fecha de despacho, estado de la ruta (disponible/cerrada), capacidad utilizada.
- **[Pedido]**: Documento de entrega. Atributos: idPedido, idCliente, pesoTotal, dirección de entrega.

### Event Contracts (Comunicación Asíncrona)

**Evento Entrada - SolicitudRutaRequerida (del Módulo de Inventario al Módulo de Logística)**:
```json
{
  "idPedido": 123,
  "idCliente": 45,
  "pesoTotal": 500,
  "direccion_entrega": "Calle Principal 123, Apartado 4A"
}
```

**Evento Salida - RutaAsignada (del Módulo de Logística al Módulo de Inventario)**:
```json
{
  "idPedido": 123,
  "idRuta": 67,
  "fecha_despacho": "2026-03-14"
}
```


## Success Criteria 

### Measurable Outcomes

- **SC-001**: Exactitud en la asignación. El 100% de las solicitudes de ruta (evento SolicitudRutaRequerida) deben ser procesadas correctamente, ya sea asignando a una ruta existente con capacidad suficiente o creando una nueva ruta, respondiendo con evento \"RutaAsignada\".


- **SC-002**: Integridad de datos. El 100% de los pedidos asignados deben tener una ruta asociada válida. El evento "RutaAsignada" debe incluir: idPedido, idRuta, fecha_despacho.


