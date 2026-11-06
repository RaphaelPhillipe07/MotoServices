package br.com.motoservices.controller;

import br.com.motoservices.bo.MotoBO;
import br.com.motoservices.vo.MotoRequestVO;
import br.com.motoservices.vo.MotoResponseVO;
import br.com.motoservices.vo.PaginaResponseVO;
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
@RequestMapping("/motos")
@RequiredArgsConstructor
@Tag(name = "Motos", description = "Catálogo dinâmico de marcas/modelos/cilindradas")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Requisição inválida",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "500", description = "Erro interno",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)))
})
public class MotoController {

    private final MotoBO motoBO;

    @Operation(summary = "Cadastrar moto no catálogo",
            description = "Marca + modelo são únicos, sem diferenciar maiúsculas.")
    @ApiResponse(responseCode = "201", description = "Moto criada")
    @PostMapping
    public ResponseEntity<MotoResponseVO> cadastrar(@Valid @RequestBody MotoRequestVO request) {
        MotoResponseVO criado = motoBO.cadastrar(request);
        return ResponseEntity.created(URI.create("/motos/" + criado.getId())).body(criado);
    }

    @Operation(summary = "Buscar moto por id")
    @ApiResponse(responseCode = "404", description = "Moto não encontrada",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{motoId}")
    public ResponseEntity<MotoResponseVO> buscarPorId(@PathVariable Long motoId) {
        return ResponseEntity.ok(motoBO.buscarPorId(motoId));
    }

    @Operation(summary = "Filtrar motos por faixa de cilindradas")
    @GetMapping("/filtrar")
    public ResponseEntity<PaginaResponseVO<MotoResponseVO>> filtrarPorCilindradas(
            @RequestParam int minimo,
            @RequestParam int maximo,
            @PageableDefault(size = 20, sort = "cilindradas") Pageable pageable) {
        return ResponseEntity.ok(motoBO.filtrarPorCilindradas(minimo, maximo, pageable));
    }
}
