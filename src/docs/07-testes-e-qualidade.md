# 07 — Testes e qualidade

## Panorama atual

| Suíte | Quantidade | Onde |
|---|---|---|
| Backend (JUnit 5 + Mockito + Spring Test) | **143 testes** | `src/test/java/br/com/motoservices` |
| Frontend (Vitest + Testing Library + axe) | **55 testes** em 13 arquivos | `frontend/src/**/*.test.ts(x)` |
| Gate de qualidade backend | Checkstyle + JaCoCo (≥ 50% de linhas) | `./mvnw verify` |

Os testes de backend se dividem em quatro níveis:

| Nível | Técnica | Exemplos |
|---|---|---|
| Domínio puro | JUnit sem Spring | `PrecificacaoTest`, `OrdemDeServicoTest` |
| BO (regra de negócio) | Mockito para DAOs | `ClienteBOTest`, `MotoBOTest`, `EstoqueBOTest`, `OrdemDeServicoBOTest` |
| Contrato HTTP | `@WebMvcTest` + `MockMvc` + `@MockBean` | `ControllerValidationTest`, `ErrorContractTest`, `OrdemDeServicoControllerTest`, `PecaControllerTest` |
| Integração real | `@SpringBootTest` + PostgreSQL embarcado | `OpenApiDocsTest`, `MigrationsTest`, `FluxoCompletoIntegrationTest` (fluxo autenticado, 401/403) |

`ProfilesConfigTest` lê os YAMLs diretamente e protege o contrato dos perfis `dev`/`prod`.

## Comandos

```bash
# tudo (testes + Checkstyle + JaCoCo) — igual ao CI
./mvnw verify

# apenas testes
./mvnw test

# uma classe
./mvnw test -Dtest=OrdemDeServicoBOTest

# um método
./mvnw test -Dtest='OrdemDeServicoBOTest#testeFecharOS'

# só testes de integração de infraestrutura
./mvnw test -Dtest='MigrationsTest,OpenApiDocsTest'
```

No Windows, use `mvnw.cmd`.

Artefatos gerados:

| Arquivo | Conteúdo |
|---|---|
| `target/surefire-reports/` | Resultado por teste |
| `target/site/jacoco/index.html` | Cobertura HTML por classe/pacote |
| `target/jacoco.exec` | Dados brutos de cobertura |
| `target/checkstyle-result.xml` | Violações de estilo (o build falha se houver) |

## PostgreSQL embarcado

A integração não depende de Docker nem de um banco local:

- Dependência `io.zonky.test:embedded-postgres:2.2.2` (escopo `test`).
- Na primeira execução, o binário do PostgreSQL (14.22) é extraído para um diretório temporário; por
  isso a primeira execução é mais lenta (dezenas de segundos).
- `OpenApiDocsTest` sobe a aplicação em porta aleatória e injeta o datasource com
  `@DynamicPropertySource`; valida Flyway, `ddl-auto: validate`, `/v3/api-docs` e `/actuator/health`.
- `MigrationsTest` sobe um PostgreSQL embarcado, roda o Flyway programaticamente e consulta
  `pg_indexes`, `information_schema` e `pg_extension` para conferir índices, CHECKs, `pg_trgm`,
  colunas e tabelas; também insere dados para provar que CHECK/unique funcionam.

### Ponto de atenção ao criar migração

`MigrationsTest` afirma `assertEquals(8, resultado.migrationsExecuted)`. Ao adicionar uma nova
migração, atualize esse número **e** acrescente as asserções do que ela cria.

## Contratos cobertos pelos testes HTTP

- Validação de Bean Validation com lista `errors[]` (campo + mensagem).
- `201` com `Location` nos cadastros.
- Envelope de paginação.
- `204` em vínculo/desvínculo.
- Códigos `400`, `401`, `403`, `404`, `409` e o formato `application/problem+json`.
- Campos de erro do BO chegando à resposta.
- Login válido/inválido, `/auth/me`, acesso sem token e papel sem permissão (integração real).

Os testes de BO cobrem, entre outros:

- CPF e CNPJ válidos, normalização de pontuação, rejeição de dígito inválido e duplicidade.
- Unicidade de moto case-insensitive e faixa de cilindradas inválida.
- Baixa de estoque atômica, baixa concorrente sem saldo, entrada de estoque.
- Desconto progressivo, fatores de risco de 400/650 cc, peça do cliente vs. da loja.
- Confirmação sem baixa dupla, fechamento com snapshot e agendamento da revisão.
- Transições de status inválidas.

## O que **não** está coberto

| Lacuna | Risco |
|---|---|
| Testes de DAO contra banco real (queries derivadas, `EntityGraph`, paginação) | Regressão de consulta só aparece em runtime. |
| Smoke test E2E da API (cadastro → OS → orçamento → confirmar → fechar) | O fluxo completo é validado manualmente, não no CI. |
| Contrato frontend ↔ backend (schemas/tipos gerados) | Divergência de campo é pega só em teste de UI mockado ou manualmente. |
| Concorrência real com múltiplas threads/transações | O `UPDATE` atômico é testado por unidade, não sob estresse. |
| Carga/performance | Sem metas definidas. |
| OWASP ampliado (XSS, CSRF, injeção, headers) | Login, papéis e 401/403 estão cobertos; faltam testes de abuso e rate limiting. |

Sugestões de evolução, em ordem de retorno:

1. Um `@SpringBootTest` de smoke com `TestRestTemplate` reproduzindo o fluxo de
   [04 — API REST](04-api-rest.md#fluxo-completo-com-curl), incluindo os casos `409`.
2. Testes `@DataJpaTest`/integração para `PecaDAO.baixarEstoque` e `OrdemDeServicoDAO`.
3. Testes de contrato (ex.: gerar tipos TypeScript a partir do OpenAPI e comparar).
4. Testes de concorrência com duas transações confirmando a mesma OS.

## Como adicionar testes

### Regra de negócio (BO)

```java
@ExtendWith(MockitoExtension.class)
class MinhaRegraTest {

    @Mock private ClienteDAO clienteDAO;
    @InjectMocks private ClienteBO clienteBO;

    @Test
    @DisplayName("descreve o comportamento em português")
    void minhaRegra() {
        // arrange: when(...).thenReturn(...)
        // act: chamar o BO
        // assert: valor e/ou verify(...)
    }
}
```

### Contrato HTTP

```java
@WebMvcTest(MeuController.class)
class MeuControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private MeuBO meuBO;

    @Test
    void responde201() throws Exception {
        // mockMvc.perform(post("/rota")...)
        //   .andExpect(status().isCreated())
        //   .andExpect(jsonPath("$.campo").value("valor"));
    }
}
```

### Integração com banco

Copie o padrão de `OpenApiDocsTest`: `@SpringBootTest(webEnvironment = RANDOM_PORT)`, um
`EmbeddedPostgres` estático e `@DynamicPropertySource` registrando URL/usuário/senha. Feche o banco
em `@AfterAll`.

## Checkstyle

Configuração em `config/checkstyle/checkstyle.xml`:

- Sem tabs; comprimento máximo de linha **160**.
- Sem imports não usados, redundantes ou com `*`.
- Chaves obrigatórias em blocos/`if`/`else`.
- Espaçamento consistente ao redor de operadores, lambdas vazias permitidas.
- Convenções de nome: tipos `PascalCase`, métodos/variáveis `camelCase`, constantes `UPPER_SNAKE`.
- `includeTestSourceDirectory: true` — os testes também são verificados.
- `failOnViolation: true` — qualquer violação derruba o `verify`.

## JaCoCo

- Agente preparado antes dos testes; relatório gerado na fase `verify`.
- Gate: **50% de linhas** no nível `BUNDLE` (cobertura mínima global, não por classe).
- O build falha se a cobertura cair abaixo disso. O relatório serve para achar lacunas, não como
  meta de qualidade isolada.

## Integração contínua

`.github/workflows/ci.yml` roda em `push` para `master`/`main` e em pull requests:

**Job `build`** (Ubuntu, JDK 17, cache Maven): `bash ./mvnw -B --no-transfer-progress verify` e
publica `target/site/jacoco` como artefato.

**Job `frontend`** (Ubuntu, Node 22, `working-directory: frontend`): `npm ci`, `npm run build`,
`npm test`.

O CI não sobe PostgreSQL: toda a integração usa o banco embarcado.

## Dicas

- Rode `./mvnw verify` antes de abrir PR; ele cobre estilo, cobertura e integração.
- Para iterar rápido, use `-Dtest=...` e só rode `verify` no fim.
- Testes de integração são os mais lentos (boot do Spring + PostgreSQL embarcado); mantenha-os
  focados em infraestrutura e fluxos, não em detalhes já cobertos por unidade.
- Cada teste tem `@DisplayName` em português descrevendo o comportamento; siga o padrão.
