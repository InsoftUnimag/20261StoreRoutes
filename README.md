# Distribución Mayorista

Este documento define la estructura técnica y lógica para una distribuidora de bebidas a gran escala, centrada en la eficiencia de la cadena de suministro, la optimización de flota por capacidad de carga y la liquidación financiera de última milla.

---

## 🏗️ Módulo 1: Gestión de Inventario y Abastecimiento
Responsable del control de flujo de entrada desde plantas de producción y la gestión de existencias en centros de distribución.

### 1.1. Catalogación de Productos (SKU)
Cada referencia debe estar parametrizada para permitir cálculos logísticos precisos:
* **SKU ID:** Identificador único por marca y presentación.
* **Presentación:** Unidad, Six-pack, Caja, Barril o Estiba.
* **Peso Logístico:** Peso bruto total de la unidad de empaque (dato maestro para el Módulo 2).
* **Gestión de Vida Útil:** Control de fechas de vencimiento bajo metodología FEFO (First Expired, First Out).

### 1.2. Ciclo de Vida del Stock
El sistema debe rastrear el estado de la mercancía dentro del Centro de Distribución:
* **Recepción:** Ingreso validado contra manifiesto de fábrica.
* **Disponible:** Stock listo para la venta.
* **Comprometido:** Producto vinculado a pedidos en proceso.
* **En Picking:** Mercancía en etapa de alistamiento.
* **Despachado:** Producto cargado y fuera de la bodega.
* **Excepciones de Inventario:**
    * ⚠️ **Avería:** Producto dañado durante la manipulación.
    * ❌ **Vencido:** Stock que ha superado su fecha de consumo.

---

## 🚚 Módulo 2: Logística de Despacho y Distribución
Módulo encargado de la ejecución de entregas y la optimización del uso de la flota vehicular.

### 2.1. Gestión de Capacidad de Flota
La flota se categoriza para maximizar la rentabilidad por viaje:
* **Camioneta Urbana:** Capacidad hasta 1.5 toneladas.
* **Camión Sencillo:** Capacidad hasta 5 toneladas.
* **Tractocamión Regional:** Capacidad superior a 25 toneladas.

### 2.2. Planificación de Rutas por Carga Crítica
El sistema genera rutas optimizadas siguiendo estas reglas:
1. **Consolidación de Carga:** Los pedidos se agrupan en vehículos hasta alcanzar el **95% de su capacidad de peso**.
2. **Secuenciación de Paradas:** Orden logístico basado en geocodificación y ventanas de entrega del cliente.
3. **Gestión de Entregas (Novedades):**
    * **Entrega Exitosa:** Confirmada la entrega.
    * **Rechazo Parcial:** Devolución de producto por parte del cliente.
    * **No Entregado:** Fallo por local cerrado o restricciones de acceso.

---

## 💰 Módulo 3: Conciliación Financiera y Liquidación Comercial
Analiza el resultado de la operación logística para ejecutar cobros a clientes y pagos a aliados de transporte.

### 3.1. Modelos de Cobro y Recaudo
El sistema procesa diferentes modalidades de pago:
* **Pago Contra Entrega (COD):** Conciliación inmediata del dinero recaudado por el transportador.
* **Cartera Comercial:** Gestión de facturación a crédito para grandes superficies.

### 3.2. Matriz de Liquidación Logística
La remuneración a la flota de distribución se calcula según la efectividad reportada en el Módulo 2:

| Resultado de la Entrega | % Pago Logístico | Acción Financiera |
| :--- | :--- | :--- |
| **Entregado Completo** | 100% | Ingreso total validado. |
| **Rechazo Parcial** | 80% | Generación automática de nota crédito. |
| **Devolución (Error Empresa)** | 0% | El costo del flete es asumido como pérdida operativa. |
| **Faltante de Inventario** | -100% | Descuento del valor comercial al transportador por pérdida. |


