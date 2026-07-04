-- =============================================================
-- MotoServices — Schema inicial
-- Convenção: OWNER/SCHEMA = MOTOSERVICES, tabelas com prefixo TB_
-- =============================================================

CREATE SCHEMA IF NOT EXISTS motoservices;

-- -------------------------------------------------------------
-- TB_MOTO — catálogo dinâmico de motos (Épico 5)
-- -------------------------------------------------------------
CREATE TABLE motoservices.tb_moto (
    id           BIGSERIAL    PRIMARY KEY,
    marca        VARCHAR(100) NOT NULL,
    modelo       VARCHAR(100) NOT NULL,
    cilindradas  INTEGER      NOT NULL
);

-- -------------------------------------------------------------
-- TB_CLIENTE — cadastro de clientes (Épico 4)
-- -------------------------------------------------------------
CREATE TABLE motoservices.tb_cliente (
    id               BIGSERIAL     PRIMARY KEY,
    nome             VARCHAR(200)  NOT NULL,
    cpf_cnpj         VARCHAR(20)   NOT NULL UNIQUE,
    contato          VARCHAR(100)  NOT NULL,
    proxima_revisao  DATE
);

-- -------------------------------------------------------------
-- TB_PECA — estoque de peças (Épico 3)
-- -------------------------------------------------------------
CREATE TABLE motoservices.tb_peca (
    id                  BIGSERIAL     PRIMARY KEY,
    codigo              VARCHAR(50)   NOT NULL UNIQUE,
    descricao           VARCHAR(255)  NOT NULL,
    valor               NUMERIC(12,2) NOT NULL,
    quantidade_estoque  INTEGER       NOT NULL,
    estoque_minimo      INTEGER       NOT NULL
);

-- -------------------------------------------------------------
-- TB_ORDEM_DE_SERVICO — OS (Épico 2)
-- -------------------------------------------------------------
CREATE TABLE motoservices.tb_ordem_de_servico (
    id              BIGSERIAL    PRIMARY KEY,
    cliente_id      BIGINT       NOT NULL,
    moto_id         BIGINT       NOT NULL,
    status          VARCHAR(30)  NOT NULL,
    data_abertura   TIMESTAMP    NOT NULL,
    data_fechamento TIMESTAMP,
    CONSTRAINT fk_os_cliente FOREIGN KEY (cliente_id)
        REFERENCES motoservices.tb_cliente (id),
    CONSTRAINT fk_os_moto FOREIGN KEY (moto_id)
        REFERENCES motoservices.tb_moto (id)
);

-- -------------------------------------------------------------
-- TB_SERVICO_DA_OFICINA — raiz da hierarquia (JPA JOINED)
-- Subtipos: TB_SERVICO_REVISAO e TB_SERVICO_TROCA_PECA
-- -------------------------------------------------------------
CREATE TABLE motoservices.tb_servico_da_oficina (
    id                       BIGSERIAL     PRIMARY KEY,
    descricao                VARCHAR(255)  NOT NULL,
    tempo_estimado           INTEGER,
    valor_base_mao_de_obra   NUMERIC(12,2) NOT NULL,
    ordem_de_servico_id      BIGINT,
    CONSTRAINT fk_servico_os FOREIGN KEY (ordem_de_servico_id)
        REFERENCES motoservices.tb_ordem_de_servico (id)
);

-- -------------------------------------------------------------
-- TB_SERVICO_REVISAO — subtipo Revisão
-- -------------------------------------------------------------
CREATE TABLE motoservices.tb_servico_revisao (
    servico_id       BIGINT PRIMARY KEY,
    checklist_geral  BOOLEAN,
    CONSTRAINT fk_revisao_servico FOREIGN KEY (servico_id)
        REFERENCES motoservices.tb_servico_da_oficina (id)
);

-- -------------------------------------------------------------
-- TB_SERVICO_TROCA_PECA — subtipo Troca de Peça
-- -------------------------------------------------------------
CREATE TABLE motoservices.tb_servico_troca_peca (
    servico_id                BIGINT    PRIMARY KEY,
    peca_id                   BIGINT,
    peca_fornecida_pela_loja  BOOLEAN   NOT NULL,
    CONSTRAINT fk_troca_servico FOREIGN KEY (servico_id)
        REFERENCES motoservices.tb_servico_da_oficina (id),
    CONSTRAINT fk_troca_peca FOREIGN KEY (peca_id)
        REFERENCES motoservices.tb_peca (id)
);
