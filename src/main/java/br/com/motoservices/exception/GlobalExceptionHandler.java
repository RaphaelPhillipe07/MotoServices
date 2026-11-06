package br.com.motoservices.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Contrato de erro da API. O formato interim é {@code {"erro": "...", "campos": {...}}};
 * a migração para RFC 9457 (ProblemDetail) acontece na Fase 2.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------- erros de domínio ----------

    @ExceptionHandler(MotoInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handleMotoInvalida(MotoInvalidaException ex) {
        return erro(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ClienteNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleClienteNaoEncontrado(ClienteNaoEncontradoException ex) {
        return erro(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(PecaNaoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handlePecaNaoEncontrada(PecaNaoEncontradaException ex) {
        return erro(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(OrdemDeServicoNaoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleOsNaoEncontrada(OrdemDeServicoNaoEncontradaException ex) {
        return erro(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(EstoqueInsuficienteException.class)
    public ResponseEntity<Map<String, Object>> handleEstoqueInsuficiente(EstoqueInsuficienteException ex) {
        return erro(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(TransicaoStatusInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handleTransicaoInvalida(TransicaoStatusInvalidaException ex) {
        return erro(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(ValidacaoException.class)
    public ResponseEntity<Map<String, Object>> handleValidacao(ValidacaoException ex) {
        return erro(HttpStatus.BAD_REQUEST, ex);
    }

    // ---------- validação de entrada ----------

    /**
     * Bean Validation em {@code @RequestBody}: devolve o mapa de campos inválidos,
     * que a UI usa para destacar o input correspondente.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleCamposInvalidos(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            campos.merge(fieldError.getField(), mensagemOuPadrao(fieldError), (atual, novo) -> atual + "; " + novo);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("erro", "Dados inválidos.");
        body.put("campos", campos);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violacao ->
                campos.put(violacao.getPropertyPath().toString(), violacao.getMessage()));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("erro", "Dados inválidos.");
        body.put("campos", campos);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonInvalido(HttpMessageNotReadableException ex) {
        return corpo(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleParametroAusente(MissingServletRequestParameterException ex) {
        return corpo(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: " + ex.getParameterName() + ".");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return corpo(HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + ex.getName() + ".");
    }

    // ---------- erros de persistência e protocolo ----------

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade de dados: {}", ex.getMostSpecificCause().getMessage());
        return corpo(HttpStatus.CONFLICT, "Operação viola uma restrição de integridade dos dados.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRecursoInexistente(NoResourceFoundException ex) {
        return corpo(HttpStatus.NOT_FOUND, "Recurso não encontrado.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex) {
        return corpo(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não suportado para este recurso.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMediaTypeNaoSuportado(HttpMediaTypeNotSupportedException ex) {
        return corpo(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type não suportado.");
    }

    // ---------- fallback ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleInesperado(Exception ex) {
        log.error("Erro não tratado", ex);
        return corpo(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado.");
    }

    private static ResponseEntity<Map<String, Object>> erro(HttpStatus status, RuntimeException ex) {
        return corpo(status, ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
    }

    private static ResponseEntity<Map<String, Object>> corpo(HttpStatus status, String mensagem) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("erro", mensagem);
        return ResponseEntity.status(status).body(body);
    }

    private static String mensagemOuPadrao(FieldError fieldError) {
        return fieldError.getDefaultMessage() == null
                ? "valor inválido"
                : fieldError.getDefaultMessage();
    }
}
