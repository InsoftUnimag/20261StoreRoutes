
ALTER TABLE routes
    ADD COLUMN IF NOT EXISTS id_carrier BIGINT REFERENCES carrier(id_carrier) ON DELETE SET NULL;

ALTER TABLE stops
    ADD COLUMN IF NOT EXISTS customer_contact VARCHAR(200);

CREATE INDEX IF NOT EXISTS idx_routes_id_carrier ON routes(id_carrier);
CREATE INDEX IF NOT EXISTS idx_stops_sequence    ON stops(id_route, sequence);
