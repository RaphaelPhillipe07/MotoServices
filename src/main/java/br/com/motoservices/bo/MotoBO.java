package br.com.motoservices.bo;

import br.com.motoservices.dao.MotoDAO;
import br.com.motoservices.exception.MotoInvalidaException;
import br.com.motoservices.exception.ValidacaoException;
import br.com.motoservices.mapper.MotoMapper;
import br.com.motoservices.model.Moto;
import br.com.motoservices.vo.MotoRequestVO;
import br.com.motoservices.vo.MotoResponseVO;
import br.com.motoservices.vo.PaginaResponseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public MotoResponseVO buscarPorId(Long motoId) {
        if (motoId == null) {
            throw new ValidacaoException("ID da moto é obrigatório.");
        }
        Moto moto = motoDAO.findById(motoId)
                .orElseThrow(() -> new MotoInvalidaException("Moto não encontrada: " + motoId));
        return MotoMapper.toResponse(moto);
    }

    @Transactional
    public MotoResponseVO cadastrar(MotoRequestVO request) {
        if (request == null) {
            throw new ValidacaoException("Dados da moto são obrigatórios.");
        }
        String marca = exigirTexto(request.getMarca(), "marca");
        String modelo = exigirTexto(request.getModelo(), "modelo");
        Integer cilindradas = request.getCilindradas();
        if (cilindradas == null || cilindradas <= 0) {
            throw new ValidacaoException("Cilindradas devem ser um número positivo.");
        }
        if (motoDAO.findByMarcaIgnoreCaseAndModeloIgnoreCase(marca, modelo).isPresent()) {
            throw new ValidacaoException("Já existe uma moto cadastrada para " + marca + " " + modelo + ".");
        }

        return MotoMapper.toResponse(motoDAO.save(MotoMapper.toEntity(request)));
    }

    @Transactional(readOnly = true)
    public PaginaResponseVO<MotoResponseVO> filtrarPorCilindradas(int minimo, int maximo, Pageable pageable) {
        if (minimo <= 0) {
            throw new ValidacaoException("Cilindrada mínima deve ser positiva.");
        }
        if (maximo < minimo) {
            throw new ValidacaoException("Cilindrada máxima deve ser maior ou igual à mínima.");
        }
        Pageable paginacao = pageable == null ? PageRequest.of(0, 20) : pageable;
        return PaginaResponseVO.de(
                motoDAO.findByCilindradasBetween(minimo, maximo, paginacao),
                MotoMapper::toResponse);
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ValidacaoException("O campo '" + campo + "' é obrigatório.");
        }
        return valor.trim();
    }
}
