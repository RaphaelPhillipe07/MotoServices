package br.com.motoservices.exception;

/**
 * Conflito de estado/unicidade que deveria ser respondido como HTTP 409.
 */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String message) {
        super(message);
    }
}
