# 09 — Guia do desenvolvedor

Este é o roteiro prático para o primeiro dia e para quem já conhece o projeto e precisa lembrar
"onde mexo".

## Primeiro dia — checklist

```bash
# 1. clone e entre na raiz
cd MotoServices

# 2. backend: teste tudo sem precisar de banco externo
./mvnw verify                # Windows: mvnw.cmd verify

# 3. suba a API (precisa de PostgreSQL local ou docker compose up postgres)
createdb motoservices
./mvnw spring-boot:run

# 4. valide
curl http://localhost:8080/actuator/health
# abra http://localhost:8080/swagger-ui.html

# 5. frontend, em outro terminal
cd frontend
npm ci
npm run dev                  # http://localhost:5173
npm test
```

Leia, nesta ordem:

1. [`README.md`](../../README.md) — produto e visão geral.
2. [01 — Visão geral](01-visao-geral.md) — vocabulário e escopo real.
3. [02 — Arquitetura](02-arquitetura.md) — camadas e fluxo.
4. [03 — Modelo de domínio](03-modelo-de-dominio.md) — regras de negócio e preço.
5. [04 — API REST](04-api-rest.md) — contrato que você vai usar/alterar.
6. [06 — Configuração](06-configuracao-e-execucao.md) e
   [07 — Testes](07-testes-e-qualidade.md) conforme a necessidade.

## Rastreando uma operação de ponta a ponta

Objetivo: entender como "abrir uma OS" atravessa UI, HTTP, BO, entidade e banco.

1. **UI:** o botão "Abrir OS" está em `frontend/src/pages/ordens/OrdensPage.tsx` e abre
   `frontend/src/features/ordens/OrdemFormDialog.tsx`.
2. **Formulário:** valida com Zod e chama `useCriarOrdem` (hooks.ts), que usa `ordensApi.criarOrdem`
   (`POST /ordens-servico`).
3. **HTTP:** `frontend/src/api/http.ts` envia JSON para `/api/ordens-servico`; em dev o proxy do Vite
   remove `/api` e encaminha para `:8080`.
4. **Controller:** `OrdemDeServicoController.criar` recebe `OrdemDeServicoRequestVO`, valida com
   Bean Validation e chama `OrdemDeServicoBO.criarOS(request)`.
5. **BO:** valida cliente (`ClienteBO.encontrarCliente`) e moto no catálogo
   (`MotoBO.encontrarMoto`), vincula a moto ao cliente, cria a OS com status `ABERTA`, salva e grava
   `HistoricoStatusOS` (`null → ABERTA`).
6. **DAO/entidade:** `OrdemDeServicoDAO.save` persiste em `tb_ordem_de_servico`; Hibernate usa
   `@Version` e o Flyway já criou o schema.
7. **Resposta:** `OrdemDeServicoMapper.toResponse` gera o VO (com serviço `valorFinal` calculado) e
   o controller devolve `201` + `Location`.

Para acompanhar no banco durante o desenvolvimento, ative o log SQL (`dev` já traz
`org.hibernate.SQL: debug`) ou consulte as tabelas com `psql`
([05 — Banco de dados](05-banco-de-dados.md#inspeção-e-manutenção)).

## Onde mexer

| Necessidade | Arquivos principais | Não esqueça |
|---|---|---|
| Nova regra de preço/desconto | `model/Precificacao.java`; uso em `bo/OrdemDeServicoBO.calcularOrcamento` e `ServicoTrocaPeca.calcularValorFinal` | Testes `PrecificacaoTest` e `OrdemDeServicoBOTest`; docs 03 |
| Novo campo persistido em cliente/moto/peça/OS/usuário | `model/*`, `db/migration/V9__...sql`, `vo/*RequestVO`, `vo/*ResponseVO`, `mapper/*` | `MigrationsTest` (contagem de migrações), docs 03/05 |
| Nova regra de acesso | `security/SecurityConfig.java` (matchers por rota/papel) | Teste de integração 401/403 e docs 04 |
| Novo endpoint | `controller/*`, `bo/*`; VO e mapper se necessário | Anotações OpenAPI, `ErrorContractTest`, docs 04 |
| Novo filtro/consulta | `dao/*` + `bo/*` + `controller/*` | Paginação e `@EntityGraph` quando houver relacionamento |
| Nova exceção de domínio | `exception/*`, `GlobalExceptionHandler` | Mapear para 400/404/409 de forma coerente; teste de contrato |
| Nova tela no frontend | `pages/*`, `AppRoutes.tsx`, `components/layout/navigation.ts`, `api/hooks.ts` | Teste de página + a11y; estados de loading/erro/vazio |
| Novo formulário | `features/<dominio>/*FormDialog.tsx` | Zod + `ApiError.fieldErrors` + invalidação de cache |
| Alterar status/fluxo da OS | `model/enums/StatusOS.java`, `bo/OrdemDeServicoBO` | Máquina de estados, histórico, testes de transição, docs 03 |
| Alterar contrato de erro | `exception/GlobalExceptionHandler.java` | `ErrorContractTest`, `frontend/src/api/http.ts`, docs 04 |

## Convenções do projeto

### Backend

- Pacotes em minúsculas, classes em `PascalCase`, métodos em `camelCase`; nomes de domínio em
  português (`OrdemDeServico`, `pecaFornecidaPelaLoja`).
- Sufixos: `*Controller`, `*BO`, `*DAO`, `*RequestVO`, `*ResponseVO`, `*Mapper`, `*Exception`.
- Controller não contém regra de negócio; BO não monta resposta HTTP; entidade não conhece Spring.
- Métodos públicos de BO são transacionais (`@Transactional` ou `readOnly = true`).
- Dinheiro: `BigDecimal` com 2 casas; use `Precificacao.arredondar` (`HALF_UP`). Nunca `double`.
- Mensagens de exceção em português, específicas e sem dados sensíveis.
- Não edite migração aplicada: crie a próxima versão.
- Checkstyle: linha ≤ 160, sem imports com `*` ou não usados, chaves obrigatórias; testes também são
  verificados.

### Frontend

- Componentes em `PascalCase`; hooks começam com `use`; tipos em `api/types.ts`.
- Chaves de cache centralizadas em `queryKeys`; mutações invalidam as chaves afetadas.
- Toda chamada HTTP passa por `apiRequest`; nunca use `fetch` direto na página.
- Formulários: Zod para validação local; erros da API via `ApiError.fieldErrors`.
- Textos e datas em pt-BR; use `src/lib/format.ts`.
- Acessibilidade: labels reais, `aria-*` quando necessário, estados anunciados; axe deve continuar
  verde.
- Tokens de cor apenas via classes/variáveis de `globals.css`; não invente cores soltas.

## Armadilhas comuns

1. **`open-in-view: false`:** acessar relacionamento lazy fora do método transacional do BO causa
   `LazyInitializationException`. Mapeie para VO dentro da transação.
2. **Entidade × schema:** `ddl-auto: validate` falha o boot se a migração e a entidade divergirem.
   Sempre altere as duas.
3. **Contagem de migrações no teste:** `MigrationsTest` espera 8 migrações; adicionar uma nova
   exige atualizar a asserção.
4. **Baixa dupla de estoque:** `confirmarOS` só aceita `ABERTA → EM_ANDAMENTO`. Não "conserte" o 409
   permitindo reconfirmar; a máquina de estados é a proteção.
5. **Preço depois do fechamento:** o snapshot é intencional. `GET /orcamento` pode mudar; o valor
   cobrado é o gravado no fechamento.
6. **`MotoInvalidaException` é 404:** se criar uma regra nova que reusa essa exceção, saiba que o
   cliente HTTP verá "não encontrado".
7. **Filtro de motos:** os parâmetros são `minimo`/`maximo`; não invente `minCilindradas`.
8. **`frontend/` não está versionado no Git deste repositório no estado atual** — confirme com
   `git status` antes de assumir que suas mudanças de SPA serão commitadas.
9. **CORS e token:** o backend restringe origens por `CORS_ALLOWED_ORIGINS`; em dev, o proxy do Vite
   evita CORS. Downloads usam `fetch` com Bearer — não coloque o token na URL.
10. **Swagger desligado em `prod`:** não espere `/swagger-ui.html` no Compose.

## Checklist de pull request

- [ ] `./mvnw verify` verde (testes + Checkstyle + JaCoCo).
- [ ] `cd frontend && npm test && npm run build` verdes, se o frontend mudou.
- [ ] Migração nova? `V9__...` + entidade + `MigrationsTest` + [05 — Banco](05-banco-de-dados.md).
- [ ] Rota nova? Regra em `SecurityConfig` + teste 401/403 + docs 04.
- [ ] Endpoint novo/alterado? OpenAPI anotado + [04 — API](04-api-rest.md) + teste de contrato.
- [ ] Regra nova? Teste unitário no BO/`Precificacao` e [03 — Domínio](03-modelo-de-dominio.md).
- [ ] Erros novos mapeados no `GlobalExceptionHandler` e cobertos por `ErrorContractTest`.
- [ ] Nenhum arquivo temporário, credencial ou artefato de build no diff.
- [ ] Documentação atualizada junto com o código (README ou `src/docs`).

## Roadmap técnico e próximos passos sugeridos

Em ordem de valor:

1. **Smoke E2E da API** em `@SpringBootTest` (fluxo completo + 409 de transição/estoque) — fechado
   por `FluxoCompletoIntegrationTest`.
2. **Testes de DAO** contra PostgreSQL real (consultas derivadas, `EntityGraph`, baixa atômica).
3. **Gestão de usuários e revogação de token** (CRUD, troca de senha, expiração curta/lista de bloqueio).
4. **Relatórios/faturamento** reutilizando `OrdemDeServicoDAO.buscarPorPeriodo` e o snapshot.
5. **Comprovante/NF** e registro de pagamento no fechamento.
6. **Tratamento de concorrência otimista** como 409 (hoje cai em 500).
7. **Alinhamento de versões** (`pom.xml`, OpenAPI, `info.app.version`).
8. **Code splitting** no frontend para eliminar o chunk único > 500 kB.
