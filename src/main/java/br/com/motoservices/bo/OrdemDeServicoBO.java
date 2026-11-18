package br.com.motoservices.bo;

import br.com.motoservices.dao.HistoricoStatusOSDAO;
import br.com.motoservices.dao.OrdemDeServicoDAO;
import br.com.motoservices.exception.ConflitoException;
import br.com.motoservices.exception.OrdemDeServicoNaoEncontradaException;
import br.com.motoservices.exception.ServicoNaoEncontradoException;
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
import br.com.motoservices.model.enums.FormaPagamento;
import br.com.motoservices.model.enums.StatusOS;
import br.com.motoservices.model.enums.TipoServico;
import br.com.motoservices.vo.HistoricoStatusResponseVO;
import br.com.motoservices.vo.OrcamentoResponseVO;
import br.com.motoservices.vo.OrdemDeServicoRequestVO;
import br.com.motoservices.vo.OrdemDeServicoResponseVO;
import br.com.motoservices.vo.PaginaResponseVO;
import br.com.motoservices.vo.ServicoRequestVO;
import br.com.motoservices.util.ComprovanteRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

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
        return listar(clienteId, null, null, pageable);
    }

    /**
     * Lista OS com filtros opcionais e combináveis por cliente, moto e status.
     */
    @Transactional(readOnly = true)
    public PaginaResponseVO<OrdemDeServicoResponseVO> listar(Long clienteId, Long motoId, StatusOS status,
                                                             Pageable pageable) {
        if (clienteId != null) {
            clienteBO.encontrarCliente(clienteId);
        }
        if (motoId != null) {
            motoBO.encontrarMotoPorId(motoId);
        }
        Pageable paginacao = pageable == null
                ? PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "dataAbertura"))
                : pageable;

        Page<OrdemDeServico> pagina = ordemDeServicoDAO.buscarComFiltros(clienteId, motoId, status, paginacao);
        return PaginaResponseVO.de(pagina, OrdemDeServicoMapper::toResponse);
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

    /**
     * Atualiza um serviço existente. O tipo (REVISAO/TROCA_PECA) não pode ser alterado.
     * Se a OS já foi confirmada, o estoque é ajustado (estorna o antigo e baixa o novo).
     */
    @Transactional
    public OrdemDeServicoResponseVO atualizarServico(Long osId, Long servicoId, ServicoRequestVO request) {
        OrdemDeServico os = encontrarOS(osId);
        if (!os.getStatus().permiteAdicionarServico()) {
            throw new ConflitoException("Não é possível alterar serviços de uma OS " + os.getStatus() + ".");
        }
        ServicoDaOficina existente = encontrarServico(os, servicoId);
        ServicoDaOficina dados = montarServico(request);

        if (!mesmoTipo(existente, dados)) {
            throw new ConflitoException("Não é possível alterar o tipo de um serviço existente.");
        }

        if (os.getStatus().estoqueBaixado()) {
            devolverEstoqueDaLoja(existente);
            baixarEstoqueDaLoja(dados);
        } else {
            checarEstoqueDaLoja(dados);
        }

        aplicarDados(existente, dados);
        ordemDeServicoDAO.save(os);
        return OrdemDeServicoMapper.toResponse(os);
    }

    /**
     * Remove um serviço. Se a OS já foi confirmada, devolve ao estoque a peça da loja.
     */
    @Transactional
    public OrdemDeServicoResponseVO removerServico(Long osId, Long servicoId) {
        OrdemDeServico os = encontrarOS(osId);
        if (!os.getStatus().permiteAdicionarServico()) {
            throw new ConflitoException("Não é possível remover serviços de uma OS " + os.getStatus() + ".");
        }
        ServicoDaOficina servico = encontrarServico(os, servicoId);
        if (os.getStatus().estoqueBaixado()) {
            devolverEstoqueDaLoja(servico);
        }
        os.removerServico(servico);
        ordemDeServicoDAO.save(os);
        return OrdemDeServicoMapper.toResponse(os);
    }

    private void adicionarServico(OrdemDeServico os, ServicoDaOficina servico) {
        validarServico(servico);
        checarEstoqueDaLoja(servico);
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

    private static boolean mesmoTipo(ServicoDaOficina existente, ServicoDaOficina dados) {
        return (existente instanceof ServicoTrocaPeca) == (dados instanceof ServicoTrocaPeca);
    }

    private static void aplicarDados(ServicoDaOficina destino, ServicoDaOficina dados) {
        destino.setDescricao(dados.getDescricao());
        destino.setTempoEstimado(dados.getTempoEstimado());
        destino.setValorBaseMaoDeObra(dados.getValorBaseMaoDeObra());
        if (destino instanceof ServicoRevisao revisao && dados instanceof ServicoRevisao revisaoDados) {
            revisao.setChecklistGeral(revisaoDados.getChecklistGeral());
        }
        if (destino instanceof ServicoTrocaPeca troca && dados instanceof ServicoTrocaPeca trocaDados) {
            troca.setPecaEnvolvida(trocaDados.getPecaEnvolvida());
            troca.setPecaFornecidaPelaLoja(trocaDados.getPecaFornecidaPelaLoja());
        }
    }

    private void checarEstoqueDaLoja(ServicoDaOficina servico) {
        if (usaPecaDaLoja(servico)) {
            estoqueBO.checarEstoque(pecaId(servico), QUANTIDADE_POR_SERVICO);
        }
    }

    private void baixarEstoqueDaLoja(ServicoDaOficina servico) {
        if (usaPecaDaLoja(servico)) {
            estoqueBO.baixarEstoque(pecaId(servico), QUANTIDADE_POR_SERVICO);
        }
    }

    private void devolverEstoqueDaLoja(ServicoDaOficina servico) {
        if (usaPecaDaLoja(servico)) {
            estoqueBO.devolverEstoque(pecaId(servico), QUANTIDADE_POR_SERVICO);
        }
    }

    private static boolean usaPecaDaLoja(ServicoDaOficina servico) {
        return servico instanceof ServicoTrocaPeca troca && troca.usaPecaDaLoja();
    }

    private static Long pecaId(ServicoDaOficina servico) {
        ServicoTrocaPeca troca = (ServicoTrocaPeca) servico;
        if (troca.getPecaEnvolvida() == null || troca.getPecaEnvolvida().getId() == null) {
            throw new ValidacaoException("Troca de peça fornecida pela loja exige uma peça cadastrada.");
        }
        return troca.getPecaEnvolvida().getId();
    }

    private ServicoDaOficina encontrarServico(OrdemDeServico os, Long servicoId) {
        if (servicoId == null) {
            throw new ValidacaoException("ID do serviço é obrigatório.");
        }
        return os.getServicos().stream()
                .filter(servico -> Objects.equals(servico.getId(), servicoId))
                .findFirst()
                .orElseThrow(() -> new ServicoNaoEncontradoException(
                        "Serviço não encontrado na OS " + os.getId() + ": " + servicoId));
    }

    // ---------- status, orçamento, confirmação, cancelamento e fechamento ----------

    /**
     * Move a OS entre os status operacionais. FECHADA e CANCELADA exigem os endpoints específicos.
     */
    @Transactional
    public OrdemDeServicoResponseVO alterarStatus(Long osId, StatusOS destino) {
        if (destino == null) {
            throw new ValidacaoException("Novo status é obrigatório.");
        }
        if (destino == StatusOS.FECHADA || destino == StatusOS.CANCELADA) {
            throw new ConflitoException(
                    "Use /fechar ou /cancelar para encerrar a ordem de serviço.");
        }
        OrdemDeServico os = encontrarOS(osId);
        StatusOS statusAnterior = os.getStatus();
        os.alterarStatus(destino);
        ordemDeServicoDAO.save(os);
        registrarHistorico(os, statusAnterior, destino);
        return OrdemDeServicoMapper.toResponse(os);
    }

    @Transactional(readOnly = true)
    public OrcamentoResponseVO calcularOrcamento(Long osId) {
        return calcularOrcamento(encontrarOS(osId));
    }

    /**
     * Confirma o orçamento: inicia o serviço e baixa o estoque das peças da loja.
     * A máquina de estados impede confirmação dupla (e baixa dupla). Uma OS sem
     * serviços não pode ser confirmada.
     */
    @Transactional
    public OrdemDeServicoResponseVO confirmarOS(Long osId) {
        OrdemDeServico os = encontrarOS(osId);
        if (os.getServicos().isEmpty()) {
            throw new ConflitoException("Não é possível confirmar uma OS sem serviços.");
        }
        StatusOS statusAnterior = os.getStatus();
        os.alterarStatus(StatusOS.EM_ANDAMENTO);
        for (ServicoDaOficina servico : os.getServicos()) {
            baixarEstoqueDaLoja(servico);
        }
        ordemDeServicoDAO.save(os);
        registrarHistorico(os, statusAnterior, StatusOS.EM_ANDAMENTO);
        return OrdemDeServicoMapper.toResponse(os);
    }

    /**
     * Cancela a OS até AGUARDANDO_CLIENTE. Se o estoque já foi baixado na confirmação,
     * as peças da loja são devolvidas automaticamente.
     */
    @Transactional
    public OrdemDeServicoResponseVO cancelarOS(Long osId) {
        OrdemDeServico os = encontrarOS(osId);
        if (!os.getStatus().permiteCancelamento()) {
            throw new ConflitoException("Não é possível cancelar uma OS com status " + os.getStatus() + ".");
        }
        StatusOS statusAnterior = os.getStatus();
        if (os.getStatus().estoqueBaixado()) {
            for (ServicoDaOficina servico : os.getServicos()) {
                devolverEstoqueDaLoja(servico);
            }
        }
        os.alterarStatus(StatusOS.CANCELADA);
        ordemDeServicoDAO.save(os);
        registrarHistorico(os, statusAnterior, StatusOS.CANCELADA);
        return OrdemDeServicoMapper.toResponse(os);
    }

    /**
     * Fecha a OS sem registrar a forma de pagamento.
     */
    @Transactional
    public OrcamentoResponseVO fecharOS(Long osId) {
        return fecharOS(osId, null);
    }

    /**
     * Fecha a OS, registra o pagamento, congela o orçamento e agenda a próxima revisão.
     */
    @Transactional
    public OrcamentoResponseVO fecharOS(Long osId, FormaPagamento formaPagamento) {
        OrdemDeServico os = encontrarOS(osId);
        if (os.getServicos().isEmpty()) {
            throw new ConflitoException("Não é possível fechar uma OS sem serviços.");
        }
        OrcamentoResponseVO orcamento = calcularOrcamento(os);

        os.setTotalPecas(orcamento.getTotalPecas());
        os.setTotalMaoDeObra(orcamento.getTotalMaoDeObra());
        os.setDescontoAplicado(orcamento.getDescontoAplicado());
        os.setTotalFinal(orcamento.getTotalFinal());
        if (formaPagamento != null) {
            os.setFormaPagamento(formaPagamento);
        }

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

    /**
     * Gera o comprovante interno imprimível (HTML) de uma OS fechada.
     */
    @Transactional(readOnly = true)
    public String gerarComprovante(Long osId) {
        OrdemDeServico os = encontrarOS(osId);
        if (os.getStatus() != StatusOS.FECHADA) {
            throw new ConflitoException(
                    "O comprovante só pode ser gerado para uma OS FECHADA.");
        }
        return ComprovanteRenderer.render(os);
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
