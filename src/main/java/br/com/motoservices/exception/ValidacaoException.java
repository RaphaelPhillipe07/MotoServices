package br.com.motoservices.exception;

/**
 * Entrada ou estado de domínio inválido (HTTP 400).
 */
public class ValidacaoException extends RuntimeException {

    public ValidacaoException(String message) {
        super(message);
    }
}
