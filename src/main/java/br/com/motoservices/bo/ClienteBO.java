package br.com.motoservices.bo;

import br.com.motoservices.dao.ClienteDAO;
import br.com.motoservices.exception.ClienteNaoEncontradoException;
import br.com.motoservices.exception.ValidacaoException;
import br.com.motoservices.model.Cliente;
import br.com.motoservices.util.Documento;
import br.com.motoservices.vo.ClienteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteBO {

    /** Periodicidade padrão de revisão sugerida ao cliente ao fechar uma OS. */
    public static final int PERIODICIDADE_REVISAO_MESES = 6;

    private final ClienteDAO clienteDAO;

    @Transactional(readOnly = true)
    public Cliente encontrarCliente(Long clienteId) {
        if (clienteId == null) {
            throw new ValidacaoException("ID do cliente é obrigatório.");
        }
        return clienteDAO.findById(clienteId)
                .orElseThrow(() -> new ClienteNaoEncontradoException(
                        "Cliente não encontrado: " + clienteId));
    }

    @Transactional
    public ClienteVO cadastrar(ClienteVO clienteVO) {
        if (clienteVO == null) {
            throw new ValidacaoException("Dados do cliente são obrigatórios.");
        }
        String nome = exigirTexto(clienteVO.getNome(), "nome");
        String contato = exigirTexto(clienteVO.getContato(), "contato");
        String documento = Documento.normalizar(clienteVO.getCpfCnpj());
        if (!Documento.isValido(documento)) {
            throw new ValidacaoException("CPF/CNPJ inválido.");
        }
        if (clienteDAO.findByCpfCnpj(documento).isPresent()) {
            throw new ValidacaoException("Já existe cliente cadastrado com este CPF/CNPJ.");
        }

        Cliente cliente = new Cliente();
        cliente.setNome(nome);
        cliente.setCpfCnpj(documento);
        cliente.setContato(contato);
        cliente.setProximaRevisao(clienteVO.getProximaRevisao());

        return paraVO(clienteDAO.save(cliente));
    }

    @Transactional(readOnly = true)
    public List<ClienteVO> buscar(String nomeOuCpf) {
        String termo = exigirTexto(nomeOuCpf, "nomeOuCpf").trim();
        String documento = Documento.normalizar(termo);

        List<Cliente> encontrados;
        if (!documento.isEmpty() && (documento.length() == 11 || documento.length() == 14)) {
            encontrados = clienteDAO.findByCpfCnpj(documento)
                    .map(List::of)
                    .orElseGet(List::of);
        } else {
            encontrados = clienteDAO.findByNomeContainingIgnoreCase(termo);
        }
        return encontrados.stream().map(ClienteBO::paraVO).toList();
    }

    /**
     * Atualiza a data da próxima revisão do cliente.
     */
    @Transactional
    public void agendarProximaRevisao(Long clienteId, LocalDate proximaRevisao) {
        if (proximaRevisao == null) {
            throw new ValidacaoException("Data da próxima revisão é obrigatória.");
        }
        if (proximaRevisao.isBefore(LocalDate.now())) {
            throw new ValidacaoException("A próxima revisão não pode estar no passado.");
        }
        Cliente cliente = encontrarCliente(clienteId);
        cliente.setProximaRevisao(proximaRevisao);
        clienteDAO.save(cliente);
    }

    public LocalDate calcularProximaRevisao(LocalDate referencia) {
        LocalDate base = referencia == null ? LocalDate.now() : referencia;
        return base.plusMonths(PERIODICIDADE_REVISAO_MESES);
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ValidacaoException("O campo '" + campo + "' é obrigatório.");
        }
        return valor.trim();
    }

    static ClienteVO paraVO(Cliente cliente) {
        return new ClienteVO(cliente.getId(), cliente.getNome(), cliente.getCpfCnpj(),
                cliente.getContato(), cliente.getProximaRevisao());
    }
}
