package br.com.motoservices.controller;

import br.com.motoservices.bo.ClienteBO;
import br.com.motoservices.vo.ClienteRequestVO;
import br.com.motoservices.vo.ClienteResponseVO;
import br.com.motoservices.vo.MotoResponseVO;
import br.com.motoservices.vo.PaginaResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Cadastro, busca e histórico de revisões")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Requisição inválida",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "500", description = "Erro interno",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)))
})
public class ClienteController {

    private final ClienteBO clienteBO;

    @Operation(summary = "Cadastrar cliente",
            description = "Valida CPF/CNPJ pelos dígitos verificadores e impede documento duplicado.")
    @ApiResponse(responseCode = "201", description = "Cliente criado")
    @PostMapping
    public ResponseEntity<ClienteResponseVO> cadastrar(@Valid @RequestBody ClienteRequestVO request) {
        ClienteResponseVO criado = clienteBO.cadastrar(request);
        return ResponseEntity.created(URI.create("/clientes/" + criado.getId())).body(criado);
    }

    @Operation(summary = "Buscar cliente por id")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{clienteId}")
    public ResponseEntity<ClienteResponseVO> buscarPorId(@PathVariable Long clienteId) {
        return ResponseEntity.ok(clienteBO.buscarPorId(clienteId));
    }

    @Operation(summary = "Buscar clientes",
            description = "Sem `nomeOuCpf`, lista todos os clientes. Com 11/14 dígitos, busca "
                    + "documento exato (no máximo um item); caso contrário, busca por nome (paginada).")
    @GetMapping
    public ResponseEntity<PaginaResponseVO<ClienteResponseVO>> buscar(
            @Parameter(description = "Nome ou CPF/CNPJ; opcional para listar todos")
            @RequestParam(required = false) String nomeOuCpf,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(clienteBO.buscar(nomeOuCpf, pageable));
    }

    @Operation(summary = "Listar próximas revisões",
            description = "Clientes com revisão agendada entre hoje e hoje + `dias`.")
    @GetMapping("/revisoes-proximas")
    public ResponseEntity<PaginaResponseVO<ClienteResponseVO>> listarProximasRevisoes(
            @Parameter(description = "Horizonte em dias (1 a 365)", example = "30")
            @RequestParam(defaultValue = "30") int dias,
            @PageableDefault(size = 20, sort = "proximaRevisao", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(clienteBO.listarProximasRevisoes(dias, pageable));
    }

    @Operation(summary = "Listar motos do cliente")
    @GetMapping("/{clienteId}/motos")
    public ResponseEntity<PaginaResponseVO<MotoResponseVO>> listarMotos(
            @PathVariable Long clienteId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(clienteBO.listarMotos(clienteId, pageable));
    }

    @Operation(summary = "Vincular moto ao cliente",
            description = "Idempotente; o vínculo também é criado automaticamente na primeira OS.")
    @ApiResponse(responseCode = "204", description = "Vínculo garantido")
    @PostMapping("/{clienteId}/motos/{motoId}")
    public ResponseEntity<Void> vincularMoto(@PathVariable Long clienteId,
                                             @PathVariable Long motoId) {
        clienteBO.vincularMoto(clienteId, motoId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Desvincular moto do cliente", description = "Idempotente.")
    @ApiResponse(responseCode = "204", description = "Vínculo removido")
    @DeleteMapping("/{clienteId}/motos/{motoId}")
    public ResponseEntity<Void> desvincularMoto(@PathVariable Long clienteId,
                                                @PathVariable Long motoId) {
        clienteBO.desvincularMoto(clienteId, motoId);
        return ResponseEntity.noContent().build();
    }
}
