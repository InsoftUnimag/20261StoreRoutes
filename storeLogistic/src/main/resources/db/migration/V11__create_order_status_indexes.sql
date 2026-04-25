
CREATE INDEX IF NOT EXISTS idx_orders_estado_final         ON orders(estado_final);
CREATE INDEX IF NOT EXISTS idx_orders_id_transportista_os  ON orders(id_transportista);
CREATE INDEX IF NOT EXISTS idx_order_alerts_id_pedido      ON order_alerts(id_pedido);
CREATE INDEX IF NOT EXISTS idx_order_status_audit_pedido   ON order_status_audit(id_pedido);
