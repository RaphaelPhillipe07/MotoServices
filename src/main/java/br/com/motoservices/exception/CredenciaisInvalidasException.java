package br.com.motoservices.exception;

/**
 * Credenciais inválidas no login. Não revela qual campo está errado.
 */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("Usuário ou senha inválidos.");
    }
}
