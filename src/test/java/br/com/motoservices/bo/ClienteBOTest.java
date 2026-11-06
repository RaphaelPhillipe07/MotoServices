package br.com.motoservices.bo;

import br.com.motoservices.dao.ClienteDAO;
import br.com.motoservices.exception.ClienteNaoEncontradoException;
import br.com.motoservices.exception.ValidacaoException;
import br.com.motoservices.model.Cliente;
import br.com.motoservices.vo.ClienteVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteBOTest {

    private static final String CPF_VALIDO = "52998224725";
    private static final String CNPJ_VALIDO = "11222333000181";

    @Mock
    private ClienteDAO clienteDAO;

    @InjectMocks
    private ClienteBO clienteBO;

    private ClienteVO vo(String nome, String documento) {
        ClienteVO vo = new ClienteVO();
        vo.setNome(nome);
        vo.setCpfCnpj(documento);
        vo.setContato("11999999999");
        return vo;
    }

    @Test
    @DisplayName("cadastra cliente com CPF e normaliza a pontuação")
    void cadastraComCpfFormatado() {
        when(clienteDAO.findByCpfCnpj(CPF_VALIDO)).thenReturn(Optional.empty());
        when(clienteDAO.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente cliente = invocation.getArgument(0);
            cliente.setId(10L);
            return cliente;
        });

        ClienteVO salvo = clienteBO.cadastrar(vo("Maria Silva", "529.982.247-25"));

        assertEquals(10L, salvo.getId());
        assertEquals(CPF_VALIDO, salvo.getCpfCnpj());
        assertEquals("Maria Silva", salvo.getNome());
    }

    @Test
    @DisplayName("aceita CNPJ (a constraint @CPF anterior rejeitava)")
    void cadastraComCnpj() {
        when(clienteDAO.findByCpfCnpj(CNPJ_VALIDO)).thenReturn(Optional.empty());
        when(clienteDAO.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClienteVO salvo = clienteBO.cadastrar(vo("Oficina Central LTDA", CNPJ_VALIDO));

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteDAO).save(captor.capture());
        assertEquals(CNPJ_VALIDO, captor.getValue().getCpfCnpj());
        assertEquals(CNPJ_VALIDO, salvo.getCpfCnpj());
    }

    @Test
    @DisplayName("rejeita CPF/CNPJ com dígito verificador inválido")
    void rejeitaDocumentoInvalido() {
        assertThrows(ValidacaoException.class,
                () -> clienteBO.cadastrar(vo("João", "11111111111")));
        verify(clienteDAO, never()).save(any());
    }

    @Test
    @DisplayName("rejeita documento já cadastrado")
    void rejeitaDocumentoDuplicado() {
        when(clienteDAO.findByCpfCnpj(CPF_VALIDO))
                .thenReturn(Optional.of(new Cliente(1L, "Outra", CPF_VALIDO, "x", null)));

        assertThrows(ValidacaoException.class,
                () -> clienteBO.cadastrar(vo("Maria", CPF_VALIDO)));
    }

    @Test
    @DisplayName("rejeita cadastro sem nome")
    void rejeitaNomeEmBranco() {
        assertThrows(ValidacaoException.class,
                () -> clienteBO.cadastrar(vo("   ", CPF_VALIDO)));
    }

    @Test
    @DisplayName("busca por CPF formatado usa o documento normalizado")
    void buscaPorCpfFormatado() {
        when(clienteDAO.findByCpfCnpj(CPF_VALIDO))
                .thenReturn(Optional.of(new Cliente(1L, "Maria", CPF_VALIDO, "119", null)));

        List<ClienteVO> resultado = clienteBO.buscar("529.982.247-25");

        assertEquals(1, resultado.size());
        assertEquals("Maria", resultado.get(0).getNome());
    }

    @Test
    @DisplayName("busca por nome quando o termo não é documento")
    void buscaPorNome() {
        when(clienteDAO.findByNomeContainingIgnoreCase("maria"))
                .thenReturn(List.of(new Cliente(1L, "Maria", CPF_VALIDO, "119", null)));

        assertEquals(1, clienteBO.buscar("  maria  ").size());
        verify(clienteDAO).findByNomeContainingIgnoreCase("maria");
    }

    @Test
    @DisplayName("cliente inexistente lança ClienteNaoEncontradoException")
    void clienteInexistente() {
        when(clienteDAO.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClienteNaoEncontradoException.class,
                () -> clienteBO.encontrarCliente(99L));
    }

    @Test
    @DisplayName("agendar revisão no passado é rejeitado")
    void revisaoNoPassado() {
        assertThrows(ValidacaoException.class,
                () -> clienteBO.agendarProximaRevisao(1L, LocalDate.now().minusDays(1)));
    }

    @Test
    @DisplayName("agendar revisão atualiza o cliente e persiste")
    void agendarRevisaoFutura() {
        Cliente cliente = new Cliente(1L, "Maria", CPF_VALIDO, "119", null);
        when(clienteDAO.findById(1L)).thenReturn(Optional.of(cliente));

        LocalDate proxima = clienteBO.calcularProximaRevisao(LocalDate.now());
        clienteBO.agendarProximaRevisao(1L, proxima);

        assertEquals(proxima, cliente.getProximaRevisao());
        assertTrue(proxima.isAfter(LocalDate.now()));
        verify(clienteDAO).save(cliente);
    }
}
