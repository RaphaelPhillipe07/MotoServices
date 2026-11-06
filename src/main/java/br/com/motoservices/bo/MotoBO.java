package br.com.motoservices.bo;

import br.com.motoservices.dao.MotoDAO;
import br.com.motoservices.exception.MotoInvalidaException;
import br.com.motoservices.exception.ValidacaoException;
import br.com.motoservices.model.Moto;
import br.com.motoservices.vo.MotoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MotoBO {

    private final MotoDAO motoDAO;

    /**
     * Valida a moto contra o catálogo dinâmico (marca + modelo, sem diferenciar maiúsculas).
     */
    @Transactional(readOnly = true)
    public Moto encontrarMoto(String marca, String modelo) {
        String marcaNormalizada = exigirTexto(marca, "marca");
        String modeloNormalizado = exigirTexto(modelo, "modelo");
        return motoDAO.findByMarcaIgnoreCaseAndModeloIgnoreCase(marcaNormalizada, modeloNormalizado)
                .orElseThrow(() -> new MotoInvalidaException(
                        "Moto não encontrada no catálogo: " + marcaNormalizada + " " + modeloNormalizado));
    }

    @Transactional
    public MotoVO cadastrar(MotoVO motoVO) {
        if (motoVO == null) {
            throw new ValidacaoException("Dados da moto são obrigatórios.");
        }
        String marca = exigirTexto(motoVO.getMarca(), "marca");
        String modelo = exigirTexto(motoVO.getModelo(), "modelo");
        Integer cilindradas = motoVO.getCilindradas();
        if (cilindradas == null || cilindradas <= 0) {
            throw new ValidacaoException("Cilindradas devem ser um número positivo.");
        }
        if (motoDAO.findByMarcaIgnoreCaseAndModeloIgnoreCase(marca, modelo).isPresent()) {
            throw new ValidacaoException("Já existe uma moto cadastrada para " + marca + " " + modelo + ".");
        }

        Moto moto = new Moto();
        moto.setMarca(marca);
        moto.setModelo(modelo);
        moto.setCilindradas(cilindradas);

        return paraVO(motoDAO.save(moto));
    }

    @Transactional(readOnly = true)
    public List<MotoVO> filtrarPorCilindradas(int minimo, int maximo) {
        if (minimo <= 0) {
            throw new ValidacaoException("Cilindrada mínima deve ser positiva.");
        }
        if (maximo < minimo) {
            throw new ValidacaoException("Cilindrada máxima deve ser maior ou igual à mínima.");
        }
        return motoDAO.findByCilindradasBetween(minimo, maximo).stream()
                .map(MotoBO::paraVO)
                .toList();
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ValidacaoException("O campo '" + campo + "' é obrigatório.");
        }
        return valor.trim();
    }

    static MotoVO paraVO(Moto moto) {
        return new MotoVO(moto.getId(), moto.getMarca(), moto.getModelo(), moto.getCilindradas());
    }
}
