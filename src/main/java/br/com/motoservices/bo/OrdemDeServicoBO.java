package br.com.motoservices.bo;

import br.com.motoservices.dao.OrdemDeServicoDAO;
import br.com.motoservices.exception.OrdemDeServicoNaoEncontradaException;
import br.com.motoservices.exception.ValidacaoException;
import br.com.motoservices.model.Cliente;
import br.com.motoservices.model.Moto;
import br.com.motoservices.model.OrdemDeServico;
import br.com.motoservices.model.Precificacao;
import br.com.motoservices.model.ServicoDaOficina;
import br.com.motoservices.model.ServicoTrocaPeca;
import br.com.motoservices.model.enums.StatusOS;
import br.com.motoservices.vo.OrcamentoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrdemDeServicoBO {

    /** Cada serviço de troca de peça consome uma unidade da peça da loja. */
    public static final int QUANTIDADE_POR_SERVICO = 1;

    private final OrdemDeServicoDAO ordemDeServicoDAO;
    private final MotoBO motoBO;
    private final ClienteBO clienteBO;
    private final EstoqueBO estoqueBO;

    @Transactional
    public OrdemDeServico criarOS(Long clienteId, String marcaMoto, String modeloMoto) {
        Cliente cliente = clienteBO.encontrarCliente(clienteId);
        Moto moto = motoBO.encontrarMoto(marcaMoto, modeloMoto);

        OrdemDeServico os = new OrdemDeServico();
        os.setCliente(cliente);
        os.setMoto(moto);
        os.setStatus(StatusOS.ABERTA);
        os.setDataAbertura(LocalDateTime.now());
        return ordemDeServicoDAO.save(os);
    }

    /**
     * Adiciona um serviço à OS. Para troca de peça fornecida pela loja, valida o
     * estoque antes de aceitar o serviço.
     */
    @Transactional
    public void adicionarServico(Long osId, ServicoDaOficina servico) {
        OrdemDeServico os = encontrarOS(osId);
        validarServico(servico);

        if (servico instanceof ServicoTrocaPeca troca && troca.usaPecaDaLoja()) {
            if (troca.getPecaEnvolvida() == null || troca.getPecaEnvolvida().getId() == null) {
                throw new ValidacaoException(
                        "Troca de peça fornecida pela loja exige uma peça cadastrada.");
            }
            estoqueBO.checarEstoque(troca.getPecaEnvolvida().getId(), QUANTIDADE_POR_SERVICO);
        }

        os.adicionarServico(servico);
        ordemDeServicoDAO.save(os);
    }

    @Transactional(readOnly = true)
    public OrcamentoVO calcularOrcamento(Long osId) {
        return calcularOrcamento(encontrarOS(osId));
    }

    /**
     * Confirma o orçamento: inicia o serviço e baixa o estoque das peças da loja.
     * A máquina de estados impede confirmação dupla (e baixa dupla).
     */
    @Transactional
    public Long confirmarOS(Long osId) {
        OrdemDeServico os = encontrarOS(osId);
        os.alterarStatus(StatusOS.EM_ANDAMENTO);

        for (ServicoDaOficina servico : os.getServicos()) {
            if (servico instanceof ServicoTrocaPeca troca && troca.usaPecaDaLoja()) {
                estoqueBO.baixarEstoque(troca.getPecaEnvolvida().getId(), QUANTIDADE_POR_SERVICO);
            }
        }

        ordemDeServicoDAO.save(os);
        return os.getId();
    }

    /**
     * Fecha a OS, registra a data de fechamento e agenda a próxima revisão do cliente.
     */
    @Transactional
    public OrcamentoVO fecharOS(Long osId) {
        OrdemDeServico os = encontrarOS(osId);
        OrcamentoVO orcamento = calcularOrcamento(os);

        if (os.getStatus() != StatusOS.CONCLUIDA) {
            os.alterarStatus(StatusOS.CONCLUIDA);
        }
        os.alterarStatus(StatusOS.FECHADA);

        LocalDate proximaRevisao = clienteBO.calcularProximaRevisao(LocalDate.now());
        clienteBO.agendarProximaRevisao(os.getCliente().getId(), proximaRevisao);

        ordemDeServicoDAO.save(os);
        return orcamento;
    }

    @Transactional(readOnly = true)
    public OrdemDeServico encontrarOS(Long osId) {
        if (osId == null) {
            throw new ValidacaoException("ID da ordem de serviço é obrigatório.");
        }
        return ordemDeServicoDAO.findById(osId)
                .orElseThrow(() -> new OrdemDeServicoNaoEncontradaException(
                        "Ordem de serviço não encontrada: " + osId));
    }

    private OrcamentoVO calcularOrcamento(OrdemDeServico os) {
        BigDecimal totalPecas = Precificacao.zero();
        BigDecimal totalMaoDeObra = Precificacao.zero();
        int pecasDaLoja = 0;

        for (ServicoDaOficina servico : os.getServicos()) {
            totalMaoDeObra = totalMaoDeObra.add(
                    Precificacao.maoDeObraComFator(servico.getValorBaseMaoDeObra(), os.getMoto()));
            if (servico instanceof ServicoTrocaPeca troca && troca.usaPecaDaLoja()) {
                totalPecas = totalPecas.add(troca.valorDaPecaDaLoja());
                pecasDaLoja++;
            }
        }

        totalPecas = Precificacao.arredondar(totalPecas);
        totalMaoDeObra = Precificacao.arredondar(totalMaoDeObra);
        BigDecimal desconto = Precificacao.descontoProgressivo(totalPecas, pecasDaLoja);
        BigDecimal totalFinal = Precificacao.arredondar(
                totalPecas.subtract(desconto).add(totalMaoDeObra));

        return new OrcamentoVO(totalPecas, totalMaoDeObra, desconto, totalFinal);
    }

    private static void validarServico(ServicoDaOficina servico) {
        if (servico == null) {
            throw new ValidacaoException("Serviço é obrigatório.");
        }
        if (servico.getDescricao() == null || servico.getDescricao().isBlank()) {
            throw new ValidacaoException("Descrição do serviço é obrigatória.");
        }
        BigDecimal valorBase = servico.getValorBaseMaoDeObra();
        if (valorBase == null || valorBase.signum() < 0) {
            throw new ValidacaoException("Valor base da mão de obra não pode ser negativo.");
        }
    }
}
