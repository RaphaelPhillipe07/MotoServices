-- =============================================================
-- MotoServices — V2: índices, unicidade e constraints de integridade
-- =============================================================

-- -------------------------------------------------------------
-- Índices de chaves estrangeiras
-- (no PostgreSQL a FK NÃO cria índice automaticamente)
-- -------------------------------------------------------------
CREATE INDEX idx_os_cliente ON motoservices.tb_ordem_de_servico (cliente_id);
CREATE INDEX idx_os_moto ON motoservices.tb_ordem_de_servico (moto_id);
CREATE INDEX idx_os_status ON motoservices.tb_ordem_de_servico (status);
CREATE INDEX idx_os_data_abertura ON motoservices.tb_ordem_de_servico (data_abertura);
CREATE INDEX idx_servico_os ON motoservices.tb_servico_da_oficina (ordem_de_servico_id);
CREATE INDEX idx_troca_peca_peca ON motoservices.tb_servico_troca_peca (peca_id);
CREATE INDEX idx_moto_cilindradas ON motoservices.tb_moto (cilindradas);

-- -------------------------------------------------------------
-- Catálogo: marca + modelo únicos, sem diferenciar maiúsculas
-- (alinha com MotoDAO.findByMarcaIgnoreCaseAndModeloIgnoreCase)
-- -------------------------------------------------------------
CREATE UNIQUE INDEX uk_moto_marca_modelo
    ON motoservices.tb_moto (lower(marca), lower(modelo));

-- -------------------------------------------------------------
-- Busca de cliente por nome (contains) — índice trigram
-- -------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_cliente_nome_trgm
    ON motoservices.tb_cliente USING gin (lower(nome) gin_trgm_ops);

-- -------------------------------------------------------------
-- CHECKs de integridade de domínio
-- -------------------------------------------------------------
ALTER TABLE motoservices.tb_peca
    ADD CONSTRAINT ck_peca_valor_nao_negativo CHECK (valor >= 0),
    ADD CONSTRAINT ck_peca_estoque_nao_negativo CHECK (quantidade_estoque >= 0),
    ADD CONSTRAINT ck_peca_estoque_minimo_nao_negativo CHECK (estoque_minimo >= 0);

ALTER TABLE motoservices.tb_servico_da_oficina
    ADD CONSTRAINT ck_servico_valor_base_nao_negativo CHECK (valor_base_mao_de_obra >= 0);

ALTER TABLE motoservices.tb_ordem_de_servico
    ADD CONSTRAINT ck_os_status_valido CHECK (
        status IN ('ABERTA', 'EM_ANDAMENTO', 'AGUARDANDO_CLIENTE', 'CONCLUIDA', 'FECHADA')
    );
