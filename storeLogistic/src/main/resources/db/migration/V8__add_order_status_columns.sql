
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS estado_final        VARCHAR(50),
    ADD COLUMN IF NOT EXISTS tasa_efectividad    INTEGER,
    ADD COLUMN IF NOT EXISTS id_cliente          BIGINT,
    ADD COLUMN IF NOT EXISTS id_transportista    BIGINT,
    ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
