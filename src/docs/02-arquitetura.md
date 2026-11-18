# 02 — Arquitetura

## Visão de alto nível

```text
┌──────────────────────────────┐        HTTP/JSON        ┌────────────────────────────────────┐
│ SPA React (Vite)             │ ──────────────────────► │ Spring Boot                        │
│ :5173 (dev, proxy /api)      │ ◄────────────────────── │ :8080                              │
│ :4173 (preview)              │  application/json       │                                    │
└──────────────────────────────┘  problem+json           │ controller → bo → dao → PostgreSQL │
                                                         │       (VO)      (entidade JPA)     │
                                                         └────────────────────────────────────┘
```

O backend é um monólito Spring Boot. Não há mensageria, cache distribuído ou chamadas a serviços
externos. O frontend é uma SPA estática que conversa apenas com esta API. A autenticação é
stateless, com JWT HS256 emitido pela própria aplicação e validado pelo Spring Security.

## Camadas e pacotes

Pacote base: `br.com.motoservices`.

| Pacote | Responsabilidade | Principais classes |
|---|---|---|
| `controller` | Contrato HTTP: rotas, validação de entrada declarativa, status, `Location`, OpenAPI. Sem regra de negócio. | `ClienteController`, `MotoController`, `PecaController`, `EstoqueController`, `OrdemDeServicoController` |
| `bo` | Regras e orquestração; fronteira transacional; busca entidades e valida regras; devolve VOs ou entidades para o controller. | `ClienteBO`, `MotoBO`, `EstoqueBO`, `OrdemDeServicoBO` |
| `dao` | Interfaces `JpaRepository` + queries JPQL. Única camada que fala com o banco. | `ClienteDAO`, `MotoDAO`, `PecaDAO`, `OrdemDeServicoDAO`, `HistoricoStatusOSDAO` |
| `model` | Entidades JPA, enums e regras de domínio puras. | `Cliente`, `Moto`, `Peca`, `OrdemDeServico`, `ServicoDaOficina` (+ subtipos), `HistoricoStatusOS`, `Precificacao`, `StatusOS`, `TipoServico` |
| `vo` | Contratos de entrada (`*RequestVO`) e saída (`*ResponseVO`), paginação e orçamento. | `ClienteRequestVO`, `OrdemDeServicoResponseVO`, `PaginaResponseVO`, `OrcamentoResponseVO` |
| `mapper` | Conversão estática entidade ↔ VO. Não acessa banco. | `ClienteMapper`, `MotoMapper`, `PecaMapper`, `OrdemDeServicoMapper`, `HistoricoStatusMapper` |
| `exception` | Exceções de domínio + `@RestControllerAdvice` que as traduz para RFC 9457. | `GlobalExceptionHandler`, `ClienteNaoEncontradoException`, `EstoqueInsuficienteException`, `TransicaoStatusInvalidaException` |
| `security` | Login, JWT, BCrypt, regras por papel, handlers 401/403 e bootstrap do administrador. | `SecurityConfig`, `JwtService`, `UsuarioDetalhesService`, `AdminBootstrap` |
| `util` | Utilitários sem estado. | `Documento` (CPF/CNPJ) |
| `config` | Beans de infraestrutura. | `OpenApiConfig` |

### Diagrama de dependências

```text
controller ──► bo ──► dao ──► Spring Data JPA ──► PostgreSQL
     │          │       │
     │          ├──► model ◄── mapper
     │          └──► vo
     └──► vo / exception
```

Regras práticas:

- `controller` nunca importa `dao` nem entidades JPA (pode usar enums de `model`, como `StatusOS`):
  só BO, VOs e exceções.
- `bo` pode devolver entidade para outro BO (ex.: `OrdemDeServicoBO` usa `MotoBO.encontrarMoto`),
  mas o controller sempre recebe VO.
- `model` não depende de `controller`, `bo`, `dao`, `mapper` nem de `vo`.
- `Precificacao` é a **única fonte** das fórmulas; subtipos de serviço delegam a ela.

## Ciclo de uma requisição

Exemplo: `POST /ordens-servico/1/servicos`.

1. **Tomcat** desserializa o JSON em `ServicoRequestVO` (`@RequestBody`) e valida Bean Validation.
   Falha aqui → `MethodArgumentNotValidException` → 400 `problem+json`.
2. **Controller** (`OrdemDeServicoController.adicionarServico`) chama o BO e devolve `200` com
   `OrdemDeServicoResponseVO`.
3. **BO** (`OrdemDeServicoBO.adicionarServico`) abre a transação, carrega a OS (`encontrarOS`),
   monta o subtipo de serviço correto, valida regras e estoque, associa à OS e salva.
4. **DAO** resolve as entidades e persiste/consulta via Hibernate.
5. **Mapper** converte a entidade em VO **dentro da transação** — importante porque
   `spring.jpa.open-in-view` está `false`; acessar lazy loading fora dela falharia.
6. **`GlobalExceptionHandler`** só entra em cena em exceções; respostas de sucesso não passam por ele.
7. **Spring Security** valida o Bearer token antes do controller. Token ausente/inválido → 401;
   papel sem permissão → 403, ambos em `problem+json`.

## Transações

- Todo método público de BO que lê ou grava dados é anotado com `@Transactional` ou
  `@Transactional(readOnly = true)`.
- Controllers não são transacionais.
- `OrdemDeServicoBO.confirmarOS` executa a mudança de status e a baixa de estoque na mesma
  transação: se a baixa falhar, o status não é commitado.
- `fecharOS` calcula, grava o snapshot, transiciona `CONCLUIDA → FECHADA` e agenda a revisão do
  cliente na mesma transação.
- `EstoqueBO.baixarEstoque` usa um `UPDATE` atômico no banco (ver
  [05 — Banco de dados](05-banco-de-dados.md#concorrência)).

## Decisões de projeto e seus motivos

| Decisão | Motivo | Consequência |
|---|---|---|
| Rest controllers como camada VIEW | O projeto veio de um modelo acadêmico `VIEW → BO → DAO`, mas a interface é REST (e a SPA é a VIEW real). | Não existe pacote `view`; a tradução de protocolo fica em `controller`. |
| BO em vez de `@Service` genérico | Mantém o vocabulário do domínio e o limite transacional explícito. | Nomes de métodos falam a linguagem do negócio (`calcularOrcamento`, `confirmarOS`). |
| Entidades não são expostas | Evita acoplamento do contrato ao schema e problemas de lazy loading. | Toda resposta passa por mapper; novas colunas exigem atualizar o VO. |
| `ddl-auto: validate` + Flyway | O schema é versionado e o Hibernate apenas confere se as entidades batem. | Entidade e migração precisam evoluir juntas; erro de boot se divergirem. |
| Herança JPA `JOINED` para serviços | `REVISAO` e `TROCA_PECA` compartilham dados e têm campos próprios. | Duas tabelas filhas; consultas podem exigir joins. |
| Snapshot do orçamento na OS | Preço de peça pode mudar depois; o cliente deve pagar o valor acordado. | Colunas `total_*` nulas até o fechamento. |
| Preço calculado em memória | Fórmulas simples e testáveis sem banco; snapshot garante estabilidade. | `GET /orcamento` pode mudar antes do fechamento se peças/serviços mudarem. |
| Erros como `ProblemDetail` | Contrato único e consumível pela SPA. | `errors[]` extra em validações de campo. |
| JWT stateless em vez de sessão | Não exige estado no servidor nem sessão compartilhada; a SPA envia `Authorization` em cada chamada. | Logout é local; revogação exige expiração curta ou lista de bloqueio (roadmap). |
| Autorização por rota no `SecurityFilterChain` | Regras visíveis em um único ponto e testáveis sem anotações espalhadas. | Novos recursos exigem revisar a matriz de papéis. |
| CORS restrito por origem | O proxy do Vite resolve em dev; origens são configuráveis para deploy direto. | Em produção, prefira mesmo domínio/proxy reverso. |

## Fronteiras que você deve respeitar ao alterar código

1. Regra de negócio nova → `bo` (ou `model`, se for cálculo puro reutilizável).
2. Novo campo de API → `vo` + `mapper` (+ `model`/migração se persistir).
3. Novo filtro/consulta → `dao` (com query explícita se derivada não bastar) + `bo` + `controller`.
4. Novo tipo de erro de domínio → exceção em `exception` + mapeamento no `GlobalExceptionHandler`.
5. Novo endpoint → `controller` + `bo`; documentar com anotações OpenAPI e atualizar
   [04 — API REST](04-api-rest.md).
