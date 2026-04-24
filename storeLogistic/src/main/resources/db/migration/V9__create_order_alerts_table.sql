
CREATE TABLE IF NOT EXISTS order_alerts (
    id_alerta               BIGSERIAL    PRIMARY KEY,
    id_pedido               BIGINT       NOT NULL REFERENCES orders(id_order) ON DELETE RESTRICT,
    id_transportista        BIGINT       NOT NULL,
    estado_final_registrado VARCHAR(50)  NOT NULL,
    tipo                    VARCHAR(50)  NOT NULL,
    descripcion             VARCHAR(500),
    estado                  VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    asignado_a_supervisor   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
