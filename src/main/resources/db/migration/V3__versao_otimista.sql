-- =============================================================
-- MotoServices — V3: controle de concorrência otimista
-- =============================================================

ALTER TABLE motoservices.tb_peca
    ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;

ALTER TABLE motoservices.tb_ordem_de_servico
    ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
