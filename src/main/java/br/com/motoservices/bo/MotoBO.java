package br.com.motoservices.bo;

import br.com.motoservices.dao.MotoDAO;
import br.com.motoservices.exception.MotoInvalidaException;
import br.com.motoservices.model.Moto;
import br.com.motoservices.vo.MotoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MotoBO {

    private final MotoDAO motoDAO;

    public Moto encontrarMoto(String marca, String modelo) {
        // TODO: delegar para motoDAO.findByMarcaAndModelo(...) e lançar MotoInvalidaException
        throw new UnsupportedOperationException("Implementar validação contra o catálogo.");
    }

    public MotoVO cadastrar(MotoVO motoVO) {
        // TODO: persistir via motoDAO.save(...) e devolver o VO atualizado com id
        throw new UnsupportedOperationException("Implementar cadastro de moto.");
    }

    public List<MotoVO> filtrarPorCilindradas(int minimo, int maximo) {
        // TODO: usar motoDAO.findByCilindradasBetween(...) e mapear para MotoVO
        throw new UnsupportedOperationException("Implementar filtro por cilindradas.");
    }
}
