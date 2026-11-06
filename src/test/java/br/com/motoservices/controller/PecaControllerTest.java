package br.com.motoservices.controller;

import br.com.motoservices.bo.EstoqueBO;
import br.com.motoservices.vo.PaginaResponseVO;
import br.com.motoservices.vo.PecaRequestVO;
import br.com.motoservices.vo.PecaResponseVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PecaController.class)
class PecaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EstoqueBO estoqueBO;

    private PecaResponseVO peca() {
        return new PecaResponseVO(1L, "P-001", "Pastilha", new BigDecimal("80.00"), 10, 3);
    }

    @Test
    @DisplayName("cadastro de peça retorna 201 com Location")
    void cadastroRetorna201() throws Exception {
        when(estoqueBO.cadastrar(any(PecaRequestVO.class))).thenReturn(peca());

        mockMvc.perform(post("/pecas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"P-001\",\"descricao\":\"Pastilha\","
                                + "\"valor\":80.00,\"quantidadeEstoque\":10,\"estoqueMinimo\":3}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/pecas/1"))
                .andExpect(jsonPath("$.codigo").value("P-001"));
    }

    @Test
    @DisplayName("cadastro com valor negativo retorna 400")
    void cadastroInvalidoRetorna400() throws Exception {
        mockMvc.perform(post("/pecas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"P-001\",\"descricao\":\"Pastilha\","
                                + "\"valor\":-1,\"quantidadeEstoque\":10,\"estoqueMinimo\":3}"))
                .andExpect(status().isBadRequest());

        verify(estoqueBO, never()).cadastrar(any());
    }

    @Test
    @DisplayName("listagem paginada devolve envelope")
    void listagemPaginada() throws Exception {
        when(estoqueBO.listar(eq("pastilha"), any(Pageable.class)))
                .thenReturn(new PaginaResponseVO<>(List.of(peca()), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/pecas").param("busca", "pastilha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codigo").value("P-001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("entrada de estoque retorna a peça atualizada")
    void entradaDeEstoque() throws Exception {
        when(estoqueBO.registrarEntrada(1L, 3)).thenReturn(
                new PecaResponseVO(1L, "P-001", "Pastilha", new BigDecimal("80.00"), 13, 3));

        mockMvc.perform(post("/pecas/1/entrada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeEstoque").value(13));
    }

    @Test
    @DisplayName("entrada com quantidade zero retorna 400")
    void entradaInvalida() throws Exception {
        mockMvc.perform(post("/pecas/1/entrada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\":0}"))
                .andExpect(status().isBadRequest());

        verify(estoqueBO, never()).registrarEntrada(anyLong(), anyInt());
    }
}
