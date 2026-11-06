package br.com.motoservices.bo;

import br.com.motoservices.dao.ClienteDAO;
import br.com.motoservices.dao.MotoDAO;
import br.com.motoservices.exception.ClienteNaoEncontradoException;
import br.com.motoservices.exception.MotoInvalidaException;
import br.com.motoservices.exception.ValidacaoException;
import br.com.motoservices.model.Cliente;
import br.com.motoservices.model.Moto;
import br.com.motoservices.vo.ClienteRequestVO;
import br.com.motoservices.vo.ClienteResponseVO;
import br.com.motoservices.vo.PaginaResponseVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.HashSet;
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
    @Mock
    private MotoDAO motoDAO;

    @InjectMocks
    private ClienteBO clienteBO;

    private ClienteRequestVO vo(String nome, String documento) {
        ClienteRequestVO vo = new ClienteRequestVO();
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

        ClienteResponseVO salvo = clienteBO.cadastrar(vo("Maria Silva", "529.982.247-25"));

        assertEquals(10L, salvo.getId());
        assertEquals(CPF_VALIDO, salvo.getCpfCnpj());
        assertEquals("Maria Silva", salvo.getNome());
    }

    @Test
    @DisplayName("aceita CNPJ (a constraint @CPF anterior rejeitava)")
    void cadastraComCnpj() {
        when(clienteDAO.findByCpfCnpj(CNPJ_VALIDO)).thenReturn(Optional.empty());
        when(clienteDAO.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClienteResponseVO salvo = clienteBO.cadastrar(vo("Oficina Central LTDA", CNPJ_VALIDO));

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
                .thenReturn(Optional.of(new Cliente(1L, "Outra", CPF_VALIDO, "x", null, null)));

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
                .thenReturn(Optional.of(new Cliente(1L, "Maria", CPF_VALIDO, "119", null, null)));

        PaginaResponseVO<ClienteResponseVO> resultado =
                clienteBO.buscar("529.982.247-25", PageRequest.of(0, 20));

        assertEquals(1, resultado.getContent().size());
        assertEquals("Maria", resultado.getContent().get(0).getNome());
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    @DisplayName("busca por nome quando o termo não é documento")
    void buscaPorNome() {
        Pageable pageable = PageRequest.of(0, 20);
        when(clienteDAO.findByNomeContainingIgnoreCase("maria", pageable))
                .thenReturn(new PageImpl<>(List.of(new Cliente(1L, "Maria", CPF_VALIDO, "119", null, null))));

        PaginaResponseVO<ClienteResponseVO> resultado = clienteBO.buscar("  maria  ", pageable);

        assertEquals(1, resultado.getContent().size());
        assertEquals(1, resultado.getTotalElements());
        verify(clienteDAO).findByNomeContainingIgnoreCase("maria", pageable);
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
        Cliente cliente = new Cliente(1L, "Maria", CPF_VALIDO, "119", null, null);
        when(clienteDAO.findById(1L)).thenReturn(Optional.of(cliente));

        LocalDate proxima = clienteBO.calcularProximaRevisao(LocalDate.now());
        clienteBO.agendarProximaRevisao(1L, proxima);

        assertEquals(proxima, cliente.getProximaRevisao());
        assertTrue(proxima.isAfter(LocalDate.now()));
        verify(clienteDAO).save(cliente);
    }

    @Test
    @DisplayName("vínculo de moto é criado e persistido")
    void vincularMoto() {
        Cliente cliente = new Cliente(1L, "Maria", CPF_VALIDO, "119", null, new HashSet<>());
        when(clienteDAO.findById(1L)).thenReturn(Optional.of(cliente));
        when(motoDAO.findById(2L)).thenReturn(Optional.of(new Moto(2L, "Honda", "CG 160", 160)));

        clienteBO.vincularMoto(1L, 2L);

        assertEquals(1, cliente.getMotos().size());
        verify(clienteDAO).save(cliente);
    }

    @Test
    @DisplayName("vínculo de moto é idempotente")
    void vinculoIdempotente() {
        Cliente cliente = new Cliente(1L, "Maria", CPF_VALIDO, "119", null, new HashSet<>());
        Moto moto = new Moto(2L, "Honda", "CG 160", 160);
        cliente.getMotos().add(moto);
        when(clienteDAO.findById(1L)).thenReturn(Optional.of(cliente));
        when(motoDAO.findById(2L)).thenReturn(Optional.of(moto));

        clienteBO.vincularMoto(1L, 2L);

        assertEquals(1, cliente.getMotos().size());
        verify(clienteDAO, never()).save(cliente);
    }

    @Test
    @DisplayName("desvínculo remove a moto do cliente")
    void desvincularMoto() {
        Cliente cliente = new Cliente(1L, "Maria", CPF_VALIDO, "119", null, new HashSet<>());
        cliente.getMotos().add(new Moto(2L, "Honda", "CG 160", 160));
        when(clienteDAO.findById(1L)).thenReturn(Optional.of(cliente));

        clienteBO.desvincularMoto(1L, 2L);

        assertTrue(cliente.getMotos().isEmpty());
        verify(clienteDAO).save(cliente);
    }

    @Test
    @DisplayName("vincular moto inexistente lança MotoInvalidaException")
    void vincularMotoInexistente() {
        when(clienteDAO.findById(1L))
                .thenReturn(Optional.of(new Cliente(1L, "Maria", CPF_VALIDO, "119", null, new HashSet<>())));
        when(motoDAO.findById(9L)).thenReturn(Optional.empty());

        assertThrows(MotoInvalidaException.class, () -> clienteBO.vincularMoto(1L, 9L));
    }

    @Test
    @DisplayName("listar motos do cliente devolve envelope ordenado")
    void listarMotos() {
        Cliente cliente = new Cliente(1L, "Maria", CPF_VALIDO, "119", null, new HashSet<>());
        cliente.getMotos().add(new Moto(2L, "Yamaha", "XTZ", 250));
        cliente.getMotos().add(new Moto(3L, "Honda", "CG 160", 160));
        when(clienteDAO.findById(1L)).thenReturn(Optional.of(cliente));

        var pagina = clienteBO.listarMotos(1L, PageRequest.of(0, 20));

        assertEquals(2, pagina.getContent().size());
        assertEquals("Honda", pagina.getContent().get(0).getMarca());
        assertEquals("Yamaha", pagina.getContent().get(1).getMarca());
    }
}
