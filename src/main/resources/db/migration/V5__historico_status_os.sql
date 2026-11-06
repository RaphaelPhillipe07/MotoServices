-- =============================================================
-- MotoServices — V5: histórico de transições de status da OS
-- =============================================================

CREATE TABLE motoservices.tb_os_status_historico (
    id                   BIGSERIAL    PRIMARY KEY,
    ordem_de_servico_id  BIGINT       NOT NULL,
    status_anterior      VARCHAR(30),
    status_novo          VARCHAR(30)  NOT NULL,
    data_hora            TIMESTAMP    NOT NULL,
    CONSTRAINT fk_historico_os FOREIGN KEY (ordem_de_servico_id)
        REFERENCES motoservices.tb_ordem_de_servico (id) ON DELETE CASCADE,
    CONSTRAINT ck_historico_status_novo CHECK (
        status_novo IN ('ABERTA', 'EM_ANDAMENTO', 'AGUARDANDO_CLIENTE', 'CONCLUIDA', 'FECHADA')
    ),
    CONSTRAINT ck_historico_status_anterior CHECK (
        status_anterior IS NULL
        OR status_anterior IN ('ABERTA', 'EM_ANDAMENTO', 'AGUARDANDO_CLIENTE', 'CONCLUIDA', 'FECHADA')
    )
);

CREATE INDEX idx_historico_os_data
    ON motoservices.tb_os_status_historico (ordem_de_servico_id, data_hora);
