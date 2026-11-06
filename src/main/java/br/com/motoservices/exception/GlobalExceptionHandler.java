package br.com.motoservices.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrato de erro da API em RFC 9457 ({@code application/problem+json}).
 *
 * <p>Além dos campos padrão ({@code type}, {@code title}, {@code status}, {@code detail},
 * {@code instance}), erros de validação trazem {@code errors: [{field, message}]},
 * que a UI usa para destacar o input correspondente.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_TYPE = "urn:motoservices:problem:";

    // ---------- erros de domínio ----------

    @ExceptionHandler(MotoInvalidaException.class)
    public ProblemDetail handleMotoInvalida(MotoInvalidaException ex, HttpServletRequest request) {
        return naoEncontrado(ex, request);
    }

    @ExceptionHandler(ClienteNaoEncontradoException.class)
    public ProblemDetail handleClienteNaoEncontrado(ClienteNaoEncontradoException ex, HttpServletRequest request) {
        return naoEncontrado(ex, request);
    }

    @ExceptionHandler(PecaNaoEncontradaException.class)
    public ProblemDetail handlePecaNaoEncontrada(PecaNaoEncontradaException ex, HttpServletRequest request) {
        return naoEncontrado(ex, request);
    }

    @ExceptionHandler(OrdemDeServicoNaoEncontradaException.class)
    public ProblemDetail handleOsNaoEncontrada(OrdemDeServicoNaoEncontradaException ex, HttpServletRequest request) {
        return naoEncontrado(ex, request);
    }

    @ExceptionHandler(EstoqueInsuficienteException.class)
    public ProblemDetail handleEstoqueInsuficiente(EstoqueInsuficienteException ex, HttpServletRequest request) {
        return conflito(ex, request);
    }

    @ExceptionHandler(TransicaoStatusInvalidaException.class)
    public ProblemDetail handleTransicaoInvalida(TransicaoStatusInvalidaException ex, HttpServletRequest request) {
        return conflito(ex, request);
    }

    @ExceptionHandler(ValidacaoException.class)
    public ProblemDetail handleValidacao(ValidacaoException ex, HttpServletRequest request) {
        return problema(HttpStatus.BAD_REQUEST, "validacao", "Dados inválidos", ex.getMessage(), request);
    }

    // ---------- validação de entrada ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleCamposInvalidos(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "validacao", "Dados inválidos",
                "Um ou mais campos estão inválidos.", request);

        List<Map<String, String>> erros = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            Map<String, String> erro = new LinkedHashMap<>();
            erro.put("field", fieldError.getField());
            erro.put("message", fieldError.getDefaultMessage() == null
                    ? "valor inválido" : fieldError.getDefaultMessage());
            erros.add(erro);
        }
        problema.setProperty("errors", erros);
        return problema;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "validacao", "Dados inválidos",
                "Um ou mais parâmetros estão inválidos.", request);

        List<Map<String, String>> erros = new ArrayList<>();
        ex.getConstraintViolations().forEach(violacao -> {
            Map<String, String> erro = new LinkedHashMap<>();
            erro.put("field", violacao.getPropertyPath().toString());
            erro.put("message", violacao.getMessage());
            erros.add(erro);
        });
        problema.setProperty("errors", erros);
        return problema;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleJsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return problema(HttpStatus.BAD_REQUEST, "requisicao-invalida", "Requisição inválida",
                "Corpo da requisição inválido ou malformado.", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleParametroAusente(MissingServletRequestParameterException ex,
                                                HttpServletRequest request) {
        return problema(HttpStatus.BAD_REQUEST, "requisicao-invalida", "Requisição inválida",
                "Parâmetro obrigatório ausente: " + ex.getParameterName() + ".", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleParametroInvalido(MethodArgumentTypeMismatchException ex,
                                                 HttpServletRequest request) {
        return problema(HttpStatus.BAD_REQUEST, "requisicao-invalida", "Requisição inválida",
                "Parâmetro inválido: " + ex.getName() + ".", request);
    }

    // ---------- persistência e protocolo ----------

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegridade(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violação de integridade de dados: {}", ex.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "conflito", "Conflito de estado",
                "Operação viola uma restrição de integridade dos dados.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleRecursoInexistente(NoResourceFoundException ex, HttpServletRequest request) {
        return problema(HttpStatus.NOT_FOUND, "nao-encontrado", "Recurso não encontrado",
                "Recurso não encontrado.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex,
                                                  HttpServletRequest request) {
        return problema(HttpStatus.METHOD_NOT_ALLOWED, "metodo-nao-suportado", "Método não suportado",
                "Método HTTP não suportado para este recurso.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleMediaTypeNaoSuportado(HttpMediaTypeNotSupportedException ex,
                                                     HttpServletRequest request) {
        return problema(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "midia-nao-suportada", "Mídia não suportada",
                "Content-Type não suportado.", request);
    }

    // ---------- fallback ----------

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleInesperado(Exception ex, HttpServletRequest request) {
        log.error("Erro não tratado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "erro-interno", "Erro interno",
                "Erro interno inesperado.", request);
    }

    // ---------- helpers ----------

    private static ProblemDetail naoEncontrado(RuntimeException ex, HttpServletRequest request) {
        return problema(HttpStatus.NOT_FOUND, "nao-encontrado", "Recurso não encontrado",
                mensagem(ex), request);
    }

    private static ProblemDetail conflito(RuntimeException ex, HttpServletRequest request) {
        return problema(HttpStatus.CONFLICT, "conflito", "Conflito de estado",
                mensagem(ex), request);
    }

    private static ProblemDetail problema(HttpStatus status, String slug, String titulo,
                                          String detalhe, HttpServletRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setType(URI.create(BASE_TYPE + slug));
        problema.setTitle(titulo);
        if (request != null) {
            problema.setInstance(URI.create(request.getRequestURI()));
        }
        return problema;
    }

    private static String mensagem(RuntimeException ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }
}
