package br.com.motoservices.controller;

import br.com.motoservices.bo.OrdemDeServicoBO;
import br.com.motoservices.vo.OrcamentoVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ordens-servico")
@RequiredArgsConstructor
@Tag(name = "Ordens de Serviço", description = "Ciclo de vida de uma OS — abertura ao fechamento")
public class OrdemDeServicoController {

    private final OrdemDeServicoBO ordemDeServicoBO;

    @PostMapping
    public ResponseEntity<Long> criarOS(@RequestParam Long clienteId,
                                        @RequestParam String marcaMoto,
                                        @RequestParam String modeloMoto) {
        Long osId = ordemDeServicoBO.criarOS(clienteId, marcaMoto, modeloMoto).getId();
        return ResponseEntity.ok(osId);
    }

    @GetMapping("/{osId}/orcamento")
    public ResponseEntity<OrcamentoVO> calcularOrcamento(@PathVariable Long osId) {
        return ResponseEntity.ok(ordemDeServicoBO.calcularOrcamento(osId));
    }

    @PostMapping("/{osId}/confirmar")
    public ResponseEntity<Long> confirmarOS(@PathVariable Long osId) {
        return ResponseEntity.ok(ordemDeServicoBO.confirmarOS(osId));
    }

    @PostMapping("/{osId}/fechar")
    public ResponseEntity<OrcamentoVO> fecharOS(@PathVariable Long osId /* ,
                                                 @RequestParam FormaPagamento pagamento */) {
        return ResponseEntity.ok(ordemDeServicoBO.fecharOS(osId));
    }
}
