
CREATE TABLE IF NOT EXISTS categorias (
                                          id_categoria     BIGSERIAL PRIMARY KEY,
                                          tipo             VARCHAR(50)    NOT NULL UNIQUE,
                                          capacidad_maxima_kg NUMERIC     NOT NULL CHECK (capacidad_maxima_kg > 0),
                                          created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vehiculos (
                                         id_vehiculo      BIGSERIAL PRIMARY KEY,
                                         id_categoria     BIGINT        NOT NULL REFERENCES categorias(id_categoria) ON DELETE RESTRICT,
                                         capacidad_carga  NUMERIC       NOT NULL CHECK (capacidad_carga > 0),
                                         estado           VARCHAR(50)   NOT NULL DEFAULT 'EN_MANTENIMIENTO',
                                         id_transportista VARCHAR(100)  NOT NULL,
                                         peso_actual      NUMERIC       NOT NULL DEFAULT 0 CHECK (peso_actual >= 0),
                                         created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vehiculos_estado          ON vehiculos(estado);
CREATE INDEX IF NOT EXISTS idx_vehiculos_id_categoria    ON vehiculos(id_categoria);
CREATE INDEX IF NOT EXISTS idx_vehiculos_id_transportista ON vehiculos(id_transportista);
