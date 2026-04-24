
CREATE TABLE IF NOT EXISTS orders (
    id_order           BIGSERIAL PRIMARY KEY,
    logistic_weight    NUMERIC       NOT NULL CHECK (logistic_weight > 0),
    delivery_address   VARCHAR(500)  NOT NULL,
    created_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS routes (
    id_route               BIGSERIAL PRIMARY KEY,
    id_vehicle             BIGINT        REFERENCES vehiculos(id_vehiculo) ON DELETE SET NULL,
    total_capacity_kg      NUMERIC       NOT NULL CHECK (total_capacity_kg > 0),
    accumulated_weight_kg  NUMERIC       NOT NULL DEFAULT 0 CHECK (accumulated_weight_kg >= 0),
    status                 VARCHAR(30)   NOT NULL DEFAULT 'AVAILABLE',
    dispatch_date          DATE          NOT NULL,
    created_at             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stops (
    id_stop           BIGSERIAL PRIMARY KEY,
    id_route          BIGINT        NOT NULL REFERENCES routes(id_route) ON DELETE CASCADE,
    id_order          BIGINT        NOT NULL REFERENCES orders(id_order) ON DELETE RESTRICT,
    sequence          INT           NOT NULL,
    delivery_address  VARCHAR(500)  NOT NULL,
    status            VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    delivery_date     DATE,
    CONSTRAINT uq_route_sequence UNIQUE (id_route, sequence),
    CONSTRAINT uq_order_stop    UNIQUE (id_order)
);