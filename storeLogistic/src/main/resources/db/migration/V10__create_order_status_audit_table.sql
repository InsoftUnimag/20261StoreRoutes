
CREATE TABLE IF NOT EXISTS order_status_audit (
    id_auditoria     BIGSERIAL   PRIMARY KEY,
    id_pedido        BIGINT      NOT NULL REFERENCES orders(id_order) ON DELETE RESTRICT,
    estado_anterior  VARCHAR(50),
    estado_nuevo     VARCHAR(50) NOT NULL,
    id_transportista BIGINT      NOT NULL,
    timestamp        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
