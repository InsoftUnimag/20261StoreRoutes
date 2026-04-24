# Feature Specification: Consultar Método de Pago de un Pedido

## User Story 

### Consulta de Forma de Pago al Módulo Financiero (Priority: P1)

Yo como módulo de Logística de Despacho y Distribución consulto el método de pago de un pedido al módulo Financiero, enviando solo el idPedido, para determinar las condiciones de pago y procesar la entrega correctamente.

**Why this priority**: Es crítico validar la forma de pago antes de completar la entrega. El método determina si se cobra en entrega (CONTRA_ENTREGA) o si el cliente tiene cartera comercial (CARTERA_COMERCIAL).

**Independent Test**: Puede probarse de forma independiente cuando se consulta el método de pago de un pedido válido y el sistema retorna: id_pedido y forma_pago. La consulta es síncrona al endpoint del Módulo Financiero.

**Communication Pattern**: Síncrono. Llamada directa al endpoint del Módulo Financiero.

**Acceptance Scenarios**:

1. **Scenario**: Consulta exitosa retorna CONTRA_ENTREGA
   - **Given** existe un pedido con forma_pago = CONTRA_ENTREGA
   - **When** se envía GET /api/v1/pedidos/{id_pedido}/forma-pago
   - **Then** se retorna {id_pedido, forma_pago: "CONTRA_ENTREGA"}
   
2. **Scenario**: Consulta exitosa retorna CARTERA_COMERCIAL
   - **Given** existe un pedido con forma_pago = CARTERA_COMERCIAL
   - **When** se envía GET /api/v1/pedidos/{id_pedido}/forma-pago
   - **Then** se retorna {id_pedido, forma_pago: "CARTERA_COMERCIAL"}

3. **Scenario**: Error cuando el pedido no existe
   - **Given** se consulta con un idPedido que no existe
   - **When** se envía GET /api/v1/pedidos/{id_pedido}/forma-pago
   - **Then** el Módulo Financiero retorna error: "Pedido no encontrado"

---

### Edge Cases

- **¿Qué sucede cuando el pedido no existe?** El Módulo Financiero retorna: "Pedido no encontrado"

- **¿Qué sucede cuando el cliente no tiene forma de pago asignada?** El Módulo Financiero retorna: "El cliente no tiene forma de pago registrada"

- **¿Qué sucede si el endpoint del Módulo Financiero no está disponible?** Se retorna un timeout o error de conexión; el módulo de Logística debe manejar reintentos.


## Requirements 


### Functional Requirements

- **FR-001**: System MUST [enviar GET request al Módulo Financiero con endpoint /api/v1/pedidos/{id_pedido}/forma-pago usando solo el idPedido.]

- **FR-002**: System MUST [procesar la respuesta del Módulo Financiero que retorna {id_pedido, forma_pago} con forma_pago = CONTRA_ENTREGA o CARTERA_COMERCIAL.]

- **FR-003**: System MUST [manejar errores del Módulo Financiero: "Pedido no encontrado" o "El cliente no tiene forma de pago registrada".]

- **FR-004**: System MUST [retentar la consulta en caso de timeout o indisponibilidad del Módulo Financiero (máximo 3 reintentos con backoff exponencial).]


### Key Entities

- **[Pedido]**: Documento de entrega. Atributos: idPedido, idCliente, forma_pago.

- **[Forma de Pago]**: Método de pago del cliente. Valores válidos:
  * `CONTRA_ENTREGA`: Pago al momento de la entrega
  * `CARTERA_COMERCIAL`: Crédito comercial (sin pago en entrega)

### Request/Response Contract

**Request (Módulo Logística → Módulo Financiero)**:
```
GET /api/v1/pedidos/{id_pedido}/forma-pago
```

**Success Response (HTTP 200)**:
```json
{
  "id_pedido": 123,
  "forma_pago": "CONTRA_ENTREGA"
}
```


## Success Criteria 

### Measurable Outcomes

- **SC-001**: Exactitud en la consulta. El 100% de las consultas deben retornar forma_pago correcta (CONTRA_ENTREGA o CARTERA_COMERCIAL) o error válido.

- **SC-002**: Disponibilidad. El 99% de las consultas deben completarse en menos de 2 segundos (incluidos reintentos).

- **SC-003**: Integridad de datos. El 100% de las respuestas exitosas deben incluir idPedido y forma_pago exactamente como lo retorna el Módulo Financiero.