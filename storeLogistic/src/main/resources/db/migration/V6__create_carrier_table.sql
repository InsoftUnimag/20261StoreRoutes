
CREATE TABLE IF NOT EXISTS carrier (
    id_carrier   BIGSERIAL     PRIMARY KEY,
    name         VARCHAR(200)  NOT NULL,
    status       VARCHAR(30)   NOT NULL DEFAULT 'ACTIVE',
    email        VARCHAR(200)  NOT NULL UNIQUE
);
