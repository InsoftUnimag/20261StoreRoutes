
CREATE INDEX IF NOT EXISTS idx_routes_status             ON routes(status);
CREATE INDEX IF NOT EXISTS idx_routes_accumulated_weight ON routes(accumulated_weight_kg);
CREATE INDEX IF NOT EXISTS idx_stops_id_route            ON stops(id_route);