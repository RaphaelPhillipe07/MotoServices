package br.com.motoservices.controller;

import br.com.motoservices.bo.EstoqueBO;
import br.com.motoservices.vo.EntradaEstoqueRequestVO;
import br.com.motoservices.vo.PaginaResponseVO;
import br.com.motoservices.vo.PecaRequestVO;
import br.com.motoservices.vo.PecaResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/pecas")
@RequiredArgsConstructor
@Tag(name = "Peças", description = "Cadastro de peças e movimentação de estoque")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Requisição inválida",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "500", description = "Erro interno",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)))
})
public class PecaController {

    private final EstoqueBO estoqueBO;

    @Operation(summary = "Cadastrar peça", description = "O código da peça é único.")
    @ApiResponse(responseCode = "201", description = "Peça criada")
    @PostMapping
    public ResponseEntity<PecaResponseVO> cadastrar(@Valid @RequestBody PecaRequestVO request) {
        PecaResponseVO criada = estoqueBO.cadastrar(request);
        return ResponseEntity.created(URI.create("/pecas/" + criada.getId())).body(criada);
    }

    @Operation(summary = "Buscar peça por id")
    @ApiResponse(responseCode = "404", description = "Peça não encontrada",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{pecaId}")
    public ResponseEntity<PecaResponseVO> buscarPorId(@PathVariable Long pecaId) {
        return ResponseEntity.ok(estoqueBO.buscarPorId(pecaId));
    }

    @Operation(summary = "Listar peças", description = "Busca opcional por código ou descrição.")
    @GetMapping
    public ResponseEntity<PaginaResponseVO<PecaResponseVO>> listar(
            @RequestParam(required = false) String busca,
            @PageableDefault(size = 20, sort = "codigo") Pageable pageable) {
        return ResponseEntity.ok(estoqueBO.listar(busca, pageable));
    }

    @Operation(summary = "Registrar entrada de estoque",
            description = "Soma a quantidade recebida ao saldo atual da peça.")
    @ApiResponse(responseCode = "404", description = "Peça não encontrada",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{pecaId}/entrada")
    public ResponseEntity<PecaResponseVO> registrarEntrada(
            @PathVariable Long pecaId,
            @Valid @RequestBody EntradaEstoqueRequestVO request) {
        return ResponseEntity.ok(estoqueBO.registrarEntrada(pecaId, request.getQuantidade()));
    }
}
