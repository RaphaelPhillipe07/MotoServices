package br.com.motoservices.bo;

import br.com.motoservices.dao.HistoricoStatusOSDAO;
import br.com.motoservices.dao.OrdemDeServicoDAO;
import br.com.motoservices.exception.OrdemDeServicoNaoEncontradaException;
import br.com.motoservices.exception.ValidacaoException;
import br.com.motoservices.mapper.HistoricoStatusMapper;
import br.com.motoservices.mapper.OrdemDeServicoMapper;
import br.com.motoservices.model.Cliente;
import br.com.motoservices.model.HistoricoStatusOS;
import br.com.motoservices.model.Moto;
import br.com.motoservices.model.OrdemDeServico;
import br.com.motoservices.model.Peca;
import br.com.motoservices.model.Precificacao;
import br.com.motoservices.model.ServicoDaOficina;
import br.com.motoservices.model.ServicoRevisao;
import br.com.motoservices.model.ServicoTrocaPeca;
import br.com.motoservices.model.enums.StatusOS;
import br.com.motoservices.model.enums.TipoServico;
import br.com.motoservices.vo.OrcamentoResponseVO;
import br.com.motoservices.vo.OrdemDeServicoRequestVO;
import br.com.motoservices.vo.OrdemDeServicoResponseVO;
import br.com.motoservices.vo.HistoricoStatusResponseVO;
import br.com.motoservices.vo.PaginaResponseVO;
import br.com.motoservices.vo.ServicoRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final HistoricoStatusOSDAO historicoStatusOSDAO;
    private final MotoBO motoBO;
    private final ClienteBO clienteBO;
    private final EstoqueBO estoqueBO;

    // ---------- abertura e consulta ----------

    @Transactional
    public OrdemDeServico criarOS(Long clienteId, String marcaMoto, String modeloMoto) {
        Cliente cliente = clienteBO.encontrarCliente(clienteId);
        Moto moto = motoBO.encontrarMoto(marcaMoto, modeloMoto);
        clienteBO.vincularMotoSeAusente(cliente, moto);

        OrdemDeServico os = new OrdemDeServico();
        os.setCliente(cliente);
        os.setMoto(moto);
        os.setStatus(StatusOS.ABERTA);
        os.setDataAbertura(LocalDateTime.now());
        OrdemDeServico salva = ordemDeServicoDAO.save(os);
        registrarHistorico(salva, null, StatusOS.ABERTA);
        return salva;
    }

    @Transactional
    public OrdemDeServicoResponseVO criarOS(OrdemDeServicoRequestVO request) {
        if (request == null) {
            throw new ValidacaoException("Dados da ordem de serviço são obrigatórios.");
        }
        return OrdemDeServicoMapper.toResponse(
                criarOS(request.getClienteId(), request.getMarcaMoto(), request.getModeloMoto()));
    }

    @Transactional(readOnly = true)
    public OrdemDeServicoResponseVO buscarPorId(Long osId) {
        return OrdemDeServicoMapper.toResponse(encontrarOS(osId));
    }

    @Transactional(readOnly = true)
    public PaginaResponseVO<OrdemDeServicoResponseVO> listarPorCliente(Long clienteId, Pageable pageable) {
        clienteBO.encontrarCliente(clienteId);
        Pageable paginacao = pageable == null
                ? PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "dataAbertura"))
                : pageable;
        return PaginaResponseVO.de(
                ordemDeServicoDAO.findByClienteId(clienteId, paginacao),
                OrdemDeServicoMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PaginaResponseVO<HistoricoStatusResponseVO> listarHistorico(Long osId, Pageable pageable) {
        OrdemDeServico os = encontrarOS(osId);
        Pageable paginacao = pageable == null
                ? PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "dataHora"))
                : pageable;
        return PaginaResponseVO.de(
                historicoStatusOSDAO.findByOrdemDeServicoId(os.getId(), paginacao),
                HistoricoStatusMapper::toResponse);
    }

    // ---------- serviços ----------

    /**
     * Adiciona um serviço já montado à OS. Para troca de peça fornecida pela loja,
     * valida o estoque antes de aceitar o serviço.
     */
    @Transactional
    public void adicionarServico(Long osId, ServicoDaOficina servico) {
        OrdemDeServico os = encontrarOS(osId);
        adicionarServico(os, servico);
    }

    /**
     * Adiciona um serviço a partir do contrato de entrada da API.
     */
    @Transactional
    public OrdemDeServicoResponseVO adicionarServico(Long osId, ServicoRequestVO request) {
        OrdemDeServico os = encontrarOS(osId);
        adicionarServico(os, montarServico(request));
        return OrdemDeServicoMapper.toResponse(os);
    }

    private void adicionarServico(OrdemDeServico os, ServicoDaOficina servico) {
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

    private ServicoDaOficina montarServico(ServicoRequestVO request) {
        if (request == null) {
            throw new ValidacaoException("Serviço é obrigatório.");
        }
        if (request.getTipo() == null) {
            throw new ValidacaoException("Tipo do serviço é obrigatório.");
        }

        ServicoDaOficina servico;
        if (request.getTipo() == TipoServico.REVISAO) {
            servico = new ServicoRevisao(Boolean.TRUE.equals(request.getChecklistGeral()));
        } else {
            if (request.getPecaFornecidaPelaLoja() == null) {
                throw new ValidacaoException(
                        "pecaFornecidaPelaLoja é obrigatório para serviço de troca de peça.");
            }
            Peca peca = request.getPecaId() == null ? null : estoqueBO.encontrarPeca(request.getPecaId());
            if (Boolean.TRUE.equals(request.getPecaFornecidaPelaLoja()) && peca == null) {
                throw new ValidacaoException(
                        "pecaId é obrigatório quando a peça é fornecida pela loja.");
            }
            servico = new ServicoTrocaPeca(peca, request.getPecaFornecidaPelaLoja());
        }

        servico.setDescricao(request.getDescricao());
        servico.setTempoEstimado(request.getTempoEstimado());
        servico.setValorBaseMaoDeObra(request.getValorBaseMaoDeObra());
        return servico;
    }

    // ---------- orçamento, confirmação e fechamento ----------

    @Transactional(readOnly = true)
    public OrcamentoResponseVO calcularOrcamento(Long osId) {
        return calcularOrcamento(encontrarOS(osId));
    }

    /**
     * Confirma o orçamento: inicia o serviço e baixa o estoque das peças da loja.
     * A máquina de estados impede confirmação dupla (e baixa dupla).
     */
    @Transactional
    public OrdemDeServicoResponseVO confirmarOS(Long osId) {
        OrdemDeServico os = encontrarOS(osId);
        StatusOS statusAnterior = os.getStatus();
        os.alterarStatus(StatusOS.EM_ANDAMENTO);

        for (ServicoDaOficina servico : os.getServicos()) {
            if (servico instanceof ServicoTrocaPeca troca && troca.usaPecaDaLoja()) {
                estoqueBO.baixarEstoque(troca.getPecaEnvolvida().getId(), QUANTIDADE_POR_SERVICO);
            }
        }

        ordemDeServicoDAO.save(os);
        registrarHistorico(os, statusAnterior, StatusOS.EM_ANDAMENTO);
        return OrdemDeServicoMapper.toResponse(os);
    }

    /**
     * Fecha a OS, registra a data de fechamento e agenda a próxima revisão do cliente.
     */
    @Transactional
    public OrcamentoResponseVO fecharOS(Long osId) {
        OrdemDeServico os = encontrarOS(osId);
        OrcamentoResponseVO orcamento = calcularOrcamento(os);

        // Congela o orçamento: alterações posteriores em peças não mudam o valor cobrado.
        os.setTotalPecas(orcamento.getTotalPecas());
        os.setTotalMaoDeObra(orcamento.getTotalMaoDeObra());
        os.setDescontoAplicado(orcamento.getDescontoAplicado());
        os.setTotalFinal(orcamento.getTotalFinal());

        if (os.getStatus() != StatusOS.CONCLUIDA) {
            StatusOS statusAnterior = os.getStatus();
            os.alterarStatus(StatusOS.CONCLUIDA);
            registrarHistorico(os, statusAnterior, StatusOS.CONCLUIDA);
        }
        StatusOS statusAnteriorFechamento = os.getStatus();
        os.alterarStatus(StatusOS.FECHADA);
        registrarHistorico(os, statusAnteriorFechamento, StatusOS.FECHADA);

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

    private OrcamentoResponseVO calcularOrcamento(OrdemDeServico os) {
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

        return new OrcamentoResponseVO(totalPecas, totalMaoDeObra, desconto, totalFinal);
    }

    private void registrarHistorico(OrdemDeServico os, StatusOS statusAnterior, StatusOS statusNovo) {
        HistoricoStatusOS historico = new HistoricoStatusOS();
        historico.setOrdemDeServico(os);
        historico.setStatusAnterior(statusAnterior);
        historico.setStatusNovo(statusNovo);
        historico.setDataHora(LocalDateTime.now());
        historicoStatusOSDAO.save(historico);
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
