package br.com.motoservices.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MotoInvalidaException.class)
    public ResponseEntity<Map<String, String>> handleMotoInvalida(MotoInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(ex));
    }

    @ExceptionHandler(ClienteNaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleClienteNaoEncontrado(ClienteNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(ex));
    }

    @ExceptionHandler(PecaNaoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handlePecaNaoEncontrada(PecaNaoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(ex));
    }

    @ExceptionHandler(OrdemDeServicoNaoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleOsNaoEncontrada(OrdemDeServicoNaoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(ex));
    }

    @ExceptionHandler(EstoqueInsuficienteException.class)
    public ResponseEntity<Map<String, String>> handleEstoqueInsuficiente(EstoqueInsuficienteException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(ex));
    }

    @ExceptionHandler(TransicaoStatusInvalidaException.class)
    public ResponseEntity<Map<String, String>> handleTransicaoInvalida(TransicaoStatusInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(ex));
    }

    @ExceptionHandler(ValidacaoException.class)
    public ResponseEntity<Map<String, String>> handleValidacao(ValidacaoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(ex));
    }

    private static Map<String, String> body(RuntimeException ex) {
        String mensagem = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return Map.of("erro", mensagem);
    }
}
