# Integración Módulo de Transporte - Consulta de Pago

Este documento detalla el endpoint creado para que el Módulo de Transporte consulte la forma de pago y el valor a cobrar de un pedido en específico.

## Endpoint

**`GET`** `/api/v1/pedidos/{id_pedido}/pago-transporte`

## Parámetros de Ruta

| Parámetro | Tipo | Descripción |
| :--- | :--- | :--- |
| `id_pedido` | `Long` | ID único del pedido que se desea consultar. |

## Respuesta Exitosa (200 OK)

El endpoint devuelve un objeto en formato `JSON` con la siguiente estructura:

```json
{
  "idPedido": 12345,
  "formaPago": "CONTRA_ENTREGA",
  "valorContraEntrega": 50000.00
}
```

### Descripción de los campos

*   **`idPedido`** (`Long`): El mismo ID de pedido que se solicitó en la URL.
*   **`formaPago`** (`String`): El método de pago registrado para el pedido (Ej: `"CONTRA_ENTREGA"`, `"CARTERA_COMERCIAL"`).
*   **`valorContraEntrega`** (`BigDecimal`): 
    *   Si la forma de pago es `"CONTRA_ENTREGA"`, este campo contendrá el **total a cobrar** por el pedido.
    *   Si la forma de pago es distinta, este valor siempre será **`0`**.
