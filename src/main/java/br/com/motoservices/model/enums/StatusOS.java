package br.com.motoservices.model.enums;

/**
 * Ciclo de vida de uma OS: ABERTA → EM_ANDAMENTO → AGUARDANDO_CLIENTE → CONCLUIDA → FECHADA.
 * Transições laterais entre EM_ANDAMENTO e AGUARDANDO_CLIENTE são permitidas.
 */
public enum StatusOS {
    ABERTA,
    EM_ANDAMENTO,
    AGUARDANDO_CLIENTE,
    CONCLUIDA,
    FECHADA;

    public boolean podeTransicionarPara(StatusOS destino) {
        if (destino == null || destino == this) {
            return false;
        }
        return switch (this) {
            case ABERTA -> destino == EM_ANDAMENTO;
            case EM_ANDAMENTO -> destino == AGUARDANDO_CLIENTE || destino == CONCLUIDA;
            case AGUARDANDO_CLIENTE -> destino == EM_ANDAMENTO || destino == CONCLUIDA;
            case CONCLUIDA -> destino == FECHADA;
            case FECHADA -> false;
        };
    }

    public boolean permiteAdicionarServico() {
        return this == ABERTA || this == EM_ANDAMENTO || this == AGUARDANDO_CLIENTE;
    }

    public boolean isEncerrada() {
        return this == FECHADA;
    }
}
