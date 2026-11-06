package br.com.motoservices.bo;

import br.com.motoservices.dao.OrdemDeServicoDAO;
import br.com.motoservices.exception.ClienteNaoEncontradoException;
import br.com.motoservices.exception.EstoqueInsuficienteException;
import br.com.motoservices.exception.MotoInvalidaException;
import br.com.motoservices.exception.OrdemDeServicoNaoEncontradaException;
import br.com.motoservices.exception.TransicaoStatusInvalidaException;
import br.com.motoservices.exception.ValidacaoException;
import br.com.motoservices.model.Cliente;
import br.com.motoservices.model.Moto;
import br.com.motoservices.model.OrdemDeServico;
import br.com.motoservices.model.Peca;
import br.com.motoservices.model.ServicoDaOficina;
import br.com.motoservices.model.ServicoRevisao;
import br.com.motoservices.model.ServicoTrocaPeca;
import br.com.motoservices.model.enums.StatusOS;
import br.com.motoservices.vo.OrcamentoVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdemDeServicoBOTest {

    private static final long OS_ID = 1L;
    private static final long CLIENTE_ID = 1L;

    @Mock
    private OrdemDeServicoDAO ordemDeServicoDAO;
    @Mock
    private MotoBO motoBO;
    @Mock
    private ClienteBO clienteBO;
    @Mock
    private EstoqueBO estoqueBO;

    @InjectMocks
    private OrdemDeServicoBO ordemDeServicoBO;

    // ---------- helpers ----------

    private Cliente cliente() {
        return new Cliente(CLIENTE_ID, "Maria", "52998224725", "11999999999", null);
    }

    private Moto moto(int cilindradas) {
        return new Moto(1L, "Honda", "CG", cilindradas);
    }

    private OrdemDeServico os(StatusOS status, int cilindradas, ServicoDaOficina... servicos) {
        OrdemDeServico os = new OrdemDeServico();
        os.setId(OS_ID);
        os.setCliente(cliente());
        os.setMoto(moto(cilindradas));
        os.setStatus(StatusOS.ABERTA);
        os.setDataAbertura(LocalDateTime.now());
        for (ServicoDaOficina servico : servicos) {
            os.adicionarServico(servico);
        }
        os.setStatus(status);
        return os;
    }

    private ServicoRevisao revisao(String valorBase) {
        ServicoRevisao revisao = new ServicoRevisao(true);
        revisao.setDescricao("Revisão");
        revisao.setValorBaseMaoDeObra(new BigDecimal(valorBase));
        return revisao;
    }

    private ServicoTrocaPeca troca(long pecaId, String valorPeca, boolean daLoja) {
        Peca peca = new Peca(pecaId, "P-00" + pecaId, "Peça", new BigDecimal(valorPeca), 10, 2);
        ServicoTrocaPeca troca = new ServicoTrocaPeca(peca, daLoja);
        troca.setDescricao("Troca de peça");
        troca.setValorBaseMaoDeObra(new BigDecimal("100.00"));
        return troca;
    }

    private ServicoDaOficina[] pecasDaLoja(int quantidade, String valorPeca) {
        ServicoDaOficina[] servicos = new ServicoDaOficina[quantidade];
        for (int i = 0; i < quantidade; i++) {
            servicos[i] = troca(i + 1, valorPeca, true);
        }
        return servicos;
    }

    private void stubOS(OrdemDeServico os) {
        when(ordemDeServicoDAO.findById(OS_ID)).thenReturn(Optional.of(os));
    }

    // ---------- orçamento ----------

    @Test
    @DisplayName("desconto de 5% ativo com 5 peças da loja")
    void testeDescontoAplicado() {
        OrdemDeServico os = os(StatusOS.ABERTA, 300, pecasDaLoja(5, "100.00"));
        stubOS(os);

        OrcamentoVO orcamento = ordemDeServicoBO.calcularOrcamento(OS_ID);

        assertEquals(new BigDecimal("500.00"), orcamento.getTotalPecas());
        assertEquals(new BigDecimal("500.00"), orcamento.getTotalMaoDeObra());
        assertEquals(new BigDecimal("25.00"), orcamento.getDescontoAplicado());
        assertEquals(new BigDecimal("975.00"), orcamento.getTotalFinal());
    }

    @Test
    @DisplayName("sem desconto com menos de 5 peças da loja")
    void testeSemDesconto() {
        OrdemDeServico os = os(StatusOS.ABERTA, 300, pecasDaLoja(4, "100.00"));
        stubOS(os);

        OrcamentoVO orcamento = ordemDeServicoBO.calcularOrcamento(OS_ID);

        assertEquals(new BigDecimal("400.00"), orcamento.getTotalPecas());
        assertEquals(new BigDecimal("0.00"), orcamento.getDescontoAplicado());
        assertEquals(new BigDecimal("800.00"), orcamento.getTotalFinal());
    }

    @ParameterizedTest(name = "{0} cc → mão de obra {1}")
    @CsvSource({"400, 100.00", "500, 150.00", "700, 170.00"})
    @DisplayName("cilindradas acima de 400cc geram +50% e acima de 650cc geram +70% na mão de obra")
    void testeMotoMaoDeObraExtra(int cilindradas, String maoDeObraEsperada) {
        OrdemDeServico os = os(StatusOS.ABERTA, cilindradas, revisao("100.00"));
        stubOS(os);

        OrcamentoVO orcamento = ordemDeServicoBO.calcularOrcamento(OS_ID);

        assertEquals(new BigDecimal(maoDeObraEsperada), orcamento.getTotalMaoDeObra());
        assertEquals(new BigDecimal(maoDeObraEsperada), orcamento.getTotalFinal());
    }

    @Test
    @DisplayName("peça fornecida pelo cliente cobra apenas a mão de obra")
    void testePecaAvulsa() {
        OrdemDeServico os = os(StatusOS.ABERTA, 300, troca(1, "80.00", false));
        stubOS(os);

        OrcamentoVO orcamento = ordemDeServicoBO.calcularOrcamento(OS_ID);

        assertEquals(new BigDecimal("0.00"), orcamento.getTotalPecas());
        assertEquals(new BigDecimal("100.00"), orcamento.getTotalMaoDeObra());
        assertEquals(new BigDecimal("100.00"), orcamento.getTotalFinal());
    }

    @Test
    @DisplayName("totais do OrcamentoVO são consistentes com as regras")
    void testeTotaisDoOrcamentoVOEstaoCorretos() {
        ServicoDaOficina[] servicos = new ServicoDaOficina[]{
                troca(1, "100.00", true), troca(2, "100.00", true), troca(3, "100.00", true),
                troca(4, "100.00", true), troca(5, "100.00", true),
                troca(6, "80.00", false),
                revisao("200.00")
        };
        OrdemDeServico os = os(StatusOS.ABERTA, 500, servicos);
        stubOS(os);

        OrcamentoVO orcamento = ordemDeServicoBO.calcularOrcamento(OS_ID);

        assertEquals(new BigDecimal("500.00"), orcamento.getTotalPecas());
        assertEquals(new BigDecimal("1200.00"), orcamento.getTotalMaoDeObra(),
                "6 trocas + 1 revisão a 150%");
        assertEquals(new BigDecimal("25.00"), orcamento.getDescontoAplicado());
        assertEquals(0, orcamento.getTotalFinal().compareTo(
                        orcamento.getTotalPecas()
                                .subtract(orcamento.getDescontoAplicado())
                                .add(orcamento.getTotalMaoDeObra())),
                "totalFinal = peças − desconto + mão de obra");
    }

    @Test
    @DisplayName("OS inexistente lança OrdemDeServicoNaoEncontradaException")
    void testeOSNaoEncontrada() {
        when(ordemDeServicoDAO.findById(OS_ID)).thenReturn(Optional.empty());

        assertThrows(OrdemDeServicoNaoEncontradaException.class,
                () -> ordemDeServicoBO.calcularOrcamento(OS_ID));
    }

    // ---------- criação ----------

    @Test
    @DisplayName("cria OS com status ABERTA e data de abertura")
    void testeCriarOS() {
        when(clienteBO.encontrarCliente(CLIENTE_ID)).thenReturn(cliente());
        when(motoBO.encontrarMoto("Honda", "CG")).thenReturn(moto(160));
        when(ordemDeServicoDAO.save(any(OrdemDeServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrdemDeServico os = ordemDeServicoBO.criarOS(CLIENTE_ID, "Honda", "CG");

        assertEquals(StatusOS.ABERTA, os.getStatus());
        assertNotNull(os.getDataAbertura());
        assertEquals(160, os.getMoto().getCilindradas());
    }

    @Test
    @DisplayName("lança MotoInvalidaException ao buscar moto inexistente")
    void testeExcecaoMotoInvalida() {
        when(clienteBO.encontrarCliente(CLIENTE_ID)).thenReturn(cliente());
        when(motoBO.encontrarMoto("Yamaha", "XTZ"))
                .thenThrow(new MotoInvalidaException("Moto não encontrada"));

        assertThrows(MotoInvalidaException.class,
                () -> ordemDeServicoBO.criarOS(CLIENTE_ID, "Yamaha", "XTZ"));
        verify(ordemDeServicoDAO, never()).save(any());
    }

    @Test
    @DisplayName("lança ClienteNaoEncontradoException ao buscar cliente inexistente")
    void testeExcecaoClienteNaoEncontrado() {
        when(clienteBO.encontrarCliente(99L))
                .thenThrow(new ClienteNaoEncontradoException("Cliente não encontrado"));

        assertThrows(ClienteNaoEncontradoException.class,
                () -> ordemDeServicoBO.criarOS(99L, "Honda", "CG"));
        verify(motoBO, never()).encontrarMoto(any(), any());
    }

    // ---------- serviços ----------

    @Test
    @DisplayName("lança EstoqueInsuficienteException ao usar peça sem estoque")
    void testeExcecaoEstoqueInsuficiente() {
        OrdemDeServico os = os(StatusOS.ABERTA, 300);
        stubOS(os);
        when(estoqueBO.checarEstoque(1L, 1))
                .thenThrow(new EstoqueInsuficienteException("Estoque insuficiente"));

        assertThrows(EstoqueInsuficienteException.class,
                () -> ordemDeServicoBO.adicionarServico(OS_ID, troca(1, "80.00", true)));
        verify(ordemDeServicoDAO, never()).save(any());
    }

    @Test
    @DisplayName("adiciona serviço de peça do cliente sem consultar estoque")
    void testeAdicionarServicoPecaDoCliente() {
        OrdemDeServico os = os(StatusOS.ABERTA, 300);
        stubOS(os);
        ServicoTrocaPeca servico = troca(1, "80.00", false);

        ordemDeServicoBO.adicionarServico(OS_ID, servico);

        assertEquals(1, os.getServicos().size());
        verify(estoqueBO, never()).checarEstoque(anyLong(), anyInt());
        verify(ordemDeServicoDAO).save(os);
    }

    @Test
    @DisplayName("serviço com valor negativo é rejeitado")
    void testeServicoInvalido() {
        OrdemDeServico os = os(StatusOS.ABERTA, 300);
        stubOS(os);
        ServicoRevisao invalido = revisao("-1.00");

        assertThrows(ValidacaoException.class,
                () -> ordemDeServicoBO.adicionarServico(OS_ID, invalido));
    }

    // ---------- confirmação e fechamento ----------

    @Test
    @DisplayName("confirmar inicia a OS e baixa estoque uma única vez")
    void testeConfirmarOS() {
        OrdemDeServico os = os(StatusOS.ABERTA, 300, troca(1, "80.00", true), revisao("100.00"));
        stubOS(os);

        Long id = ordemDeServicoBO.confirmarOS(OS_ID);

        assertEquals(OS_ID, id);
        assertEquals(StatusOS.EM_ANDAMENTO, os.getStatus());
        verify(estoqueBO).baixarEstoque(1L, 1);

        assertThrows(TransicaoStatusInvalidaException.class,
                () -> ordemDeServicoBO.confirmarOS(OS_ID));
        verify(estoqueBO, times(1)).baixarEstoque(1L, 1);
    }

    @Test
    @DisplayName("fechar conclui, encerra e agenda a próxima revisão")
    void testeFecharOS() {
        OrdemDeServico os = os(StatusOS.CONCLUIDA, 300, troca(1, "100.00", true));
        stubOS(os);
        LocalDate proxima = LocalDate.now().plusMonths(6);
        when(clienteBO.calcularProximaRevisao(any(LocalDate.class))).thenReturn(proxima);

        OrcamentoVO orcamento = ordemDeServicoBO.fecharOS(OS_ID);

        assertEquals(StatusOS.FECHADA, os.getStatus());
        assertNotNull(os.getDataFechamento());
        assertEquals(new BigDecimal("200.00"), orcamento.getTotalFinal());
        verify(clienteBO).agendarProximaRevisao(CLIENTE_ID, proxima);
    }
}
