package br.com.motoservices.bo;

import br.com.motoservices.dao.PecaDAO;
import br.com.motoservices.exception.EstoqueInsuficienteException;
import br.com.motoservices.exception.PecaNaoEncontradaException;
import br.com.motoservices.exception.ValidacaoException;
import br.com.motoservices.model.Peca;
import br.com.motoservices.vo.PecaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EstoqueBO {

    private final PecaDAO pecaDAO;

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
        peca.setQuantidadeEstoque(peca.getQuantidadeEstoque() - quantidade);
        pecaDAO.save(peca);
    }

    @Transactional(readOnly = true)
    public List<PecaVO> listarBaixoEstoque() {
        return pecaDAO.findAbaixoDoEstoqueMinimo().stream()
                .map(EstoqueBO::paraVO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Peca encontrarPeca(Long pecaId) {
        if (pecaId == null) {
            throw new ValidacaoException("ID da peça é obrigatório.");
        }
        return pecaDAO.findById(pecaId)
                .orElseThrow(() -> new PecaNaoEncontradaException("Peça não encontrada: " + pecaId));
    }

    static PecaVO paraVO(Peca peca) {
        return new PecaVO(peca.getId(), peca.getCodigo(), peca.getDescricao(),
                peca.getValor(), peca.getQuantidadeEstoque(), peca.getEstoqueMinimo());
    }
}
