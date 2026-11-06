package br.com.motoservices.bo;

import br.com.motoservices.dao.PecaDAO;
import br.com.motoservices.exception.EstoqueInsuficienteException;
import br.com.motoservices.exception.PecaNaoEncontradaException;
import br.com.motoservices.exception.ValidacaoException;
import br.com.motoservices.mapper.PecaMapper;
import br.com.motoservices.model.Peca;
import br.com.motoservices.vo.PaginaResponseVO;
import br.com.motoservices.vo.PecaRequestVO;
import br.com.motoservices.vo.PecaResponseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class EstoqueBO {

    private final PecaDAO pecaDAO;

    // ---------- cadastro e consulta ----------

    @Transactional
    public PecaResponseVO cadastrar(PecaRequestVO request) {
        if (request == null) {
            throw new ValidacaoException("Dados da peça são obrigatórios.");
        }
        String codigo = exigirTexto(request.getCodigo(), "codigo");
        exigirTexto(request.getDescricao(), "descricao");
        exigirNaoNegativo(request.getValor(), "valor");
        exigirNaoNegativo(request.getQuantidadeEstoque(), "quantidadeEstoque");
        exigirNaoNegativo(request.getEstoqueMinimo(), "estoqueMinimo");

        if (pecaDAO.findByCodigo(codigo).isPresent()) {
            throw new ValidacaoException("Já existe peça cadastrada com o código " + codigo + ".");
        }
        return PecaMapper.toResponse(pecaDAO.save(PecaMapper.toEntity(request)));
    }

    @Transactional(readOnly = true)
    public PecaResponseVO buscarPorId(Long pecaId) {
        return PecaMapper.toResponse(encontrarPeca(pecaId));
    }

    @Transactional(readOnly = true)
    public PaginaResponseVO<PecaResponseVO> listar(String busca, Pageable pageable) {
        Pageable paginacao = pageable == null ? PageRequest.of(0, 20) : pageable;
        Page<Peca> pagina = busca == null || busca.isBlank()
                ? pecaDAO.findAll(paginacao)
                : pecaDAO.findByDescricaoContainingIgnoreCaseOrCodigoContainingIgnoreCase(
                        busca.trim(), busca.trim(), paginacao);
        return PaginaResponseVO.de(pagina, PecaMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PaginaResponseVO<PecaResponseVO> listarBaixoEstoque(Pageable pageable) {
        Pageable paginacao = pageable == null ? PageRequest.of(0, 20) : pageable;
        return PaginaResponseVO.de(
                pecaDAO.findAbaixoDoEstoqueMinimo(paginacao),
                PecaMapper::toResponse);
    }

    // ---------- movimentação de estoque ----------

    /**
     * Registra entrada de estoque (compra/recebimento) de uma peça existente.
     */
    @Transactional
    public PecaResponseVO registrarEntrada(Long pecaId, int quantidade) {
        if (quantidade <= 0) {
            throw new ValidacaoException("Quantidade da entrada deve ser positiva.");
        }
        Peca peca = encontrarPeca(pecaId);
        int atual = peca.getQuantidadeEstoque() == null ? 0 : peca.getQuantidadeEstoque();
        peca.setQuantidadeEstoque(atual + quantidade);
        return PecaMapper.toResponse(pecaDAO.save(peca));
    }

    /**
     * Carrega a peça e confirma que há quantidade suficiente em estoque.
     */
    @Transactional(readOnly = true)
    public Peca checarEstoque(Long pecaId, int quantidadeSolicitada) {
        if (quantidadeSolicitada <= 0) {
            throw new ValidacaoException("Quantidade solicitada deve ser positiva.");
        }
        Peca peca = encontrarPeca(pecaId);
        int disponivel = peca.getQuantidadeEstoque() == null ? 0 : peca.getQuantidadeEstoque();
        if (disponivel < quantidadeSolicitada) {
            throw new EstoqueInsuficienteException(
                    "Estoque insuficiente para a peça " + peca.getCodigo()
                            + ": disponível " + disponivel + ", solicitado " + quantidadeSolicitada + ".");
        }
        return peca;
    }

    @Transactional
    public void baixarEstoque(Long pecaId, int quantidade) {
        Peca peca = checarEstoque(pecaId, quantidade);
        int linhasAfetadas = pecaDAO.baixarEstoque(pecaId, quantidade);
        if (linhasAfetadas == 0) {
            throw new EstoqueInsuficienteException(
                    "Estoque insuficiente para a peça " + peca.getCodigo()
                            + ": saldo alterado concorrentemente, solicitado " + quantidade + ".");
        }
    }

    @Transactional(readOnly = true)
    public Peca encontrarPeca(Long pecaId) {
        if (pecaId == null) {
            throw new ValidacaoException("ID da peça é obrigatório.");
        }
        return pecaDAO.findById(pecaId)
                .orElseThrow(() -> new PecaNaoEncontradaException("Peça não encontrada: " + pecaId));
    }

    // ---------- helpers ----------

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ValidacaoException("O campo '" + campo + "' é obrigatório.");
        }
        return valor.trim();
    }

    private static void exigirNaoNegativo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() < 0) {
            throw new ValidacaoException("O campo '" + campo + "' não pode ser negativo.");
        }
    }

    private static void exigirNaoNegativo(Integer valor, String campo) {
        if (valor == null || valor < 0) {
            throw new ValidacaoException("O campo '" + campo + "' não pode ser negativo.");
        }
    }
}
