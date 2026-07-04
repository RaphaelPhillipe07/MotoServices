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

    @ExceptionHandler(EstoqueInsuficienteException.class)
    public ResponseEntity<Map<String, String>> handleEstoqueInsuficiente(EstoqueInsuficienteException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(ex));
    }

    private static Map<String, String> body(RuntimeException ex) {
        return Map.of("erro", ex.getMessage());
    }
}
