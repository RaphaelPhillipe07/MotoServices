package br.com.motoservices.bo;

import br.com.motoservices.dao.OrdemDeServicoDAO;
import br.com.motoservices.model.OrdemDeServico;
import br.com.motoservices.vo.OrcamentoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrdemDeServicoBO {

    private final OrdemDeServicoDAO ordemDeServicoDAO;
    private final MotoBO motoBO;
    private final ClienteBO clienteBO;
    private final EstoqueBO estoqueBO;


    public OrdemDeServico criarOS(Long clienteId, String marcaMoto, String modeloMoto) {
        // TODO: chamar clienteBO.encontrarCliente e motoBO.encontrarMoto; montar e persistir a OS
        throw new UnsupportedOperationException("Implementar criação de OS.");
    }

    public void adicionarServico(Long osId /* , ServicoDaOficina servico */) {
        // TODO: carregar a OS, validar estoque se for troca com peça da loja, e adicionar
        throw new UnsupportedOperationException("Implementar adição de serviço à OS.");
    }

    public OrcamentoVO calcularOrcamento(Long osId) {
        // TODO: iterar servicos.calcularValorFinal(moto), totalizar peças/MO e aplicar desconto
        throw new UnsupportedOperationException("Implementar cálculo de orçamento.");
    }

    public Long confirmarOS(Long osId) {
        // TODO: @Transactional — salvar OS e chamar estoqueBO.baixarEstoque para cada peça
        throw new UnsupportedOperationException("Implementar confirmação de OS.");
    }

    public OrcamentoVO fecharOS(Long osId /* , FormaPagamento pagamento */) {
        // TODO: atualizar status, registrar pagamento, agendar revisão e devolver o VO
        throw new UnsupportedOperationException("Implementar fechamento de OS.");
    }
}
