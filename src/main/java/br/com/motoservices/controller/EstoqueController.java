package br.com.motoservices.controller;

import br.com.motoservices.bo.EstoqueBO;
import br.com.motoservices.vo.PaginaResponseVO;
import br.com.motoservices.vo.PecaResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/estoque")
@RequiredArgsConstructor
@Tag(name = "Estoque", description = "Controle de peças e alertas de ruptura")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Requisição inválida",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "500", description = "Erro interno",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)))
})
public class EstoqueController {

    private final EstoqueBO estoqueBO;

    @Operation(summary = "Listar peças abaixo do estoque mínimo",
            description = "Compara a quantidade atual com o estoque mínimo de cada peça.")
    @GetMapping("/baixo")
    public ResponseEntity<PaginaResponseVO<PecaResponseVO>> listarBaixoEstoque(
            @PageableDefault(size = 20, sort = "codigo") Pageable pageable) {
        return ResponseEntity.ok(estoqueBO.listarBaixoEstoque(pageable));
    }
}
