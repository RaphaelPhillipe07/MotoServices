package br.com.motoservices.exception;

public class ServicoNaoEncontradoException extends RuntimeException {

    public ServicoNaoEncontradoException(String message) {
        super(message);
    }
}
