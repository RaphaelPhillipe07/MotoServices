package br.com.motoservices.bo;

import br.com.motoservices.dao.ClienteDAO;
import br.com.motoservices.dao.OrdemDeServicoDAO;
import br.com.motoservices.dao.PecaDAO;
import br.com.motoservices.exception.ClienteNaoEncontradoException;
import br.com.motoservices.exception.EstoqueInsuficienteException;
import br.com.motoservices.exception.MotoInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MockitoExtension.class)
class OrdemDeServicoBOTest {

    @Mock
    private OrdemDeServicoDAO ordemDeServicoDAO;
    @Mock
    private ClienteDAO clienteDAO;
    @Mock
    private PecaDAO pecaDAO;

    @Mock
    private MotoBO motoBO;
    @Mock
    private ClienteBO clienteBO;
    @Mock
    private EstoqueBO estoqueBO;

    @InjectMocks
    private OrdemDeServicoBO ordemDeServicoBO;

    @Test
    @DisplayName("desconto de 3% ativo com >= 6 peças da loja")
    void testeDescontoAplicado() {
        // TODO: montar OS com 6 ServicoTrocaPeca(pecaLoja=true), chamar calcularOrcamento,
        //       e afirmar que descontoAplicado = totalPecas * 0.03.
        fail("Implementar assim que o BO tiver a regra de desconto progressivo.");
    }

    @Test
    @DisplayName("sem desconto com < 6 peças da loja")
    void testeSemDesconto() {
        // TODO: OS com 5 peças da loja e afirmar descontoAplicado == 0.
        fail("Implementar assim que o BO tiver a regra de desconto progressivo.");
    }

    @Test
    @DisplayName("cilindradas > 400cc gera +50% e > 650cc gera +70% na mão de obra")
    void testeMotoMaoDeObraExtra() {
        // TODO: parametrizar com duas motos (ex.: 500cc e 700cc) e afirmar os acréscimos.
        fail("Implementar assim que o BO tiver o fator de risco por cilindrada.");
    }

    @Test
    @DisplayName("peça avulsa (cliente): cobra apenas a mão de obra")
    void testePecaAvulsa() {
        // TODO: ServicoTrocaPeca(pecaLoja=false) e afirmar que totalPecas == 0.
        fail("Implementar assim que o BO diferenciar peça da loja vs. do cliente.");
    }

    @Test
    @DisplayName("lança EstoqueInsuficienteException ao usar peça sem estoque")
    void testeExcecaoEstoqueInsuficiente() {
        // TODO: quando(estoqueBO.checarEstoque(...)) lançar a exceção e capturar aqui.
        assertThrows(EstoqueInsuficienteException.class,
                () -> fail("Configurar o mock do EstoqueBO para lançar a exceção."));
    }

    @Test
    @DisplayName("lança MotoInvalidaException ao buscar moto inexistente")
    void testeExcecaoMotoInvalida() {
        // TODO: quando(motoBO.encontrarMoto(...)) lançar a exceção.
        assertThrows(MotoInvalidaException.class,
                () -> fail("Configurar o mock do MotoBO para lançar a exceção."));
    }

    @Test
    @DisplayName("lança ClienteNaoEncontradoException ao buscar cliente inexistente")
    void testeExcecaoClienteNaoEncontrado() {
        // TODO: quando(clienteBO.encontrarCliente(...)) lançar a exceção.
        assertThrows(ClienteNaoEncontradoException.class,
                () -> fail("Configurar o mock do ClienteBO para lançar a exceção."));
    }

    @Test
    @DisplayName("totais do OrcamentoVO estão consistentes com as regras")
    void testeTotaisDoOrcamentoVOEstaoCorretos() {
        // TODO: montar cenário completo e afirmar
        //       totalFinal == totalPecas - descontoAplicado + totalMaoDeObra.
        fail("Implementar assim que o BO tiver a montagem do OrcamentoVO.");
    }
}
