-- =============================================================
-- MotoServices — V7: cancelamento, forma de pagamento e índices
-- =============================================================

-- Cancelamento passa a ser um status válido da OS.
ALTER TABLE motoservices.tb_ordem_de_servico
    DROP CONSTRAINT ck_os_status_valido;

ALTER TABLE motoservices.tb_ordem_de_servico
    ADD CONSTRAINT ck_os_status_valido CHECK (
        status IN ('ABERTA', 'EM_ANDAMENTO', 'AGUARDANDO_CLIENTE', 'CONCLUIDA', 'FECHADA', 'CANCELADA')
    );

ALTER TABLE motoservices.tb_os_status_historico
    DROP CONSTRAINT ck_historico_status_novo,
    DROP CONSTRAINT ck_historico_status_anterior;

ALTER TABLE motoservices.tb_os_status_historico
    ADD CONSTRAINT ck_historico_status_novo CHECK (
        status_novo IN ('ABERTA', 'EM_ANDAMENTO', 'AGUARDANDO_CLIENTE', 'CONCLUIDA', 'FECHADA', 'CANCELADA')
    ),
    ADD CONSTRAINT ck_historico_status_anterior CHECK (
        status_anterior IS NULL
        OR status_anterior IN ('ABERTA', 'EM_ANDAMENTO', 'AGUARDANDO_CLIENTE', 'CONCLUIDA', 'FECHADA', 'CANCELADA')
    );

-- Forma de pagamento informada no fechamento (opcional).
ALTER TABLE motoservices.tb_ordem_de_servico
    ADD COLUMN forma_pagamento VARCHAR(30);

ALTER TABLE motoservices.tb_ordem_de_servico
    ADD CONSTRAINT ck_os_forma_pagamento CHECK (
        forma_pagamento IS NULL
        OR forma_pagamento IN ('DINHEIRO', 'PIX', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'BOLETO', 'OUTRO')
    );

-- Consultas de agenda de revisões e relatórios por período de fechamento.
CREATE INDEX idx_cliente_proxima_revisao
    ON motoservices.tb_cliente (proxima_revisao);

CREATE INDEX idx_os_data_fechamento
    ON motoservices.tb_ordem_de_servico (data_fechamento);

CREATE INDEX idx_os_status_fechamento
    ON motoservices.tb_ordem_de_servico (status, data_fechamento);
