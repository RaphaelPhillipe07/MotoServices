package br.com.motoservices.bo;

import br.com.motoservices.dao.ClienteDAO;
import br.com.motoservices.exception.ClienteNaoEncontradoException;
import br.com.motoservices.exception.ValidacaoException;
import br.com.motoservices.mapper.ClienteMapper;
import br.com.motoservices.model.Cliente;
import br.com.motoservices.util.Documento;
import br.com.motoservices.vo.ClienteRequestVO;
import br.com.motoservices.vo.ClienteResponseVO;
import br.com.motoservices.vo.PaginaResponseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @Transactional(readOnly = true)
    public ClienteResponseVO buscarPorId(Long clienteId) {
        return ClienteMapper.toResponse(encontrarCliente(clienteId));
    }

    @Transactional
    public ClienteResponseVO cadastrar(ClienteRequestVO request) {
        if (request == null) {
            throw new ValidacaoException("Dados do cliente são obrigatórios.");
        }
        exigirTexto(request.getNome(), "nome");
        exigirTexto(request.getContato(), "contato");
        String documento = Documento.normalizar(request.getCpfCnpj());
        if (!Documento.isValido(documento)) {
            throw new ValidacaoException("CPF/CNPJ inválido.");
        }
        if (clienteDAO.findByCpfCnpj(documento).isPresent()) {
            throw new ValidacaoException("Já existe cliente cadastrado com este CPF/CNPJ.");
        }

        return ClienteMapper.toResponse(clienteDAO.save(ClienteMapper.toEntity(request)));
    }

    @Transactional(readOnly = true)
    public PaginaResponseVO<ClienteResponseVO> buscar(String nomeOuCpf, Pageable pageable) {
        String termo = exigirTexto(nomeOuCpf, "nomeOuCpf").trim();
        String documento = Documento.normalizar(termo);

        if (!documento.isEmpty() && (documento.length() == 11 || documento.length() == 14)) {
            List<ClienteResponseVO> encontrados = clienteDAO.findByCpfCnpj(documento)
                    .map(List::of)
                    .orElseGet(List::of)
                    .stream()
                    .map(ClienteMapper::toResponse)
                    .toList();
            return PaginaResponseVO.de(encontrados, 0, Math.max(encontrados.size(), 1));
        }

        Pageable paginacao = pageable == null ? PageRequest.of(0, 20) : pageable;
        return PaginaResponseVO.de(
                clienteDAO.findByNomeContainingIgnoreCase(termo, paginacao),
                ClienteMapper::toResponse);
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
}
