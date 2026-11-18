package br.com.motoservices.bo;

import br.com.motoservices.dao.OrdemDeServicoDAO;
import br.com.motoservices.exception.ValidacaoException;
import br.com.motoservices.model.OrdemDeServico;
import br.com.motoservices.model.ServicoTrocaPeca;
import br.com.motoservices.model.enums.StatusOS;
import br.com.motoservices.model.enums.TipoServico;
import br.com.motoservices.vo.ClienteRankingResponseVO;
import br.com.motoservices.vo.FaturamentoResponseVO;
import br.com.motoservices.vo.ServicoRankingResponseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Consultas gerenciais calculadas exclusivamente sobre OS FECHADAS e o snapshot do orçamento.
 * O período é interpretado pela data de fechamento (competência do faturamento).
 */
@Service
@RequiredArgsConstructor
public class RelatorioBO {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ISO_LOCAL_DATE;

    private final OrdemDeServicoDAO ordemDeServicoDAO;

    @Transactional(readOnly = true)
    public FaturamentoResponseVO faturamento(LocalDate inicio, LocalDate fim) {
        Periodo periodo = periodo(inicio, fim);
        OrdemDeServicoDAO.FaturamentoProjection projecao = ordemDeServicoDAO.faturamentoPorPeriodo(
                StatusOS.FECHADA, periodo.inicio(), periodo.fim());

        long quantidade = projecao == null || projecao.getQuantidadeOs() == null
                ? 0L : projecao.getQuantidadeOs();
        BigDecimal totalPecas = valor(projecao == null ? null : projecao.getTotalPecas());
        BigDecimal totalMaoDeObra = valor(projecao == null ? null : projecao.getTotalMaoDeObra());
        BigDecimal desconto = valor(projecao == null ? null : projecao.getDescontoAplicado());
        BigDecimal totalFinal = valor(projecao == null ? null : projecao.getTotalFinal());
        BigDecimal ticketMedio = quantidade == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : totalFinal.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);

        return new FaturamentoResponseVO(inicio, fim, totalPecas, totalMaoDeObra,
                desconto, totalFinal, ticketMedio, quantidade);
    }

    @Transactional(readOnly = true)
    public List<ServicoRankingResponseVO> servicosMaisRealizados(LocalDate inicio, LocalDate fim, int limite) {
        Periodo periodo = periodo(inicio, fim);
        int tamanho = limite(limite);
        return ordemDeServicoDAO.servicosMaisRealizados(
                        StatusOS.FECHADA, periodo.inicio(), periodo.fim(), PageRequest.of(0, tamanho))
                .stream()
                .map(projecao -> new ServicoRankingResponseVO(
                        projecao.getDescricao(),
                        tipo(projecao.getTipo()),
                        projecao.getQuantidade() == null ? 0L : projecao.getQuantidade(),
                        valor(projecao.getTotalBaseMaoDeObra())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClienteRankingResponseVO> clientesMaisAtendidos(LocalDate inicio, LocalDate fim, int limite) {
        Periodo periodo = periodo(inicio, fim);
        int tamanho = limite(limite);
        return ordemDeServicoDAO.clientesMaisAtendidos(
                        StatusOS.FECHADA, periodo.inicio(), periodo.fim(), PageRequest.of(0, tamanho))
                .stream()
                .map(projecao -> new ClienteRankingResponseVO(
                        projecao.getClienteId(),
                        projecao.getClienteNome(),
                        projecao.getQuantidadeOs() == null ? 0L : projecao.getQuantidadeOs(),
                        valor(projecao.getTotalFinal())))
                .toList();
    }

    @Transactional(readOnly = true)
    public String exportarOrdensCsv(LocalDate inicio, LocalDate fim) {
        Periodo periodo = periodo(inicio, fim);
        List<OrdemDeServico> ordens = ordemDeServicoDAO.buscarFechadasPorPeriodo(
                StatusOS.FECHADA, periodo.inicio(), periodo.fim());

        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("id;data_abertura;data_fechamento;cliente_id;cliente;moto;status;")
                .append("total_pecas;total_mao_de_obra;desconto;total_final;forma_pagamento\r\n");
        for (OrdemDeServico os : ordens) {
            csv.append(os.getId()).append(';')
                    .append(os.getDataAbertura() == null ? "" : os.getDataAbertura().format(DATA_HORA)).append(';')
                    .append(os.getDataFechamento() == null ? "" : os.getDataFechamento().format(DATA_HORA)).append(';')
                    .append(os.getCliente() == null ? "" : os.getCliente().getId()).append(';')
                    .append(celula(os.getCliente() == null ? null : os.getCliente().getNome())).append(';')
                    .append(celula(os.getMoto() == null ? null
                            : os.getMoto().getMarca() + " " + os.getMoto().getModelo())).append(';')
                    .append(os.getStatus()).append(';')
                    .append(valor(os.getTotalPecas()).toPlainString()).append(';')
                    .append(valor(os.getTotalMaoDeObra()).toPlainString()).append(';')
                    .append(valor(os.getDescontoAplicado()).toPlainString()).append(';')
                    .append(valor(os.getTotalFinal()).toPlainString()).append(';')
                    .append(os.getFormaPagamento() == null ? "" : os.getFormaPagamento()).append("\r\n");
        }
        return csv.toString();
    }

    private static Periodo periodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new ValidacaoException("As datas de início e fim são obrigatórias.");
        }
        if (inicio.isAfter(fim)) {
            throw new ValidacaoException("A data de início não pode ser posterior à data de fim.");
        }
        return new Periodo(inicio.atStartOfDay(), fim.atTime(LocalTime.MAX));
    }

    private static int limite(int limite) {
        if (limite < 1 || limite > 50) {
            throw new ValidacaoException("O limite deve estar entre 1 e 50.");
        }
        return limite;
    }

    private static BigDecimal valor(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static TipoServico tipo(Class<?> tipo) {
        return tipo != null && ServicoTrocaPeca.class.isAssignableFrom(tipo)
                ? TipoServico.TROCA_PECA : TipoServico.REVISAO;
    }

    private static String celula(String valor) {
        if (valor == null) {
            return "";
        }
        String texto = valor.replace("\"", "\"\"");
        return texto.contains(";") || texto.contains("\"") || texto.contains("\n") || texto.contains("\r")
                ? "\"" + texto + "\""
                : texto;
    }

    private record Periodo(LocalDateTime inicio, LocalDateTime fim) {
    }
}
