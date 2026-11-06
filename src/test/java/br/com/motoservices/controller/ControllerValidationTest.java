package br.com.motoservices.controller;

import br.com.motoservices.bo.ClienteBO;
import br.com.motoservices.bo.MotoBO;
import br.com.motoservices.vo.ClienteRequestVO;
import br.com.motoservices.vo.ClienteResponseVO;
import br.com.motoservices.vo.MotoResponseVO;
import br.com.motoservices.vo.PaginaResponseVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ClienteController.class, MotoController.class})
class ControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClienteBO clienteBO;

    @MockBean
    private MotoBO motoBO;

    @Test
    @DisplayName("cliente sem nome retorna 400 e não chega ao BO")
    void clienteSemNome() throws Exception {
        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"  \",\"cpfCnpj\":\"52998224725\",\"contato\":\"119\"}"))
                .andExpect(status().isBadRequest());

        verify(clienteBO, never()).cadastrar(any());
    }

    @Test
    @DisplayName("cliente sem documento retorna 400")
    void clienteSemDocumento() throws Exception {
        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria\",\"contato\":\"119\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("cliente válido retorna 201 com Location")
    void clienteValidoRetorna201() throws Exception {
        when(clienteBO.cadastrar(any(ClienteRequestVO.class)))
                .thenReturn(new ClienteResponseVO(1L, "Maria", "52998224725", "119", null));

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria\",\"cpfCnpj\":\"52998224725\",\"contato\":\"119\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/clientes/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("busca de cliente por id retorna 200")
    void clientePorId() throws Exception {
        when(clienteBO.buscarPorId(1L))
                .thenReturn(new ClienteResponseVO(1L, "Maria", "52998224725", "119", null));

        mockMvc.perform(get("/clientes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria"));
    }

    @Test
    @DisplayName("busca paginada devolve envelope com metadados")
    void buscaPaginada() throws Exception {
        when(clienteBO.buscar(eq("Maria"), any(Pageable.class)))
                .thenReturn(new PaginaResponseVO<>(
                        List.of(new ClienteResponseVO(1L, "Maria", "52998224725", "119", null)),
                        0, 20, 1, 1, true, true));

        mockMvc.perform(get("/clientes").param("nomeOuCpf", "Maria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("Maria"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("vincular moto ao cliente retorna 204")
    void vincularMoto() throws Exception {
        mockMvc.perform(post("/clientes/1/motos/2"))
                .andExpect(status().isNoContent());

        verify(clienteBO).vincularMoto(1L, 2L);
    }

    @Test
    @DisplayName("desvincular moto do cliente retorna 204")
    void desvincularMoto() throws Exception {
        mockMvc.perform(delete("/clientes/1/motos/2"))
                .andExpect(status().isNoContent());

        verify(clienteBO).desvincularMoto(1L, 2L);
    }

    @Test
    @DisplayName("listar motos do cliente devolve envelope")
    void listarMotosDoCliente() throws Exception {
        when(clienteBO.listarMotos(eq(1L), any(Pageable.class)))
                .thenReturn(new PaginaResponseVO<>(
                        List.of(new MotoResponseVO(2L, "Honda", "CG 160", 160)),
                        0, 20, 1, 1, true, true));

        mockMvc.perform(get("/clientes/1/motos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].modelo").value("CG 160"));
    }

    @Test
    @DisplayName("moto sem cilindradas retorna 400")
    void motoSemCilindradas() throws Exception {
        mockMvc.perform(post("/motos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marca\":\"Honda\",\"modelo\":\"CG 160\"}"))
                .andExpect(status().isBadRequest());

        verify(motoBO, never()).cadastrar(any());
    }

    @Test
    @DisplayName("moto com cilindradas negativas retorna 400")
    void motoComCilindradasNegativas() throws Exception {
        mockMvc.perform(post("/motos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marca\":\"Honda\",\"modelo\":\"CG 160\",\"cilindradas\":-1}"))
                .andExpect(status().isBadRequest());
    }
}
