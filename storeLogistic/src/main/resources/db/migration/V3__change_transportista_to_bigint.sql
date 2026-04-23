UPDATE vehiculos SET id_transportista = '1' WHERE id_transportista NOT SIMILAR TO '[0-9]+';
ALTER TABLE vehiculos ALTER COLUMN id_transportista TYPE BIGINT USING id_transportista::BIGINT;