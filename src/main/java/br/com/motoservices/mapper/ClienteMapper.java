package br.com.motoservices.mapper;

import br.com.motoservices.model.Cliente;
import br.com.motoservices.util.Documento;
import br.com.motoservices.vo.ClienteRequestVO;
import br.com.motoservices.vo.ClienteResponseVO;

/**
 * Conversão entre entidade {@link Cliente} e VOs de entrada/saída.
 */
public final class ClienteMapper {

    private ClienteMapper() {
    }

    public static Cliente toEntity(ClienteRequestVO request) {
        Cliente cliente = new Cliente();
        cliente.setNome(request.getNome().trim());
        cliente.setCpfCnpj(Documento.normalizar(request.getCpfCnpj()));
        cliente.setContato(request.getContato().trim());
        cliente.setProximaRevisao(request.getProximaRevisao());
        return cliente;
    }

    public static ClienteResponseVO toResponse(Cliente cliente) {
        return new ClienteResponseVO(cliente.getId(), cliente.getNome(), cliente.getCpfCnpj(),
                cliente.getContato(), cliente.getProximaRevisao());
    }
}
