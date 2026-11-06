package br.com.motoservices.controller;

import br.com.motoservices.bo.OrdemDeServicoBO;
import br.com.motoservices.model.enums.StatusOS;
import br.com.motoservices.vo.HistoricoStatusResponseVO;
import br.com.motoservices.vo.OrdemDeServicoRequestVO;
import br.com.motoservices.vo.OrdemDeServicoResponseVO;
import br.com.motoservices.vo.PaginaResponseVO;
import br.com.motoservices.vo.ServicoRequestVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrdemDeServicoController.class)
class OrdemDeServicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrdemDeServicoBO ordemDeServicoBO;

    private OrdemDeServicoResponseVO os(StatusOS status) {
        return new OrdemDeServicoResponseVO(1L, 1L, "Maria", 1L, "Honda", "CG", 160,
                status, LocalDateTime.now(), null, null, null, null, null);
    }

    @Test
    @DisplayName("abertura de OS retorna 201 com Location")
    void criarRetorna201() throws Exception {
        when(ordemDeServicoBO.criarOS(any(OrdemDeServicoRequestVO.class)))
                .thenReturn(os(StatusOS.ABERTA));

        mockMvc.perform(post("/ordens-servico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":1,\"marcaMoto\":\"Honda\",\"modeloMoto\":\"CG\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/ordens-servico/1"))
                .andExpect(jsonPath("$.status").value("ABERTA"));
    }

    @Test
    @DisplayName("abertura sem clienteId retorna 400")
    void criarSemClienteRetorna400() throws Exception {
        mockMvc.perform(post("/ordens-servico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marcaMoto\":\"Honda\",\"modeloMoto\":\"CG\"}"))
                .andExpect(status().isBadRequest());

        verify(ordemDeServicoBO, never()).criarOS(any(OrdemDeServicoRequestVO.class));
    }

    @Test
    @DisplayName("detalhe da OS retorna 200")
    void buscarPorIdRetorna200() throws Exception {
        when(ordemDeServicoBO.buscarPorId(1L)).thenReturn(os(StatusOS.EM_ANDAMENTO));

        mockMvc.perform(get("/ordens-servico/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.clienteNome").value("Maria"))
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
    }

    @Test
    @DisplayName("histórico por cliente devolve envelope paginado")
    void listarPorCliente() throws Exception {
        when(ordemDeServicoBO.listarPorCliente(eq(1L), any(Pageable.class)))
                .thenReturn(new PaginaResponseVO<>(
                        List.of(os(StatusOS.ABERTA)), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/ordens-servico").param("clienteId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("adição de serviço retorna a OS atualizada")
    void adicionarServico() throws Exception {
        when(ordemDeServicoBO.adicionarServico(eq(1L), any(ServicoRequestVO.class)))
                .thenReturn(os(StatusOS.ABERTA));

        mockMvc.perform(post("/ordens-servico/1/servicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"REVISAO\",\"descricao\":\"Revisão 10.000 km\","
                                + "\"valorBaseMaoDeObra\":100.00,\"checklistGeral\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("serviço sem tipo retorna 400")
    void servicoSemTipoRetorna400() throws Exception {
        mockMvc.perform(post("/ordens-servico/1/servicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Revisão\",\"valorBaseMaoDeObra\":100.00}"))
                .andExpect(status().isBadRequest());

        verify(ordemDeServicoBO, never()).adicionarServico(any(), any(ServicoRequestVO.class));
    }

    @Test
    @DisplayName("confirmação da OS retorna o novo status")
    void confirmarOS() throws Exception {
        when(ordemDeServicoBO.confirmarOS(1L)).thenReturn(os(StatusOS.EM_ANDAMENTO));

        mockMvc.perform(post("/ordens-servico/1/confirmar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
    }

    @Test
    @DisplayName("histórico de status devolve envelope paginado")
    void listarHistorico() throws Exception {
        when(ordemDeServicoBO.listarHistorico(eq(1L), any(Pageable.class)))
                .thenReturn(new PaginaResponseVO<>(
                        List.of(new HistoricoStatusResponseVO(1L, 1L, null, StatusOS.ABERTA,
                                LocalDateTime.now())),
                        0, 20, 1, 1, true, true));

        mockMvc.perform(get("/ordens-servico/1/historico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].statusNovo").value("ABERTA"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
