-- =============================================================
-- MotoServices — V8: usuários e papéis de acesso (autenticação)
-- =============================================================

CREATE TABLE motoservices.tb_usuario (
    id        BIGSERIAL     PRIMARY KEY,
    username  VARCHAR(60)   NOT NULL UNIQUE,
    nome      VARCHAR(200)  NOT NULL,
    senha     VARCHAR(100)  NOT NULL,
    papel     VARCHAR(30)   NOT NULL,
    ativo     BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_usuario_papel CHECK (papel IN ('ADMIN', 'ATENDENTE', 'MECANICO'))
);

CREATE INDEX idx_usuario_username
    ON motoservices.tb_usuario (username);
