
ALTER TABLE stops
    ADD COLUMN IF NOT EXISTS customer_contact VARCHAR(200);

CREATE INDEX IF NOT EXISTS idx_stops_sequence ON stops(id_route, sequence);
