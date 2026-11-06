package br.com.motoservices.bo;

import br.com.motoservices.dao.MotoDAO;
import br.com.motoservices.exception.MotoInvalidaException;
import br.com.motoservices.exception.ValidacaoException;
import br.com.motoservices.model.Moto;
import br.com.motoservices.vo.MotoRequestVO;
import br.com.motoservices.vo.MotoResponseVO;
import br.com.motoservices.vo.PaginaResponseVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MotoBOTest {

    @Mock
    private MotoDAO motoDAO;

    @InjectMocks
    private MotoBO motoBO;

    @Test
    @DisplayName("encontrar moto normaliza o texto e delega ao catálogo")
    void encontrarMotoNormaliza() {
        Moto moto = new Moto(1L, "Honda", "CG 160", 160);
        when(motoDAO.findByMarcaIgnoreCaseAndModeloIgnoreCase("Honda", "CG 160"))
                .thenReturn(Optional.of(moto));

        assertEquals(moto, motoBO.encontrarMoto("  Honda ", " CG 160 "));
    }

    @Test
    @DisplayName("moto fora do catálogo lança MotoInvalidaException")
    void motoForaDoCatalogo() {
        when(motoDAO.findByMarcaIgnoreCaseAndModeloIgnoreCase("Yamaha", "XTZ"))
                .thenReturn(Optional.empty());

        assertThrows(MotoInvalidaException.class, () -> motoBO.encontrarMoto("Yamaha", "XTZ"));
    }

    @Test
    @DisplayName("cadastro exige cilindradas positivas")
    void cadastroSemCilindradas() {
        MotoRequestVO vo = new MotoRequestVO("Honda", "Biz", 0);
        assertThrows(ValidacaoException.class, () -> motoBO.cadastrar(vo));
    }

    @Test
    @DisplayName("não permite duplicar marca/modelo no catálogo")
    void cadastroDuplicado() {
        MotoRequestVO vo = new MotoRequestVO("Honda", "CG 160", 160);
        when(motoDAO.findByMarcaIgnoreCaseAndModeloIgnoreCase("Honda", "CG 160"))
                .thenReturn(Optional.of(new Moto(1L, "Honda", "CG 160", 160)));

        assertThrows(ValidacaoException.class, () -> motoBO.cadastrar(vo));
        verify(motoDAO, never()).save(any());
    }

    @Test
    @DisplayName("cadastro válido devolve VO com id")
    void cadastroValido() {
        MotoRequestVO vo = new MotoRequestVO("Honda", "CG 160", 160);
        when(motoDAO.findByMarcaIgnoreCaseAndModeloIgnoreCase("Honda", "CG 160"))
                .thenReturn(Optional.empty());
        when(motoDAO.save(any(Moto.class))).thenAnswer(invocation -> {
            Moto moto = invocation.getArgument(0);
            moto.setId(7L);
            return moto;
        });

        MotoResponseVO salvo = motoBO.cadastrar(vo);

        assertEquals(7L, salvo.getId());
        assertEquals(160, salvo.getCilindradas());
    }

    @Test
    @DisplayName("filtro rejeita faixa invertida")
    void filtroInvertido() {
        Pageable pageable = PageRequest.of(0, 20);
        assertThrows(ValidacaoException.class, () -> motoBO.filtrarPorCilindradas(500, 100, pageable));
        verify(motoDAO, never()).findByCilindradasBetween(500, 100, pageable);
    }

    @Test
    @DisplayName("filtro devolve VOs mapeados")
    void filtroValido() {
        Pageable pageable = PageRequest.of(0, 20);
        when(motoDAO.findByCilindradasBetween(150, 300, pageable))
                .thenReturn(new PageImpl<>(List.of(new Moto(1L, "Honda", "CG 160", 160))));

        PaginaResponseVO<MotoResponseVO> resultado =
                motoBO.filtrarPorCilindradas(150, 300, pageable);

        assertEquals(1, resultado.getContent().size());
        assertEquals("CG 160", resultado.getContent().get(0).getModelo());
    }
}
