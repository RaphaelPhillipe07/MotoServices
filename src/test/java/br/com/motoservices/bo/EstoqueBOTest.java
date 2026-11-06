package br.com.motoservices.bo;

import br.com.motoservices.dao.PecaDAO;
import br.com.motoservices.exception.EstoqueInsuficienteException;
import br.com.motoservices.exception.PecaNaoEncontradaException;
import br.com.motoservices.exception.ValidacaoException;
import br.com.motoservices.model.Peca;
import br.com.motoservices.vo.PecaVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstoqueBOTest {

    @Mock
    private PecaDAO pecaDAO;

    @InjectMocks
    private EstoqueBO estoqueBO;

    private Peca peca(int quantidade) {
        return new Peca(1L, "P-001", "Pastilha", new BigDecimal("80.00"), quantidade, 3);
    }

    @Test
    @DisplayName("checagem de estoque aprova quantidade disponível")
    void checagemAprovada() {
        when(pecaDAO.findById(1L)).thenReturn(Optional.of(peca(5)));

        assertEquals(5, estoqueBO.checarEstoque(1L, 5).getQuantidadeEstoque());
    }

    @Test
    @DisplayName("checagem de estoque reprova quantidade acima do disponível")
    void checagemReprovada() {
        when(pecaDAO.findById(1L)).thenReturn(Optional.of(peca(2)));

        assertThrows(EstoqueInsuficienteException.class, () -> estoqueBO.checarEstoque(1L, 3));
    }

    @Test
    @DisplayName("quantidade solicitada deve ser positiva")
    void quantidadeInvalida() {
        assertThrows(ValidacaoException.class, () -> estoqueBO.checarEstoque(1L, 0));
    }

    @Test
    @DisplayName("peça inexistente lança PecaNaoEncontradaException")
    void pecaInexistente() {
        when(pecaDAO.findById(9L)).thenReturn(Optional.empty());

        assertThrows(PecaNaoEncontradaException.class, () -> estoqueBO.baixarEstoque(9L, 1));
    }

    @Test
    @DisplayName("baixa de estoque decrementa e persiste")
    void baixaDeEstoque() {
        Peca peca = peca(5);
        when(pecaDAO.findById(1L)).thenReturn(Optional.of(peca));

        estoqueBO.baixarEstoque(1L, 2);

        assertEquals(3, peca.getQuantidadeEstoque());
        verify(pecaDAO).save(peca);
    }

    @Test
    @DisplayName("baixa sem estoque não altera a peça nem persiste")
    void baixaSemEstoqueNaoAltera() {
        Peca peca = peca(1);
        when(pecaDAO.findById(1L)).thenReturn(Optional.of(peca));

        assertThrows(EstoqueInsuficienteException.class, () -> estoqueBO.baixarEstoque(1L, 2));

        assertEquals(1, peca.getQuantidadeEstoque());
    }

    @Test
    @DisplayName("listagem de baixo estoque usa a comparação coluna-a-coluna")
    void listaBaixoEstoque() {
        when(pecaDAO.findAbaixoDoEstoqueMinimo()).thenReturn(List.of(peca(1)));

        List<PecaVO> resultado = estoqueBO.listarBaixoEstoque();

        assertEquals(1, resultado.size());
        assertEquals("P-001", resultado.get(0).getCodigo());
        assertEquals(new BigDecimal("80.00"), resultado.get(0).getValor());
    }
}
