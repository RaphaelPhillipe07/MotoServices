package br.com.motoservices.controller;

import br.com.motoservices.bo.ClienteBO;
import br.com.motoservices.bo.MotoBO;
import br.com.motoservices.vo.ClienteVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @DisplayName("cliente válido é encaminhado ao BO e retorna 200")
    void clienteValido() throws Exception {
        when(clienteBO.cadastrar(any(ClienteVO.class)))
                .thenReturn(new ClienteVO(1L, "Maria", "52998224725", "119", null));

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria\",\"cpfCnpj\":\"52998224725\",\"contato\":\"119\"}"))
                .andExpect(status().isOk());
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
