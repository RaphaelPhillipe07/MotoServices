package br.com.motoservices.bo;

import br.com.motoservices.dao.ClienteDAO;
import br.com.motoservices.exception.ClienteNaoEncontradoException;
import br.com.motoservices.model.Cliente;
import br.com.motoservices.vo.ClienteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteBO {

    private final ClienteDAO clienteDAO;

    public Cliente encontrarCliente(Long clienteId) {
        // TODO: clienteDAO.findById(...).orElseThrow(...)
        throw new UnsupportedOperationException("Implementar busca de cliente por id.");
    }

    public ClienteVO cadastrar(ClienteVO clienteVO) {
        // TODO: validar CPF/CNPJ e persistir
        throw new UnsupportedOperationException("Implementar cadastro de cliente.");
    }

    public List<ClienteVO> buscar(String nomeOuCpf) {
        // TODO: decidir entre findByNomeContainingIgnoreCase / findByCpfCnpj
        throw new UnsupportedOperationException("Implementar busca de cliente.");
    }

    /**
     * Atualiza a data da próxima revisão do cliente ao fechar uma OS.
     */
    public void agendarProximaRevisao(Long clienteId /* , LocalDate proxima */) {
        // TODO: calcular periodicidade e persistir em cliente.proximaRevisao
        throw new UnsupportedOperationException("Implementar agendamento de revisão.");
    }
}
