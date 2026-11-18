package br.com.motoservices.bo;

import br.com.motoservices.dao.OrdemDeServicoDAO;
import br.com.motoservices.exception.ValidacaoException;
import br.com.motoservices.model.Cliente;
import br.com.motoservices.model.Moto;
import br.com.motoservices.model.OrdemDeServico;
import br.com.motoservices.model.enums.FormaPagamento;
import br.com.motoservices.model.enums.StatusOS;
import br.com.motoservices.model.enums.TipoServico;
import br.com.motoservices.vo.ClienteRankingResponseVO;
import br.com.motoservices.vo.FaturamentoResponseVO;
import br.com.motoservices.vo.ServicoRankingResponseVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioBOTest {

    @Mock
    private OrdemDeServicoDAO ordemDeServicoDAO;

    @InjectMocks
    private RelatorioBO relatorioBO;

    @Test
    @DisplayName("faturamento soma o snapshot e calcula ticket médio")
    void faturamento() {
        OrdemDeServicoDAO.FaturamentoProjection projecao =
                mock(OrdemDeServicoDAO.FaturamentoProjection.class);
        when(projecao.getTotalPecas()).thenReturn(new BigDecimal("500.00"));
        when(projecao.getTotalMaoDeObra()).thenReturn(new BigDecimal("850.00"));
        when(projecao.getDescontoAplicado()).thenReturn(new BigDecimal("25.00"));
        when(projecao.getTotalFinal()).thenReturn(new BigDecimal("1325.00"));
        when(projecao.getQuantidadeOs()).thenReturn(2L);
        when(ordemDeServicoDAO.faturamentoPorPeriodo(eq(StatusOS.FECHADA),
                any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(projecao);

        FaturamentoResponseVO resultado = relatorioBO.faturamento(
                LocalDate.now().minusDays(10), LocalDate.now());

        assertEquals(new BigDecimal("1325.00"), resultado.getTotalFinal());
        assertEquals(new BigDecimal("662.50"), resultado.getTicketMedio());
        assertEquals(2, resultado.getQuantidadeOs());
    }

    @Test
    @DisplayName("período invertido é rejeitado")
    void periodoInvertido() {
        LocalDate hoje = LocalDate.now();

        assertThrows(ValidacaoException.class,
                () -> relatorioBO.faturamento(hoje, hoje.minusDays(1)));
    }

    @Test
    @DisplayName("ranking de serviços normaliza o tipo do subtipo JPA")
    void servicosMaisRealizados() {
        OrdemDeServicoDAO.ServicoRankingProjection projecao =
                mock(OrdemDeServicoDAO.ServicoRankingProjection.class);
        when(projecao.getDescricao()).thenReturn("Troca de pastilha");
        org.mockito.Mockito.doReturn(br.com.motoservices.model.ServicoTrocaPeca.class)
                .when(projecao).getTipo();
        when(projecao.getQuantidade()).thenReturn(3L);
        when(projecao.getTotalBaseMaoDeObra()).thenReturn(new BigDecimal("300.00"));
        when(ordemDeServicoDAO.servicosMaisRealizados(eq(StatusOS.FECHADA),
                any(LocalDateTime.class), any(LocalDateTime.class), any())).thenReturn(List.of(projecao));

        List<ServicoRankingResponseVO> resultado = relatorioBO.servicosMaisRealizados(
                LocalDate.now().minusDays(10), LocalDate.now(), 5);

        assertEquals(1, resultado.size());
        assertEquals(TipoServico.TROCA_PECA, resultado.get(0).getTipo());
        assertEquals(3, resultado.get(0).getQuantidade());
    }

    @Test
    @DisplayName("ranking de clientes mantém a ordem devolvida pelo DAO")
    void clientesMaisAtendidos() {
        OrdemDeServicoDAO.ClienteRankingProjection projecao =
                mock(OrdemDeServicoDAO.ClienteRankingProjection.class);
        when(projecao.getClienteId()).thenReturn(1L);
        when(projecao.getClienteNome()).thenReturn("Maria");
        when(projecao.getQuantidadeOs()).thenReturn(2L);
        when(projecao.getTotalFinal()).thenReturn(new BigDecimal("700.00"));
        when(ordemDeServicoDAO.clientesMaisAtendidos(eq(StatusOS.FECHADA),
                any(LocalDateTime.class), any(LocalDateTime.class), any())).thenReturn(List.of(projecao));

        List<ClienteRankingResponseVO> resultado = relatorioBO.clientesMaisAtendidos(
                LocalDate.now().minusDays(10), LocalDate.now(), 5);

        assertEquals(1, resultado.size());
        assertEquals("Maria", resultado.get(0).getClienteNome());
        assertEquals(new BigDecimal("700.00"), resultado.get(0).getTotalFinal());
    }

    @Test
    @DisplayName("CSV contém cabeçalho, totais e forma de pagamento")
    void exportacaoCsv() {
        OrdemDeServico os = new OrdemDeServico();
        os.setId(7L);
        os.setCliente(new Cliente(1L, "Maria", "52998224725", "119", null, null));
        os.setMoto(new Moto(1L, "Honda", "CG 160", 160));
        os.setStatus(StatusOS.FECHADA);
        os.setDataAbertura(LocalDateTime.now().minusDays(2));
        os.setDataFechamento(LocalDateTime.now().minusDays(1));
        os.setTotalPecas(new BigDecimal("100.00"));
        os.setTotalMaoDeObra(new BigDecimal("250.00"));
        os.setDescontoAplicado(BigDecimal.ZERO);
        os.setTotalFinal(new BigDecimal("350.00"));
        os.setFormaPagamento(FormaPagamento.PIX);
        when(ordemDeServicoDAO.buscarFechadasPorPeriodo(eq(StatusOS.FECHADA),
                any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(os));

        String csv = relatorioBO.exportarOrdensCsv(LocalDate.now().minusDays(3), LocalDate.now());

        assertTrue(csv.contains("id;data_abertura;data_fechamento"));
        assertTrue(csv.contains("Maria"));
        assertTrue(csv.contains("350.00"));
        assertTrue(csv.contains("PIX"));
    }

    @Test
    @DisplayName("limite de ranking fora de 1..50 é rejeitado")
    void limiteInvalido() {
        LocalDate hoje = LocalDate.now();

        assertThrows(ValidacaoException.class,
                () -> relatorioBO.servicosMaisRealizados(hoje, hoje, 0));
    }
}
