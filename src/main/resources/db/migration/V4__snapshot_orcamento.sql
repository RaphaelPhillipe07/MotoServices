-- =============================================================
-- MotoServices — V4: snapshot do orçamento no fechamento da OS
-- =============================================================

ALTER TABLE motoservices.tb_ordem_de_servico
    ADD COLUMN total_pecas NUMERIC(12,2),
    ADD COLUMN total_mao_de_obra NUMERIC(12,2),
    ADD COLUMN desconto_aplicado NUMERIC(12,2),
    ADD COLUMN total_final NUMERIC(12,2);
