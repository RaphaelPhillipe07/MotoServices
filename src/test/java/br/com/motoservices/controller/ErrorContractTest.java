package br.com.motoservices.controller;

import br.com.motoservices.bo.ClienteBO;
import br.com.motoservices.bo.OrdemDeServicoBO;
import br.com.motoservices.exception.ValidacaoException;
import br.com.motoservices.vo.ClienteRequestVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ClienteController.class, OrdemDeServicoController.class})
class ErrorContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClienteBO clienteBO;

    @MockBean
    private OrdemDeServicoBO ordemDeServicoBO;

    @Test
    @DisplayName("Bean Validation devolve ProblemDetail com lista de erros por campo")
    void camposInvalidosNoCorpo() throws Exception {
        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"cpfCnpj\":\"52998224725\",\"contato\":\"119\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:motoservices:problem:validacao"))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/clientes"))
                .andExpect(jsonPath("$.errors[0].field").value("nome"))
                .andExpect(jsonPath("$.errors[0].message").value("nome é obrigatório"));
    }

    @Test
    @DisplayName("JSON malformado retorna 400 com mensagem estável")
    void jsonMalformado() throws Exception {
        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.detail").value("Corpo da requisição inválido ou malformado."));
    }

    @Test
    @DisplayName("parâmetro obrigatório ausente retorna 400 identificando o nome")
    void parametroAusente() throws Exception {
        mockMvc.perform(get("/clientes"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Parâmetro obrigatório ausente: nomeOuCpf."));
    }

    @Test
    @DisplayName("tipo de parâmetro inválido retorna 400")
    void tipoDeParametroInvalido() throws Exception {
        mockMvc.perform(get("/ordens-servico/abc/orcamento"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Parâmetro inválido: osId."));
    }

    @Test
    @DisplayName("exceção de domínio 'ValidacaoException' vira 400")
    void validacaoDeDominio() throws Exception {
        when(clienteBO.cadastrar(any(ClienteRequestVO.class)))
                .thenThrow(new ValidacaoException("CPF/CNPJ inválido."));

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria\",\"cpfCnpj\":\"52998224725\",\"contato\":\"119\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:motoservices:problem:validacao"))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value("CPF/CNPJ inválido."));
    }

    @Test
    @DisplayName("violação de integridade vira 409 sem vazar SQL")
    void violacaoDeIntegridade() throws Exception {
        when(clienteBO.cadastrar(any(ClienteRequestVO.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria\",\"cpfCnpj\":\"52998224725\",\"contato\":\"119\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:motoservices:problem:conflito"))
                .andExpect(jsonPath("$.title").value("Conflito de estado"))
                .andExpect(jsonPath("$.detail")
                        .value("Operação viola uma restrição de integridade dos dados."));
    }

    @Test
    @DisplayName("erro inesperado vira 500 genérico, sem detalhes internos")
    void erroInesperado() throws Exception {
        when(clienteBO.cadastrar(any(ClienteRequestVO.class)))
                .thenThrow(new IllegalStateException("detalhe interno sensível"));

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria\",\"cpfCnpj\":\"52998224725\",\"contato\":\"119\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("urn:motoservices:problem:erro-interno"))
                .andExpect(jsonPath("$.title").value("Erro interno"))
                .andExpect(jsonPath("$.detail").value("Erro interno inesperado."));
    }

    @Test
    @DisplayName("método HTTP não suportado retorna 405")
    void metodoNaoSuportado() throws Exception {
        mockMvc.perform(delete("/clientes"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.title").value("Método não suportado"));
    }
}
