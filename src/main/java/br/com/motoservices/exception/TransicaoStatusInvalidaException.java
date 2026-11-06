package br.com.motoservices.exception;

/**
 * Transição de status não permitida pelo ciclo de vida da OS (HTTP 409).
 */
public class TransicaoStatusInvalidaException extends RuntimeException {

    public TransicaoStatusInvalidaException(String message) {
        super(message);
    }
}
