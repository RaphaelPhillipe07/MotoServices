package br.com.motoservices.integration;

import br.com.motoservices.dao.UsuarioDAO;
import br.com.motoservices.model.Usuario;
import br.com.motoservices.model.enums.Papel;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test ponta a ponta contra PostgreSQL real: cadastros, ciclo completo da OS,
 * cancelamento com estorno, pagamento, comprovante, relatórios e agenda de revisões.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FluxoCompletoIntegrationTest {

    private static final EmbeddedPostgres POSTGRES;

    static {
        try {
            POSTGRES = EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @AfterAll
    static void encerrar() throws IOException {
        POSTGRES.close();
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private UsuarioDAO usuarioDAO;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void usarClienteHttpSemRetryDeAutenticacao() {
        // O cliente HTTP padrão (HttpURLConnection) lança HttpRetryException ao receber
        // 401 com WWW-Authenticate em um POST com corpo. O cliente do JDK não tem esse problema.
        rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    @DisplayName("sem token, recurso protegido retorna 401 em RFC 9457")
    void semTokenRetorna401() {
        ResponseEntity<String> resposta = rest.getForEntity("/clientes", String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, resposta.getStatusCode());
        assertTrue(resposta.getHeaders().getContentType()
                .isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertTrue(resposta.getBody().contains("urn:motoservices:problem:nao-autenticado"));
        assertTrue(resposta.getBody().contains("\"title\":\"Não autenticado\""));
    }

    @Test
    @DisplayName("login inválido retorna 401 sem revelar o campo errado")
    void loginInvalidoRetorna401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> resposta = rest.exchange("/auth/login", HttpMethod.POST,
                new HttpEntity<>(Map.of("username", "admin", "senha", "senha-errada"), headers),
                Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, resposta.getStatusCode());
        assertEquals("Usuário ou senha inválidos.", resposta.getBody().get("detail"));
    }

    @Test
    @DisplayName("perfil MECANICO recebe 403 em relatórios")
    void mecanicoRecebe403EmRelatorios() {
        if (usuarioDAO.findByUsername("mecanico").isEmpty()) {
            usuarioDAO.save(new Usuario(null, "mecanico", "Mecânico de Teste",
                    passwordEncoder.encode("mecanico123"), Papel.MECANICO, true));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> login = rest.exchange("/auth/login", HttpMethod.POST,
                new HttpEntity<>(Map.of("username", "mecanico", "senha", "mecanico123"), headers),
                Map.class);
        assertEquals(HttpStatus.OK, login.getStatusCode());
        String tokenMecanico = (String) login.getBody().get("accessToken");

        HttpHeaders autorizacao = new HttpHeaders();
        autorizacao.setBearerAuth(tokenMecanico);
        ResponseEntity<String> resposta = rest.exchange("/relatorios/faturamento?inicio=2026-01-01&fim=2026-12-31",
                HttpMethod.GET, new HttpEntity<>(autorizacao), String.class);

        assertEquals(HttpStatus.FORBIDDEN, resposta.getStatusCode());
        assertTrue(resposta.getBody().contains("urn:motoservices:problem:acesso-negado"));
    }

    @Test
    @DisplayName("admin autenticado consulta o próprio perfil")
    void adminConsultaProprioPerfil() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token());
        ResponseEntity<Map> resposta = rest.exchange("/auth/me", HttpMethod.GET,
                new HttpEntity<>(headers), Map.class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals("admin", resposta.getBody().get("username"));
        assertEquals("ADMIN", resposta.getBody().get("papel"));
    }

    @Test
    @DisplayName("fluxo completo: OS, cancelamento, pagamento, comprovante e relatórios")
    void fluxoCompleto() {
        Long clienteId = id(post("/clientes", Map.of(
                "nome", "Maria Silva",
                "cpfCnpj", "52998224725",
                "contato", "11999999999")));
        Long motoId = id(post("/motos", Map.of(
                "marca", "Honda", "modelo", "CG 160", "cilindradas", 160)));
        Long pecaId = id(post("/pecas", Map.of(
                "codigo", "P-001", "descricao", "Pastilha de freio",
                "valor", 120.00, "quantidadeEstoque", 10, "estoqueMinimo", 2)));

        Long osCancelada = id(post("/ordens-servico", Map.of(
                "clienteId", clienteId, "marcaMoto", "Honda", "modeloMoto", "CG 160")));
        post("/ordens-servico/" + osCancelada + "/servicos", Map.of(
                "tipo", "TROCA_PECA", "descricao", "Troca de pastilha",
                "valorBaseMaoDeObra", 80.00, "pecaId", pecaId, "pecaFornecidaPelaLoja", true));
        assertEquals(10, ((Number) get("/pecas/" + pecaId).get("quantidadeEstoque")).intValue());

        post("/ordens-servico/" + osCancelada + "/confirmar", null);
        assertEquals(9, ((Number) get("/pecas/" + pecaId).get("quantidadeEstoque")).intValue());
        assertEquals("CANCELADA", post("/ordens-servico/" + osCancelada + "/cancelar", null).get("status"));
        assertEquals(10, ((Number) get("/pecas/" + pecaId).get("quantidadeEstoque")).intValue());

        Long osFechada = id(post("/ordens-servico", Map.of(
                "clienteId", clienteId, "marcaMoto", "Honda", "modeloMoto", "CG 160")));
        post("/ordens-servico/" + osFechada + "/servicos", Map.of(
                "tipo", "REVISAO", "descricao", "Revisão de 10.000 km",
                "tempoEstimado", 60, "valorBaseMaoDeObra", 150.00, "checklistGeral", true));
        post("/ordens-servico/" + osFechada + "/confirmar", null);
        post("/ordens-servico/" + osFechada + "/fechar", Map.of("formaPagamento", "PIX"));

        Map<String, Object> os = get("/ordens-servico/" + osFechada);
        assertEquals("FECHADA", os.get("status"));
        assertEquals("PIX", os.get("formaPagamento"));
        assertNotNull(os.get("totalFinal"));

        ResponseEntity<String> comprovante = rest.exchange(
                "/ordens-servico/" + osFechada + "/comprovante", HttpMethod.GET,
                new HttpEntity<>(autorizacao()), String.class);
        assertEquals(HttpStatus.OK, comprovante.getStatusCode());
        assertTrue(comprovante.getHeaders().getContentType().isCompatibleWith(MediaType.TEXT_HTML));
        assertTrue(comprovante.getBody().contains("Comprovante interno"));
        assertTrue(comprovante.getBody().contains("PIX"));

        String hoje = LocalDate.now().toString();
        Map<String, Object> faturamento = get(
                "/relatorios/faturamento?inicio=" + hoje + "&fim=" + hoje);
        assertEquals(1, ((Number) faturamento.get("quantidadeOs")).intValue());
        assertEquals(0, new java.math.BigDecimal(faturamento.get("totalFinal").toString())
                .compareTo(new java.math.BigDecimal("150.00")));

        List<?> servicos = getList("/relatorios/servicos-mais-realizados?inicio=" + hoje
                + "&fim=" + hoje + "&limite=5");
        assertEquals(1, servicos.size());
        List<?> clientes = getList("/relatorios/clientes-mais-atendidos?inicio=" + hoje
                + "&fim=" + hoje + "&limite=5");
        assertEquals(1, clientes.size());

        ResponseEntity<String> csv = rest.exchange(
                "/relatorios/ordens/exportar?inicio=" + hoje + "&fim=" + hoje, HttpMethod.GET,
                new HttpEntity<>(autorizacao()), String.class);
        assertEquals(HttpStatus.OK, csv.getStatusCode());
        assertTrue(csv.getBody().contains("id;data_abertura;data_fechamento"));
        assertTrue(csv.getBody().contains("PIX"));

        Map<String, Object> revisoes = get("/clientes/revisoes-proximas?dias=365");
        assertTrue(((Number) revisoes.get("totalElements")).intValue() >= 1);

        assertEquals(motoId, ((Number) get("/motos/" + motoId).get("id")).longValue());
    }

    private Map<String, Object> post(String path, Object body) {
        HttpHeaders headers = autorizacaoJson();
        ResponseEntity<Map> resposta = rest.exchange(path, HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);
        assertTrue(resposta.getStatusCode().is2xxSuccessful(), path + " -> " + resposta.getStatusCode());
        return resposta.getBody();
    }

    private Map<String, Object> get(String path) {
        ResponseEntity<Map> resposta = rest.exchange(path, HttpMethod.GET,
                new HttpEntity<>(autorizacao()), Map.class);
        assertTrue(resposta.getStatusCode().is2xxSuccessful(), path + " -> " + resposta.getStatusCode());
        return resposta.getBody();
    }

    private List<?> getList(String path) {
        ResponseEntity<List> resposta = rest.exchange(path, HttpMethod.GET,
                new HttpEntity<>(autorizacao()), List.class);
        assertTrue(resposta.getStatusCode().is2xxSuccessful(), path + " -> " + resposta.getStatusCode());
        return resposta.getBody();
    }

    private HttpHeaders autorizacao() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token());
        return headers;
    }

    private HttpHeaders autorizacaoJson() {
        HttpHeaders headers = autorizacao();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String token() {
        if (token == null) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map> login = rest.exchange("/auth/login", HttpMethod.POST,
                    new HttpEntity<>(Map.of("username", "admin", "senha", "admin123"), headers),
                    Map.class);
            assertEquals(HttpStatus.OK, login.getStatusCode(), "login do administrador inicial");
            token = (String) login.getBody().get("accessToken");
        }
        return token;
    }

    private static long id(Map<String, Object> corpo) {
        return ((Number) corpo.get("id")).longValue();
    }
}
