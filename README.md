<p align="center">
  <img src="assets/logo-horizontal.png" alt="MotoServices" width="550"/>
</p>

<p align="center">
  <strong>Sistema de gestão de ordens de serviço exclusivo para oficinas de motocicletas.</strong><br/>
  Unifique venda de peças, mão de obra e histórico de revisões — com precificação dinâmica baseada no modelo da moto.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/status-em%20desenvolvimento-yellow" alt="Status"/>
  <img src="https://img.shields.io/badge/backend-Java%20%2B%20Spring%20Boot-6DB33F" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/db-PostgreSQL%20%2B%20JPA-336791" alt="PostgreSQL"/>
  <img src="https://img.shields.io/badge/migrations-Flyway-CC0200?logo=flyway&logoColor=white" alt="Flyway"/>
  <img src="https://img.shields.io/badge/build-Maven-C71A36?logo=apachemaven&logoColor=white" alt="Maven"/>
  <img src="https://img.shields.io/badge/testes-JUnit%205-25A162" alt="JUnit"/>
  <img src="https://img.shields.io/badge/license-MIT-blue" alt="License"/>
</p>

---

> 📚 **Documentação técnica detalhada:** [`src/docs/`](src/docs/README.md) — arquitetura, domínio,
> API, banco, execução, testes, frontend e guia do desenvolvedor.

## Visão Geral

O **MotoServices** é um sistema de gestão de ordens de serviço (OS) para oficinas de motocicletas. Ele unifica o fluxo de venda de peças avulsas, instalação e prestação de serviços técnicos (revisões), sendo alimentado por uma base de dados dinâmica para motos (marcas, modelos, cilindradas). O sistema também conta com cadastro de cliente para acompanhamento de revisões futuras.

O sistema automatiza esse fluxo, separando o que é **venda de produto** do que é **mão de obra**, e registra o orçamento final no fechamento de cada OS. A emissão de comprovante fiscal (NF) ainda **não está implementada** — está no roadmap.

## Objetivos

**Geral:** Gerenciar e orçar serviços de manutenção, revisões, vendas de peças e substituição de peças para motocicletas, garantindo rastreabilidade, consistência de estoque e histórico do cliente.

**Específicos:**

- Encontrar e validar marca e modelo das motos a partir de base dinâmica.
- Calcular o custo da mão de obra de forma dinâmica com base em cilindradas e modelo.
- Aplicar regras de desconto progressivo (≥ 5 peças da loja = 5% sobre o total de peças).
- Diferenciar o cálculo quando a peça é fornecida pela loja ou trazida pelo cliente.
- Realizar cadastro do cliente para acompanhamento de revisões posteriores.
- Controlar estoque de peças com baixa automática e alertas de ruptura.
- *(Roadmap)* Emitir comprovante de serviço (NF) ao fechar uma OS.
- Agendar a próxima revisão do cliente ao fechar uma OS (notificação por calendário ainda no roadmap).

---

## Atores

| Ator | Responsabilidade |
|---|---|
| **Mecânico / Chefe de Oficina** | Cria a OS, identifica a moto, diagnostica e insere os serviços — informando se as peças saem do estoque da loja ou são fornecidas pelo cliente. |
| **Atendente / Caixa** | Consulta a OS finalizada e confere o orçamento congelado. O registro da forma de pagamento e a emissão do comprovante (NF) estão no roadmap. |
| **Cliente** | Acompanha a OS e as revisões periódicas. O recebimento da NF está no roadmap. |
| **Administrador** | Gerencia cadastro de peças e estoque e registra novas motos na base. Tabelas de preço e relatórios gerenciais estão no roadmap. |

---

## Features

### Precificação Dinâmica de Serviços e Peças
Motor de cálculo de orçamento que diferencia tipo de serviço, origem da peça e perfil da moto. Cobre orçamentos de revisão com fator de risco por cilindradas, troca de peças com ou sem peça da loja, desconto progressivo por volume e exibição consolidada do resumo.

### Gestão de Ordens de Serviço (OS)
Ciclo de vida completo de uma OS — da abertura ao fechamento, com histórico de transições e snapshot do orçamento no fechamento. Inclui múltiplos serviços por OS, edição/remoção de serviço, pagamento no fechamento e o fluxo `ABERTA → EM_ANDAMENTO → AGUARDANDO_CLIENTE → CONCLUÍDA → FECHADA`, com cancelamento até `AGUARDANDO_CLIENTE` e estorno automático de estoque.

### Comprovante Interno e Pagamento
O fechamento registra a forma de pagamento (`PIX`, dinheiro, cartões, boleto ou outro) e congela o orçamento. `GET /ordens-servico/{id}/comprovante` gera um HTML imprimível com cliente, moto, serviços, totais e pagamento — sem validade fiscal; a NF fiscal segue no roadmap.

### Controle de Estoque de Peças
Inventário com cadastro, baixa automática ao confirmar OS e alertas de ruptura. Lança `EstoqueInsuficienteException` quando não há peças disponíveis e registra entradas por compra do administrador.

### Cadastro de Clientes e Histórico de Revisões
Relacionamento com o cliente: cadastro com CPF/CNPJ, vinculação de motos, histórico completo de OSs e calendário de próximas revisões. O vínculo cliente↔moto é criado automaticamente na primeira OS (regra permissiva) e também pode ser gerenciado por `POST/DELETE /clientes/{id}/motos/{motoId}`. Busca por nome, CPF ou CNPJ.

### Catálogo de Motos (Marcas e Modelos)
Base de dados dinâmica com marcas, modelos, cilindradas. Valida a moto ao criar a OS (`MotoInvalidaException` se não encontrada) e expõe filtro por faixa de cilindradas em `GET /motos/filtrar?minimo=1&maximo=2000` — os dois parâmetros são obrigatórios, inclusivos e a faixa precisa ser válida (`minimo > 0` e `maximo >= minimo`). Filtro por valor/preço está no roadmap.

### Relatórios e Faturamento
Faturamento por período de **fechamento**, ticket médio, peças, mão de obra, desconto, serviços mais realizados, clientes com mais atendimentos e exportação CSV das OS fechadas. Todos os valores usam o snapshot gravado na OS, não o preço atual das peças.

---

## Estado de implementação

| Área | Situação |
|---|---|
| Catálogo de motos, clientes e vínculo cliente↔moto | ✅ Implementado |
| Estoque de peças, entrada e alerta de ruptura | ✅ Implementado |
| Ordens de serviço: abertura, serviços, orçamento, confirmação, cancelamento, fechamento e histórico | ✅ Implementado |
| Pagamento no fechamento e comprovante interno imprimível | ✅ Implementado |
| Relatórios e faturamento por período + CSV | ✅ Implementado |
| Precificação dinâmica (fator de risco + desconto progressivo) | ✅ Implementado |
| API documentada em OpenAPI/Swagger + erros RFC 9457 | ✅ Implementado |
| Frontend React (landing, painel, clientes, motos, estoque, OS e relatórios) | ✅ Implementado |
| Login JWT, sessão no frontend e autorização por papéis (ADMIN, ATENDENTE, MECANICO) | ✅ Implementado |

---

## Stack Tecnológica

| Camada | Tecnologia | Por quê |
|---|---|---|
| **Linguagem** | Java 17 | Tipagem forte, ecossistema robusto para sistemas corporativos. |
| **Framework** | Spring Boot 3 | Configuração mínima, injeção de dependência e suporte nativo a REST. |
| **Persistência** | PostgreSQL + Spring Data JPA + Hibernate | ORM maduro com suporte a herança de entidades. |
| **Migrações (DB)**| Flyway | Controle de versão do esquema do banco de dados (scripts em `db/migration`). |
| **Build** | Maven | Gerenciamento de dependências e ciclo de build padronizado. |
| **Testes** | JUnit 5 + Mockito + PostgreSQL embarcado | Testes unitários, slice MVC e integração com Flyway/mapeamento JPA sem depender de Docker. |
| **Documentação API** | SpringDoc OpenAPI (Swagger UI) | Contrato gerado em `/swagger-ui.html` com erros RFC 9457 documentados. |
| **Validação** | Jakarta Bean Validation + domínio | Constraints nos VOs de entrada; CPF/CNPJ validados por dígito verificador no `ClienteBO`. |
| **Segurança** | Spring Security + JWT HS256 + BCrypt | Login stateless, token Bearer e permissões por papel; senhas nunca em texto puro. |
| **Frontend** | React 19 + TypeScript + Vite + Tailwind 4 | UI acessível com primitivos Radix, tokens de cor/tipografia e estados completos. |
| **Qualidade** | Checkstyle + JaCoCo + GitHub Actions | Lint, gate de cobertura e build contínuo (`.github/workflows/ci.yml`). |
| **Execução** | Maven Wrapper + Docker Compose | Reproduzível sem Maven instalado; stack completa com `docker compose up`. |

---

## Como executar

### Requisitos
- JDK 17+
- PostgreSQL 14+ **ou** Docker

O build usa o **Maven Wrapper** — não é necessário ter Maven instalado.

### Local (perfil `dev`)

```bash
# banco local (uma vez)
createdb motoservices

# opcionais — padrões entre parênteses
export DB_HOST=localhost DB_PORT=5432 DB_NAME=motoservices DB_USER=postgres DB_PASS=postgres

./mvnw spring-boot:run
```

As migrações Flyway (V1–V8) rodam automaticamente na inicialização.

Na primeira subida com a tabela de usuários vazia, o backend cria o administrador
`admin` com a senha `admin123` (apenas no perfil `dev`; sobrescreva com
`ADMIN_USERNAME`/`ADMIN_PASSWORD`). O painel fica em `/entrar` no frontend.

| Recurso | URL |
|---|---|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health | http://localhost:8080/actuator/health |

> **Windows:** use `mvnw.cmd` no lugar de `./mvnw` (ex.: `mvnw.cmd spring-boot:run`).

### Autenticação e papéis

Exceto `POST /auth/login`, `/actuator/health` e a documentação OpenAPI, todas as rotas exigem
`Authorization: Bearer <token>`. O token é um JWT HS256 emitido pela própria API, com validade
padrão de 60 minutos (`JWT_EXPIRATION_MINUTES`).

| Papel | Pode |
|---|---|
| `ADMIN` | Tudo, incluindo cadastro/entrada de peças, fechamento e relatórios. |
| `ATENDENTE` | Clientes, motos, OS, fechamento/cancelamento e relatórios. Não cadastra peças nem dá entrada em estoque. |
| `MECANICO` | Consulta e opera OS (abrir, adicionar serviço, confirmar, alterar status), sem fechar/cancelar nem acessar relatórios. |

Variáveis de ambiente de segurança:

| Variável | Default (dev) | Obrigatória em `prod` | Uso |
|---|---|---|---|
| `JWT_SECRET` | segredo local de 32+ caracteres | **sim** | Assinatura/validação HS256 |
| `JWT_EXPIRATION_MINUTES` | `60` | não | Validade do token |
| `ADMIN_USERNAME` | `admin` | não | Usuário criado quando a tabela está vazia |
| `ADMIN_PASSWORD` | `admin123` | recomendada | Senha em texto puro convertida em BCrypt no boot |
| `ADMIN_BOOTSTRAP` | `true` | não | Desliga a criação automática do administrador |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:4173` | conforme proxy | Origens autorizadas |

### API (resumo)

Todas as rotas são relativas a `http://localhost:8080`. A documentação interativa fica em `/swagger-ui.html`; a referência completa, com payloads e exemplos, está em [`src/docs/04-api-rest.md`](src/docs/04-api-rest.md).

| Método | Rota | Observações |
|---|---|---|
| `POST` | `/auth/login` | Público; corpo `{"username","senha"}`; devolve `accessToken`, `tokenType`, `expiresIn` e `usuario` |
| `GET` | `/auth/me` | Requer Bearer; devolve o usuário autenticado |
| `POST` | `/clientes` | Valida CPF/CNPJ e impede documento duplicado |
| `GET` | `/clientes?nomeOuCpf=...` | `nomeOuCpf` opcional; sem termo lista todos; documento exato (11/14 dígitos) ou nome (paginado) |
| `GET` | `/clientes/revisoes-proximas?dias=30` | Clientes com revisão na janela de 1–365 dias |
| `GET` | `/clientes/{id}` | `404` se não existir |
| `GET` | `/clientes/{id}/motos` | Paginado |
| `POST` / `DELETE` | `/clientes/{id}/motos/{motoId}` | Vínculo idempotente; responde `204` |
| `POST` | `/motos` | Marca + modelo únicos (case-insensitive) |
| `GET` | `/motos/{id}` | `404` se não existir |
| `GET` | `/motos/filtrar?minimo=1&maximo=2000` | **`minimo` e `maximo` obrigatórios** (inclusivos); `minimo > 0` e `maximo >= minimo`; paginado |
| `POST` | `/pecas` | Código único; valores e saldos não negativos |
| `GET` | `/pecas?busca=...` | `busca` opcional por código ou descrição |
| `GET` | `/pecas/{id}` | `404` se não existir |
| `POST` | `/pecas/{id}/entrada` | Corpo `{"quantidade": N}`; soma ao saldo |
| `GET` | `/estoque/baixo` | Peças com `quantidadeEstoque < estoqueMinimo` |
| `POST` | `/ordens-servico` | Corpo `{"clienteId", "marcaMoto", "modeloMoto"}` |
| `GET` | `/ordens-servico?clienteId=&motoId=&status=` | Filtros opcionais e combináveis; paginado por `dataAbertura` (desc) |
| `GET` | `/ordens-servico/{id}` | OS com serviços |
| `GET` | `/ordens-servico/{id}/historico` | Transições de status (mais recentes primeiro) |
| `POST` | `/ordens-servico/{id}/servicos` | `REVISAO` ou `TROCA_PECA` |
| `PUT` / `DELETE` | `/ordens-servico/{id}/servicos/{servicoId}` | Edita (mesmo tipo) ou remove; estoque é ajustado |
| `GET` | `/ordens-servico/{id}/orcamento` | Totais calculados (não persistidos) |
| `PATCH` | `/ordens-servico/{id}/status` | Transições operacionais até `CONCLUIDA` |
| `POST` | `/ordens-servico/{id}/confirmar` | `ABERTA → EM_ANDAMENTO`; baixa estoque uma vez |
| `POST` | `/ordens-servico/{id}/cancelar` | Cancela até `AGUARDANDO_CLIENTE`; estorna estoque |
| `POST` | `/ordens-servico/{id}/fechar` | Conclui, encerra, registra pagamento e agenda revisão (+6 meses) |
| `GET` | `/ordens-servico/{id}/comprovante` | HTML imprimível (somente OS `FECHADA`, sem validade fiscal) |
| `GET` | `/relatorios/faturamento?inicio=&fim=` | Totais e ticket médio por data de fechamento |
| `GET` | `/relatorios/servicos-mais-realizados?inicio=&fim=&limite=5` | Ranking de serviços |
| `GET` | `/relatorios/clientes-mais-atendidos?inicio=&fim=&limite=5` | Ranking de clientes |
| `GET` | `/relatorios/ordens/exportar?inicio=&fim=` | CSV das OS fechadas |

Paginação: `?page=0&size=20&sort=campo,asc|desc` (máximo de 100 itens por página).

### Docker Compose (perfil `prod`)

```bash
docker compose up --build
```

Sobe PostgreSQL 16 e a aplicação em `http://localhost:8080`. O perfil `prod` exige
`DB_URL`, `DB_USER` e `DB_PASS` (já definidos no compose) e desabilita o Swagger.

### Qualidade

```bash
./mvnw verify
```

Executa a suíte de testes, o **Checkstyle** e o gate de cobertura do **JaCoCo**
(mínimo de 50% de linhas). É o mesmo comando do workflow em `.github/workflows/ci.yml`.

### Frontend (React)

```bash
cd frontend
npm install
npm run dev       # http://localhost:5173 com proxy /api → :8080
npm test          # Vitest + Testing Library + axe
npm run build     # tsc + bundle de produção
```

O frontend tem landing pública em `/`, painel em `/painel` e páginas de clientes, motos, estoque,
ordens de serviço e relatórios. Inclui pagamento no fechamento, comprovante, cancelamento e
remoção de serviço. A acessibilidade é checada com `vitest-axe` e com um teste de contraste
WCAG AA dos tokens de cor.

---

## Estrutura do Repositório

```text
.
├── .github/workflows/ci.yml        # build + testes + lint + cobertura
├── config/checkstyle/              # regras de lint
├── src/main/java/br/com/motoservices/
│   ├── bo/                         # regras de negócio (Cliente, Moto, Estoque, OS)
│   ├── controller/                 # API REST (clientes, motos, peças, OS, estoque)
│   ├── dao/                        # Spring Data JPA
│   ├── exception/                  # exceções de domínio + handler RFC 9457
│   ├── mapper/                     # entidade ↔ request/response
│   ├── model/                      # entidades JPA, enums e Precificacao
│   ├── security/                   # JWT, UserDetailsService, regras e bootstrap do admin
│   ├── util/                       # validação de CPF/CNPJ
│   └── vo/                         # contratos de entrada/saída e paginação
├── src/main/resources/
│   ├── application.yml             # base (perfil dev por padrão)
│   ├── application-dev.yml         # datasource local + SQL formatado
│   ├── application-prod.yml        # credenciais via ambiente, Swagger off
│   └── db/migration/               # V1..V8 (Flyway)
├── src/test/java/                  # unitários, MVC, migrations e OpenAPI
├── src/docs/                       # documentação técnica detalhada (comece por src/docs/README.md)
├── frontend/                       # SPA React + Vite + Tailwind
│   ├── src/api/                    # cliente HTTP tipado + hooks TanStack Query
│   ├── src/components/ui/          # UI kit acessível (Radix + cva)
│   ├── src/features/               # formulários por domínio
│   ├── src/pages/                  # clientes, motos, estoque e OS
│   ├── src/styles/                 # tokens + teste de contraste
│   └── src/test/                   # setup RTL/axe
├── Dockerfile                      # build multi-stage, runtime não-root
├── docker-compose.yml              # PostgreSQL + aplicação
├── mvnw / mvnw.cmd                 # Maven Wrapper
└── pom.xml
```

---

## Estrutura das camadas do projeto

O backend segue uma separação em camadas inspirada no modelo **VIEW → BO → DAO**, adaptada para uma
API REST. A regra central é: **o controller fala VOs, o BO orquestra entidades, o DAO persiste
entidades** e os *mappers* fazem a conversão na borda. VOs não chegam ao DAO e entidades JPA não são
serializadas na resposta HTTP.

```text
┌──────────────────────────────┐
│ VIEW                         │
│ SPA React (frontend/)        │  HTTP/JSON
│ + controllers REST           │ ─────────────────────────┐
└──────────────────────────────┘                          │ RequestVO
                                                          ▼
                                               ┌──────────────────────┐
                                               │ CONTROLLER           │  valida entrada,
                                               │ br.com...controller  │  define status/Location
                                               └──────────┬───────────┘
                                                          │ RequestVO / ResponseVO
                                                          ▼
                                               ┌──────────────────────┐
                                               │ BO                   │  regras de negócio,
                                               │ br.com...bo          │  orçamento, transações
                                               └──────────┬───────────┘
                                                          │ entidades JPA
                                                          ▼
                                               ┌──────────────────────┐
                                               │ DAO                  │  Spring Data JPA
                                               │ br.com...dao         │
                                               └──────────┬───────────┘
                                                          │ JPQL/SQL
                                                          ▼
                                               ┌──────────────────────┐
                                               │ PostgreSQL           │  schema motoservices
                                               │ migrações Flyway     │  tabelas tb_*
                                               └──────────────────────┘

  mapper   (br.com...mapper)   entidade ⇄ VO, sem acesso a banco
  exception(br.com...exception)exceção de domínio → ProblemDetail (RFC 9457)
```

| Camada | Pacote | Responsabilidade | Exemplos |
|---|---|---|---|
| **VIEW** | `frontend/` + `controller` | Interface com o usuário (SPA) e contrato HTTP: rotas, validação declarativa, status, `Location` e OpenAPI. Não calcula nem acessa o banco. | `OrdemDeServicoController`, `ClienteController` |
| **BO** | `bo` | Regras de negócio, orquestração e fronteira transacional. Carrega entidades, valida regras, calcula orçamento e aciona o DAO. | `OrdemDeServicoBO`, `EstoqueBO`, `ClienteBO`, `MotoBO` |
| **DAO** | `dao` | Persistência exclusivamente via Spring Data JPA: queries derivadas, JPQL, `@EntityGraph` e baixa atômica de estoque. | `OrdemDeServicoDAO`, `PecaDAO` |
| **MODEL** | `model` | Entidades JPA, enums e regras de domínio puras (sem Spring). | `OrdemDeServico`, `ServicoTrocaPeca`, `StatusOS`, `Precificacao` |
| **VO** | `vo` | Contratos de entrada e saída da API e envelopes de paginação. Não contêm regra de negócio. | `OrdemDeServicoRequestVO`, `OrcamentoResponseVO`, `PaginaResponseVO` |
| **MAPPER** | `mapper` | Conversão estática entidade ↔ VO, executada dentro da transação do BO. | `OrdemDeServicoMapper`, `PecaMapper` |
| **EXCEPTION** | `exception` | Exceções de domínio e tradução centralizada para `application/problem+json`. | `GlobalExceptionHandler`, `EstoqueInsuficienteException` |
| **CONFIG/UTIL** | `config`, `util` | Infraestrutura e utilitários sem estado. | `OpenApiConfig`, `Documento` |

Fluxo de uma requisição (ex.: `POST /ordens-servico/{id}/servicos`):

1. O controller recebe o JSON em `ServicoRequestVO`, valida com Bean Validation e chama o BO.
2. O BO abre a transação, carrega a `OrdemDeServico` pelo DAO e aplica as regras de negócio.
3. O DAO consulta/persiste **entidades** via Hibernate; não recebe VOs nem devolve `ResultSet`.
4. O `OrdemDeServicoMapper` converte a entidade em `OrdemDeServicoResponseVO` ainda dentro da
   transação — necessário porque `spring.jpa.open-in-view` está `false`.
5. O controller devolve o VO com o status HTTP adequado; falhas passam pelo `GlobalExceptionHandler`.

Consequências práticas:

- **BO → DAO usa entidades**, não VOs; os VOs existem apenas na borda (controller ↔ BO).
- **Nenhum controller acessa DAO** diretamente; a ordem é sempre `controller → BO → DAO`.
- **O schema não é escrito em SQL manual**: as entidades declaram `schema = "MOTOSERVICES"`, o Flyway
  versiona as migrações e o Hibernate apenas valida o mapeamento (`ddl-auto: validate`).
- **Dinheiro e cálculo ficam no domínio**: `Precificacao` é a fonte única das fórmulas, e
  `ServicoRevisao`/`ServicoTrocaPeca` implementam `calcularValorFinal` por polimorfismo.
- **Transações pertencem ao BO**: métodos públicos são `@Transactional` (ou `readOnly = true`);
  controllers e mappers não controlam transação.

A versão detalhada, com decisões de projeto e limites de cada camada, está em
[`src/docs/02-arquitetura.md`](src/docs/02-arquitetura.md).


---

## Diagrama de Sequência

Fluxo completo de criação e fechamento de uma Ordem de Serviço:




```
Mecânico      View          BO              DAO            Banco
   │            │             │               │               │
   │── buscarMoto(marca, modelo) ──►          │               │
   │            │── encontrarMoto() ─────────►│               │
   │            │             │── selecionarMoto()───────────►│
   │            │             │◄──────────── MotoVO ──────────│
   │            │             │  [alt: MotoInvalidaException] │
   │◄── MotoVO validada ──────│               │               │
   │                          │               │               │
   │── criarOS(clientId, moto) ──────────────►│               │
   │            │             │── encontrarCliente()─────────►│
   │            │             │◄──────────── ClienteVO ───────│
   │            │             │  [alt: ClienteNaoEncontradoException]
   │                          │               │               │
   │  [loop] adicionarServico(tipo, params) ─►│               │
   │            │             │  [se TrocaPeca && pecaLoja]   │
   │            │             │── checarEstoque() ───────────►│
   │            │             │◄──────────── PecaVO ──────────│
   │            │             │  [alt: EstoqueInsuficienteException]
   │            │             │  os.servicos.add(servico)     │
   │                          │               │               │
   │── calcularOrcamento() ──────────────────────────────────►│
   │            │             │  [loop] s.calcularValorFinal(moto) // polimorfismo
   │            │             │  [se >= 5 peças loja] desconto = totalPecas * 0.05
   │◄── OrcamentoVO ──────────│               │               │
   │                          │               │               │
   │── confirmarOS() ────────────────────────────────────────►│
   │            │             │── inserirOS() ───────────────►│
   │            │             │── atualizarEstoque() ────────►│
   │◄── osId confirmado ──────│               │               │
   │                          │               │               │
Atendente                     │               │               │
   │── fecharOS(osId) ───────────────────────►│               │
   │            │             │── atualizarStatusOS(FECHADA)─►│
   │            │             │── agendarProximaRevisao() ───►│
   │◄── OrcamentoVO ──────────│  [NF: roadmap]│               │
```

---

## Diagrama de Classes

```
<<interface>>
Precificavel
+ calcularValorFinal(moto: Moto): double
        ▲
        │ implements
        │
abstract ServicoDaOficina
- id: int
- descricao: String
- tempoEstimado: int
- valorBaseMaoDeObra: double
+ calcularValorFinal(moto: Moto): double  {abstract}
        ▲                   ▲
        │                   │
ServicoRevisao      ServicoTrocaPeca
- checklistGeral    - pecaEnvolvida: Peca
                    - pecaFornecidaPelaLoja: boolean

Moto                        Cliente
- id: int                   - id: int
- marca: String             - nome: String
- modelo: String            - cpfCnpj: String
- cilindradas: int          - contato: String
                            - proximaRevisao: LocalDate

Peca                        OrdemDeServico
- id: int                   - id: int
- codigo: String            - cliente: Cliente
- descricao: String         - moto: Moto
- valor: double             - status: StatusOS (enum)
- quantidadeEstoque: int    - servicos: List<ServicoDaOficina>
- estoqueMinimo: int        - dataAbertura: LocalDateTime
+ isAbaixoDoMinimo(): bool  - dataFechamento: LocalDateTime

OrcamentoVO (Value Object)   Exceptions
- totalPecas: BigDecimal     EstoqueInsuficienteException
- totalMaoDeObra: BigDecimal MotoInvalidaException
- descontoAplicado: BigDecimal ClienteNaoEncontradoException
- totalFinal: BigDecimal

BO Layer                        DAO Layer
OrdemDeServicoBO                OrdemDeServicoDAO
ClienteBO                       ClienteDAO
EstoqueBO                       PecaDAO
MotoBO                          MotoDAO
```

---

## Testes (JUnit 5)

Comando: `./mvnw verify` — suíte completa, Checkstyle e gate de cobertura JaCoCo.
A integração roda Flyway e o mapeamento JPA (`ddl-auto: validate`) em um PostgreSQL
embarcado, sem Docker.



**`OrdemDeServicoBOTest`**

```
- `testeDescontoAplicado()` — desconto de 5% ativo com ≥ 5 peças da loja
- `testeSemDesconto()` — sem desconto com menos de 5 peças da loja
- `testeMotoMaoDeObraExtra()` — cilindradas > 400 cc gera acréscimo de 50% e > 650 cc gera acréscimo de 70% na mão de obra
- `testePecaAvulsa()` — peça comprada por fora: cobra só mão de obra
- `testeExcecaoEstoqueInsuficiente()` — exceção ao usar peça sem estoque
- `testeExcecaoMotoInvalida()` — exceção ao buscar moto inexistente
- `testeExcecaoClienteNaoEncontrado()` — exceção ao buscar cliente inexistente
- `testeTotaisDoOrcamentoVOEstaoCorretos()` — totais do VO consistentes com as regras
- `testeConfirmarOS()` — confirmação sem baixa dupla de estoque
- `testeFecharOS()` — fechamento com agendamento da próxima revisão

```

**`OrdemDeServicoDAOTest`** *(planejado — testes de integração com Testcontainers)*
```
- `testeInserirOS()` — persistência de nova OS no banco
- `testeAtualizarStatusOS()` — transição de status refletida na tabela
- `testeSelecionarOSPorCliente()` — listagem de OSs por `clienteId`
- `testeAtualizarEstoqueAposOS()` — baixa automática de estoque ao confirmar OS
- `testeSelecionarPecasComBaixoEstoque()` — query retorna apenas peças abaixo do mínimo

```

---

## Licença

MIT — ver [LICENSE](./LICENSE).

---

<p align="center">
  <sub>Feito para garantir mais previsibilidade ao mecânico e ao cliente.</sub>
</p>
