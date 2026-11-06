-- =============================================================
-- MotoServices — V6: relação Cliente ↔ Moto
-- (permissiva: a primeira OS vincula automaticamente a moto)
-- =============================================================

CREATE TABLE motoservices.tb_cliente_moto (
    cliente_id  BIGINT NOT NULL,
    moto_id     BIGINT NOT NULL,
    CONSTRAINT pk_cliente_moto PRIMARY KEY (cliente_id, moto_id),
    CONSTRAINT fk_cm_cliente FOREIGN KEY (cliente_id)
        REFERENCES motoservices.tb_cliente (id) ON DELETE CASCADE,
    CONSTRAINT fk_cm_moto FOREIGN KEY (moto_id)
        REFERENCES motoservices.tb_moto (id) ON DELETE CASCADE
);

CREATE INDEX idx_cm_moto ON motoservices.tb_cliente_moto (moto_id);
