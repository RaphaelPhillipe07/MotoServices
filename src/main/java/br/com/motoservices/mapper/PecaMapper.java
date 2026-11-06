package br.com.motoservices.mapper;

import br.com.motoservices.model.Peca;
import br.com.motoservices.vo.PecaRequestVO;
import br.com.motoservices.vo.PecaResponseVO;

/**
 * Conversão entre entidade {@link Peca} e VOs de entrada/saída.
 */
public final class PecaMapper {

    private PecaMapper() {
    }

    public static Peca toEntity(PecaRequestVO request) {
        Peca peca = new Peca();
        peca.setCodigo(request.getCodigo().trim());
        peca.setDescricao(request.getDescricao().trim());
        peca.setValor(request.getValor());
        peca.setQuantidadeEstoque(request.getQuantidadeEstoque());
        peca.setEstoqueMinimo(request.getEstoqueMinimo());
        return peca;
    }

    public static PecaResponseVO toResponse(Peca peca) {
        return new PecaResponseVO(peca.getId(), peca.getCodigo(), peca.getDescricao(),
                peca.getValor(), peca.getQuantidadeEstoque(), peca.getEstoqueMinimo());
    }
}
