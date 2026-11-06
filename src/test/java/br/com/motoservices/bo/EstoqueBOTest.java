package br.com.motoservices.bo;

import br.com.motoservices.dao.PecaDAO;
import br.com.motoservices.exception.EstoqueInsuficienteException;
import br.com.motoservices.exception.PecaNaoEncontradaException;
import br.com.motoservices.exception.ValidacaoException;
import br.com.motoservices.model.Peca;
import br.com.motoservices.vo.PaginaResponseVO;
import br.com.motoservices.vo.PecaRequestVO;
import br.com.motoservices.vo.PecaResponseVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
        Pageable pageable = PageRequest.of(0, 20);
        when(pecaDAO.findAbaixoDoEstoqueMinimo(pageable))
                .thenReturn(new PageImpl<>(List.of(peca(1))));

        PaginaResponseVO<PecaResponseVO> resultado = estoqueBO.listarBaixoEstoque(pageable);

        assertEquals(1, resultado.getContent().size());
        assertEquals(1, resultado.getTotalElements());
        assertEquals("P-001", resultado.getContent().get(0).getCodigo());
        assertEquals(new BigDecimal("80.00"), resultado.getContent().get(0).getValor());
    }

    @Test
    @DisplayName("cadastro de peça válido devolve VO com id")
    void cadastroValido() {
        when(pecaDAO.findByCodigo("P-002")).thenReturn(Optional.empty());
        when(pecaDAO.save(any(Peca.class))).thenAnswer(invocation -> {
            Peca peca = invocation.getArgument(0);
            peca.setId(2L);
            return peca;
        });

        PecaResponseVO salvo = estoqueBO.cadastrar(new PecaRequestVO(
                "P-002", "Filtro de óleo", new BigDecimal("45.90"), 5, 2));

        assertEquals(2L, salvo.getId());
        assertEquals("P-002", salvo.getCodigo());
        assertEquals(new BigDecimal("45.90"), salvo.getValor());
    }

    @Test
    @DisplayName("não permite cadastrar peça com código repetido")
    void cadastroCodigoDuplicado() {
        when(pecaDAO.findByCodigo("P-001")).thenReturn(Optional.of(peca(1)));

        assertThrows(ValidacaoException.class, () -> estoqueBO.cadastrar(new PecaRequestVO(
                "P-001", "Outra descrição", new BigDecimal("10.00"), 1, 1)));
        verify(pecaDAO, never()).save(any());
    }

    @Test
    @DisplayName("cadastro rejeita valor negativo")
    void cadastroValorNegativo() {
        assertThrows(ValidacaoException.class, () -> estoqueBO.cadastrar(new PecaRequestVO(
                "P-003", "Peça", new BigDecimal("-1.00"), 1, 1)));
    }

    @Test
    @DisplayName("entrada de estoque soma à quantidade atual")
    void entradaDeEstoque() {
        Peca peca = peca(5);
        when(pecaDAO.findById(1L)).thenReturn(Optional.of(peca));
        when(pecaDAO.save(any(Peca.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PecaResponseVO atualizada = estoqueBO.registrarEntrada(1L, 3);

        assertEquals(8, peca.getQuantidadeEstoque());
        assertEquals(8, atualizada.getQuantidadeEstoque());
        verify(pecaDAO).save(peca);
    }

    @Test
    @DisplayName("entrada de estoque exige quantidade positiva")
    void entradaComQuantidadeInvalida() {
        assertThrows(ValidacaoException.class, () -> estoqueBO.registrarEntrada(1L, 0));
        verify(pecaDAO, never()).save(any());
    }

    @Test
    @DisplayName("listagem sem busca usa findAll paginado")
    void listagemSemBusca() {
        Pageable pageable = PageRequest.of(0, 20);
        when(pecaDAO.findAll(pageable)).thenReturn(new PageImpl<>(List.of(peca(4))));

        PaginaResponseVO<PecaResponseVO> resultado = estoqueBO.listar(null, pageable);

        assertEquals(1, resultado.getContent().size());
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    @DisplayName("listagem com busca filtra por código ou descrição")
    void listagemComBusca() {
        Pageable pageable = PageRequest.of(0, 20);
        when(pecaDAO.findByDescricaoContainingIgnoreCaseOrCodigoContainingIgnoreCase(
                "pastilha", "pastilha", pageable))
                .thenReturn(new PageImpl<>(List.of(peca(4))));

        PaginaResponseVO<PecaResponseVO> resultado = estoqueBO.listar("  pastilha  ", pageable);

        assertEquals(1, resultado.getContent().size());
        assertEquals("P-001", resultado.getContent().get(0).getCodigo());
    }
}
