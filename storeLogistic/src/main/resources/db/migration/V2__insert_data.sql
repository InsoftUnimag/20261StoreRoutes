
INSERT INTO categorias (tipo, capacidad_maxima_kg) VALUES
                                                       ('CAMIONETA_URBANA',      1500),
                                                       ('CAMION_SENCILLO',       5000),
                                                       ('TRACTOCAMION_REGIONAL', 30000)
ON CONFLICT (tipo) DO NOTHING;

INSERT INTO vehiculos (id_categoria, capacidad_carga, estado, id_transportista, peso_actual) VALUES
                                                                                                 (1, 1500, 'DISPONIBLE',      'TRANSP001', 800),
                                                                                                 (1, 1500, 'EN_MANTENIMIENTO','TRANSP002', 0),
                                                                                                 (1, 1500, 'EN_RUTA',         'TRANSP003', 1200);

INSERT INTO vehiculos (id_categoria, capacidad_carga, estado, id_transportista, peso_actual) VALUES
                                                                                                 (2, 5000, 'DISPONIBLE',      'TRANSP004', 2500),
                                                                                                 (2, 5000, 'EN_RUTA',         'TRANSP005', 4800),
                                                                                                 (2, 5000, 'FUERA_DE_SERVICIO','TRANSP006', 0);

INSERT INTO vehiculos (id_categoria, capacidad_carga, estado, id_transportista, peso_actual) VALUES
                                                                                                 (3, 30000, 'DISPONIBLE',      'TRANSP007', 15000),
                                                                                                 (3, 30000, 'EN_RUTA',         'TRANSP008', 28000),
                                                                                                 (3, 30000, 'EN_MANTENIMIENTO','TRANSP009', 0);
