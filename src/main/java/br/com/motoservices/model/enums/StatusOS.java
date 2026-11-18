package br.com.motoservices.model.enums;

/**
 * Ciclo de vida de uma OS: ABERTA → EM_ANDAMENTO → AGUARDANDO_CLIENTE → CONCLUIDA → FECHADA,
 * com cancelamento permitido até AGUARDANDO_CLIENTE. Transições laterais entre EM_ANDAMENTO e
 * AGUARDANDO_CLIENTE são permitidas.
 */
public enum StatusOS {
    ABERTA,
    EM_ANDAMENTO,
    AGUARDANDO_CLIENTE,
    CONCLUIDA,
    FECHADA,
    CANCELADA;

    public boolean podeTransicionarPara(StatusOS destino) {
        if (destino == null || destino == this) {
            return false;
        }
        return switch (this) {
            case ABERTA -> destino == EM_ANDAMENTO || destino == CANCELADA;
            case EM_ANDAMENTO -> destino == AGUARDANDO_CLIENTE || destino == CONCLUIDA
                    || destino == CANCELADA;
            case AGUARDANDO_CLIENTE -> destino == EM_ANDAMENTO || destino == CONCLUIDA
                    || destino == CANCELADA;
            case CONCLUIDA -> destino == FECHADA;
            case FECHADA, CANCELADA -> false;
        };
    }

    public boolean permiteAdicionarServico() {
        return this == ABERTA || this == EM_ANDAMENTO || this == AGUARDANDO_CLIENTE;
    }

    public boolean permiteCancelamento() {
        return this == ABERTA || this == EM_ANDAMENTO || this == AGUARDANDO_CLIENTE;
    }

    /** Indica que a confirmação já ocorreu e o estoque das peças da loja foi baixado. */
    public boolean estoqueBaixado() {
        return this == EM_ANDAMENTO || this == AGUARDANDO_CLIENTE;
    }

    public boolean isEncerrada() {
        return this == FECHADA || this == CANCELADA;
    }
}
