package br.com.motoservices.bo;

import br.com.motoservices.dao.PecaDAO;
import br.com.motoservices.exception.EstoqueInsuficienteException;
import br.com.motoservices.model.Peca;
import br.com.motoservices.vo.PecaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EstoqueBO {

    private final PecaDAO pecaDAO;

    public Peca checarEstoque(Long pecaId, int quantidadeSolicitada) {
        // TODO: carregar a peça, comparar quantidade e lançar exceção se faltar
        throw new UnsupportedOperationException("Implementar checagem de estoque.");
    }

    public void baixarEstoque(Long pecaId, int quantidade) {
        // TODO: @Transactional — decrementar e persistir
        throw new UnsupportedOperationException("Implementar baixa de estoque.");
    }

    public List<PecaVO> listarBaixoEstoque() {
        // TODO: usar pecaDAO.findByQuantidadeEstoqueLessThan(...)
        throw new UnsupportedOperationException("Implementar listagem de baixo estoque.");
    }
}
