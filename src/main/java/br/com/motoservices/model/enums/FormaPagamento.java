package br.com.motoservices.model.enums;

/**
 * Forma de pagamento registrada no fechamento da OS. Opcional: OS antigas e fechamentos
 * sem informação permanecem com {@code null}.
 */
public enum FormaPagamento {
    DINHEIRO,
    PIX,
    CARTAO_CREDITO,
    CARTAO_DEBITO,
    BOLETO,
    OUTRO
}
