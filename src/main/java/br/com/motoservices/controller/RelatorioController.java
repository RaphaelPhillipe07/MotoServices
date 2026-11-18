package br.com.motoservices.controller;

import br.com.motoservices.bo.RelatorioBO;
import br.com.motoservices.vo.ClienteRankingResponseVO;
import br.com.motoservices.vo.FaturamentoResponseVO;
import br.com.motoservices.vo.ServicoRankingResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Relatórios gerenciais de OS encerradas. O período é sempre por data de fechamento.
 */
@RestController
@RequestMapping("/relatorios")
@RequiredArgsConstructor
@Tag(name = "Relatórios", description = "Faturamento e rankings de OS encerradas")
public class RelatorioController {

    private final RelatorioBO relatorioBO;

    @Operation(summary = "Faturamento por período",
            description = "Totais de peças, mão de obra, desconto e ticket médio das OS FECHADAS.")
    @GetMapping("/faturamento")
    public ResponseEntity<FaturamentoResponseVO> faturamento(
            @Parameter(description = "Data inicial (inclusive)", example = "2026-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @Parameter(description = "Data final (inclusive)", example = "2026-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(relatorioBO.faturamento(inicio, fim));
    }

    @Operation(summary = "Serviços mais realizados",
            description = "Ranking por quantidade de serviços em OS FECHADAS; usa o valor base da mão de obra.")
    @GetMapping("/servicos-mais-realizados")
    public ResponseEntity<List<ServicoRankingResponseVO>> servicosMaisRealizados(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @Parameter(description = "Quantidade de itens (1 a 50)", example = "5")
            @RequestParam(defaultValue = "5") int limite) {
        return ResponseEntity.ok(relatorioBO.servicosMaisRealizados(inicio, fim, limite));
    }

    @Operation(summary = "Clientes com mais atendimentos",
            description = "Ranking por quantidade de OS FECHADAS e valor total no período.")
    @GetMapping("/clientes-mais-atendidos")
    public ResponseEntity<List<ClienteRankingResponseVO>> clientesMaisAtendidos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @Parameter(description = "Quantidade de itens (1 a 50)", example = "5")
            @RequestParam(defaultValue = "5") int limite) {
        return ResponseEntity.ok(relatorioBO.clientesMaisAtendidos(inicio, fim, limite));
    }

    @Operation(summary = "Exportar OS fechadas em CSV",
            description = "Arquivo separado por ponto e vírgula, com snapshot do orçamento e forma de pagamento.")
    @GetMapping(value = "/ordens/exportar", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> exportarOrdens(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        String csv = relatorioBO.exportarOrdensCsv(inicio, fim);
        String nome = "ordens-" + inicio + "-a-" + fim + ".csv";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename(nome).build());
        return ResponseEntity.ok().headers(headers).body(csv);
    }
}
