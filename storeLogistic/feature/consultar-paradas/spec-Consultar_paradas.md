# Feature Specification: Consultar paradas de las rutas

## User Scenarios & Testing

### User Story 1 — Visualizar lista de paradas de la ruta asignada (Priority: P1)

Como transportista (conductor), quiero ver la lista completa de paradas asignadas a mi ruta —ordenadas por secuencia— para tener un panorama de mis entregas del día.

**Why this priority**: Es la pantalla principal del conductor. Sin ella no puede planificar ni ejecutar su jornada.

**Acceptance Scenarios**:

1. **Scenario**: Consulta exitosa — ruta asignada con paradas
   - **Given** una ruta asignada a un vehículo con `id_transportista = carrierId`, con múltiples paradas
   - **When** el transportista solicita `GET /api/v1/logistics/routes/{routeId}/stops?carrierId={carrierId}`
   - **Then** HTTP 200 con lista ordenada por `sequence` ASC. Cada parada incluye: `idStop`, `sequence`, `deliveryAddress`, `orderId`, `status`.

2. **Scenario**: Ruta con cero paradas
   - **Given** una ruta asignada al transportista sin paradas registradas
   - **When** el transportista consulta la ruta
   - **Then** HTTP 200 con `stops: []` y `totalStops: 0`.

---

### User Story 2 — Ver detalle completo de una parada (Priority: P1)

Como transportista, quiero ver el detalle completo de una parada específica —incluyendo el método de pago y el monto a cobrar— para saber exactamente cómo gestionar la entrega.

**Why this priority**: El conductor necesita saber si debe cobrar en efectivo (CONTRA_ENTREGA) o si el pago ya está gestionado (CARTERA_COMERCIAL) antes de llegar a la puerta del cliente.

**Acceptance Scenarios**:

1. **Scenario**: Detalle exitoso — parada CONTRA_ENTREGA
   - **Given** una parada con pedido de método CONTRA_ENTREGA
   - **When** el transportista solicita `GET /api/v1/logistics/routes/{routeId}/stops/{stopId}?carrierId={carrierId}`
   - **Then** HTTP 200 con `idStop`, `sequence`, `deliveryAddress`, `orderId`, `customerContact`, `paymentMethod: "CONTRA_ENTREGA"`, `totalACobrar: 150000.00`, `status`.

2. **Scenario**: Detalle exitoso — parada CARTERA_COMERCIAL
   - **Given** una parada con pedido de método CARTERA_COMERCIAL
   - **When** el transportista solicita el detalle
   - **Then** HTTP 200 con `paymentMethod: "CARTERA_COMERCIAL"`, `totalACobrar: null` (el conductor NO cobra).

---

### Edge Cases

1. **Transportista intenta consultar ruta no asignada a su vehículo**
   Respuesta: HTTP 403 `"Acceso denegado"` — sin revelar si la ruta existe.

2. **routeId inexistente**
   Respuesta: HTTP 403 `"Acceso denegado"` — misma respuesta (ambigüedad intencional por seguridad).

3. **stopId no pertenece a la ruta indicada**
   Respuesta: HTTP 403 `"Acceso denegado"`.

---

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir a un transportista listar las paradas de una ruta, verificando que la ruta esté asignada al vehículo con su `id_transportista`. Datos mínimos por parada: `idStop`, `sequence`, `deliveryAddress`, `orderId`, `status`.
- **FR-002**: El sistema DEBE permitir al transportista ver el detalle de una parada específica, incluyendo: `customerContact`, `paymentMethod` (obtenido del Módulo Financiero via `ConsultPaymentMethodUseCase`), y `totalACobrar` (null si CARTERA_COMERCIAL, monto si CONTRA_ENTREGA).
- **FR-003**: El sistema DEBE restringir el acceso únicamente a rutas cuyo vehículo tenga `id_transportista = carrierId`. El rechazo (403) no debe revelar si la ruta existe o pertenece a otro transportista.
- **FR-004**: El sistema DEBE registrar logs de auditoría (INFO en consulta exitosa, WARN en acceso denegado).

### Key Entities

- **Ruta**: `id_route`, `id_vehicle`, `status`, `dispatch_date`.
- **Parada**: `id_stop`, `id_route`, `id_order`, `sequence`, `delivery_address`, `customer_contact`, `status`.
- **Vehículo**: `id_vehiculo`, `id_transportista (BIGINT)` — enlace entre ruta y transportista.
- **Transportista**: identificado por `id_transportista (Long)` — módulo externo, sin tabla propia en este módulo.
- **Método de pago**: consultado al Módulo Financiero por `id_order`. Retorna `paymentMethod` y `totalPedido` (nullable).

---

## Request / Response Contract

### Endpoint 1 — Lista de paradas

```
GET /api/v1/logistics/routes/{routeId}/stops?carrierId={carrierId}
```

**Params:** `routeId` (Long, path, requerido), `carrierId` (Long, query, requerido)

**Response 200:**
```json
{
  "routeId": 1,
  "carrierId": 42,
  "totalStops": 2,
  "stops": [
    { "idStop": 10, "sequence": 1, "deliveryAddress": "Calle 10 #20-30", "orderId": 5, "status": "PENDING" },
    { "idStop": 11, "sequence": 2, "deliveryAddress": "Carrera 5 #15-20", "orderId": 6, "status": "PENDING" }
  ]
}
```

### Endpoint 2 — Detalle de una parada

```
GET /api/v1/logistics/routes/{routeId}/stops/{stopId}?carrierId={carrierId}
```

**Params:** `routeId` (Long, path), `stopId` (Long, path), `carrierId` (Long, query)

**Response 200 — CONTRA_ENTREGA:**
```json
{
  "idStop": 10,
  "sequence": 1,
  "deliveryAddress": "Calle 10 #20-30",
  "orderId": 5,
  "customerContact": "3001234567",
  "paymentMethod": "CONTRA_ENTREGA",
  "totalACobrar": 150000.00,
  "status": "PENDING"
}
```

**Response 200 — CARTERA_COMERCIAL:**
```json
{
  "idStop": 11,
  "sequence": 2,
  "deliveryAddress": "Carrera 5 #15-20",
  "orderId": 6,
  "customerContact": "3009876543",
  "paymentMethod": "CARTERA_COMERCIAL",
  "totalACobrar": null,
  "status": "PENDING"
}
```

**Response 403:**
```json
{ "codigo": "ACCESO_DENEGADO", "mensaje": "Acceso denegado", "timestamp": "..." }
```

**Response 400:**
```json
{ "codigo": "PARAMETRO_REQUERIDO", "mensaje": "Parámetro requerido: carrierId", "timestamp": "..." }
```

---

## Success Criteria

- **SC-001**: Lista de paradas retorna en menos de 3 segundos para rutas con hasta 20 paradas (sin llamadas externas).
- **SC-002**: Detalle de parada retorna en menos de 3 segundos (incluye una llamada al Módulo Financiero con reintentos).
- **SC-003**: El 95% de las consultas se completan sin errores en condiciones normales.
- **SC-004**: El 403 nunca revela si la ruta existe o pertenece a otro transportista.