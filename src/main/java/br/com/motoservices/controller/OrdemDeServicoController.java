package br.com.motoservices.controller;

import br.com.motoservices.bo.OrdemDeServicoBO;
import br.com.motoservices.model.enums.StatusOS;
import br.com.motoservices.vo.FechamentoRequestVO;
import br.com.motoservices.vo.HistoricoStatusResponseVO;
import br.com.motoservices.vo.OrcamentoResponseVO;
import br.com.motoservices.vo.OrdemDeServicoRequestVO;
import br.com.motoservices.vo.OrdemDeServicoResponseVO;
import br.com.motoservices.vo.PaginaResponseVO;
import br.com.motoservices.vo.ServicoRequestVO;
import br.com.motoservices.vo.StatusRequestVO;
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
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/ordens-servico")
@RequiredArgsConstructor
@Tag(name = "Ordens de Serviço", description = "Ciclo de vida de uma OS — abertura ao fechamento")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Requisição inválida",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "500", description = "Erro interno",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)))
})
public class OrdemDeServicoController {

    private final OrdemDeServicoBO ordemDeServicoBO;

    @Operation(summary = "Abrir ordem de serviço",
            description = "Valida cliente e moto no catálogo e abre a OS com status ABERTA.")
    @ApiResponse(responseCode = "201", description = "OS criada")
    @PostMapping
    public ResponseEntity<OrdemDeServicoResponseVO> criar(@Valid @RequestBody OrdemDeServicoRequestVO request) {
        OrdemDeServicoResponseVO criada = ordemDeServicoBO.criarOS(request);
        return ResponseEntity.created(URI.create("/ordens-servico/" + criada.getId())).body(criada);
    }

    @Operation(summary = "Buscar OS por id")
    @ApiResponse(responseCode = "404", description = "OS não encontrada",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{osId}")
    public ResponseEntity<OrdemDeServicoResponseVO> buscarPorId(@PathVariable Long osId) {
        return ResponseEntity.ok(ordemDeServicoBO.buscarPorId(osId));
    }

    @Operation(summary = "Listar ordens de serviço",
            description = "Filtros opcionais e combináveis por cliente, moto e status; "
                    + "ordenado por data de abertura (desc).")
    @GetMapping
    public ResponseEntity<PaginaResponseVO<OrdemDeServicoResponseVO>> listar(
            @RequestParam(required = false) Long clienteId,
            @Parameter(description = "Filtra as OS de uma moto específica")
            @RequestParam(required = false) Long motoId,
            @RequestParam(required = false) StatusOS status,
            @PageableDefault(size = 20, sort = "dataAbertura", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ordemDeServicoBO.listar(clienteId, motoId, status, pageable));
    }

    @Operation(summary = "Histórico de status da OS",
            description = "Transições registradas para a OS, das mais recentes para as mais antigas.")
    @ApiResponse(responseCode = "404", description = "OS não encontrada",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{osId}/historico")
    public ResponseEntity<PaginaResponseVO<HistoricoStatusResponseVO>> listarHistorico(
            @PathVariable Long osId,
            @PageableDefault(size = 20, sort = "dataHora", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ordemDeServicoBO.listarHistorico(osId, pageable));
    }

    @Operation(summary = "Adicionar serviço à OS",
            description = "Aceita REVISAO ou TROCA_PECA. Peça da loja exige pecaId e estoque disponível.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "OS ou peça não encontrada",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Estoque insuficiente",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{osId}/servicos")
    public ResponseEntity<OrdemDeServicoResponseVO> adicionarServico(
            @PathVariable Long osId,
            @Valid @RequestBody ServicoRequestVO request) {
        return ResponseEntity.ok(ordemDeServicoBO.adicionarServico(osId, request));
    }

    @Operation(summary = "Alterar status operacional da OS",
            description = "Permite transições entre ABERTA, EM_ANDAMENTO, AGUARDANDO_CLIENTE e "
                    + "CONCLUIDA. Para encerrar use /fechar ou /cancelar.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "OS não encontrada",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Transição de status inválida",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{osId}/status")
    public ResponseEntity<OrdemDeServicoResponseVO> alterarStatus(
            @PathVariable Long osId,
            @Valid @RequestBody StatusRequestVO request) {
        return ResponseEntity.ok(ordemDeServicoBO.alterarStatus(osId, request.getStatus()));
    }

    @Operation(summary = "Cancelar OS",
            description = "Permite cancelar até AGUARDANDO_CLIENTE; se o estoque já foi baixado, "
                    + "as peças da loja são devolvidas automaticamente.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "OS não encontrada",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Status não permite cancelamento",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{osId}/cancelar")
    public ResponseEntity<OrdemDeServicoResponseVO> cancelarOS(@PathVariable Long osId) {
        return ResponseEntity.ok(ordemDeServicoBO.cancelarOS(osId));
    }

    @Operation(summary = "Atualizar serviço da OS",
            description = "Mantém o tipo do serviço. Se a OS já foi confirmada, ajusta o estoque "
                    + "(estorna a peça antiga e baixa a nova).")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "OS, serviço ou peça não encontrada",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "OS encerrada, tipo alterado ou estoque insuficiente",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{osId}/servicos/{servicoId}")
    public ResponseEntity<OrdemDeServicoResponseVO> atualizarServico(
            @PathVariable Long osId,
            @PathVariable Long servicoId,
            @Valid @RequestBody ServicoRequestVO request) {
        return ResponseEntity.ok(ordemDeServicoBO.atualizarServico(osId, servicoId, request));
    }

    @Operation(summary = "Remover serviço da OS",
            description = "Se a OS já foi confirmada, devolve ao estoque a peça da loja.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "OS ou serviço não encontrado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "OS encerrada",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{osId}/servicos/{servicoId}")
    public ResponseEntity<OrdemDeServicoResponseVO> removerServico(
            @PathVariable Long osId,
            @PathVariable Long servicoId) {
        return ResponseEntity.ok(ordemDeServicoBO.removerServico(osId, servicoId));
    }

    @Operation(summary = "Calcular orçamento da OS",
            description = "Soma peças da loja, aplica fator de risco por cilindrada e desconto de 5%.")
    @ApiResponse(responseCode = "404", description = "OS não encontrada",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{osId}/orcamento")
    public ResponseEntity<OrcamentoResponseVO> calcularOrcamento(@PathVariable Long osId) {
        return ResponseEntity.ok(ordemDeServicoBO.calcularOrcamento(osId));
    }

    @Operation(summary = "Confirmar orçamento",
            description = "Inicia a OS e baixa o estoque das peças da loja uma única vez.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "OS não encontrada",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Transição de status inválida ou estoque insuficiente",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{osId}/confirmar")
    public ResponseEntity<OrdemDeServicoResponseVO> confirmarOS(@PathVariable Long osId) {
        return ResponseEntity.ok(ordemDeServicoBO.confirmarOS(osId));
    }

    @Operation(summary = "Gerar comprovante interno da OS",
            description = "HTML imprimível com cliente, moto, serviços, totais e forma de pagamento. "
                    + "Sem validade fiscal; disponível apenas para OS FECHADA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comprovante HTML"),
            @ApiResponse(responseCode = "409", description = "OS ainda não foi fechada",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(value = "/{osId}/comprovante", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> comprovante(@PathVariable Long osId) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(ordemDeServicoBO.gerarComprovante(osId));
    }

    @Operation(summary = "Fechar OS",
            description = "Conclui e encerra a OS, registra a forma de pagamento (opcional), "
                    + "agenda a próxima revisão e devolve o orçamento final.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "OS não encontrada",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Transição inválida ou OS sem serviços",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{osId}/fechar")
    public ResponseEntity<OrcamentoResponseVO> fecharOS(
            @PathVariable Long osId,
            @RequestBody(required = false) FechamentoRequestVO request) {
        return ResponseEntity.ok(ordemDeServicoBO.fecharOS(
                osId, request == null ? null : request.getFormaPagamento()));
    }
}
