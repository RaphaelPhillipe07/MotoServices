package br.com.motoservices.mapper;

import br.com.motoservices.model.Moto;
import br.com.motoservices.vo.MotoRequestVO;
import br.com.motoservices.vo.MotoResponseVO;

/**
 * Conversão entre entidade {@link Moto} e VOs de entrada/saída.
 */
public final class MotoMapper {

    private MotoMapper() {
    }

    public static Moto toEntity(MotoRequestVO request) {
        Moto moto = new Moto();
        moto.setMarca(request.getMarca().trim());
        moto.setModelo(request.getModelo().trim());
        moto.setCilindradas(request.getCilindradas());
        return moto;
    }

    public static MotoResponseVO toResponse(Moto moto) {
        return new MotoResponseVO(moto.getId(), moto.getMarca(), moto.getModelo(), moto.getCilindradas());
    }
}
