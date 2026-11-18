# 08 — Frontend

## Stack

Definida em `frontend/package.json`:

| Item | Versão/uso |
|---|---|
| React | 19 + TypeScript |
| Build/dev server | Vite 8 (plugin React) |
| Estilo | Tailwind CSS 4 via `@tailwindcss/vite`, tokens creme/verde em `src/styles/globals.css` |
| Dados remotos | TanStack Query 5 |
| Rotas | React Router 7 (`BrowserRouter`, `Routes`) |
| Formulários | React Hook Form + Zod (`@hookform/resolvers`) |
| Primitivos acessíveis | Radix Dialog e Tabs |
| Ícones | lucide-react |
| Testes | Vitest 5 + Testing Library + jsdom + `vitest-axe` |
| Lint | oxlint |

O SPA não tem estado global de dados (Redux/Zustand): o cache do TanStack Query é a fonte de dados do
servidor, e o estado de UI é local aos componentes. A sessão autenticada é a exceção: fica em
`localStorage` (`motoservices.sessao`) e é lida por `useSessao`.

## Estrutura

```text
frontend/
├── index.html                  # lang="pt-BR", fonte Inter, meta description
├── vite.config.ts              # porta 5173, proxy /api → :8080, config do Vitest
├── src/
│   ├── main.tsx                # createRoot + StrictMode + globals.css
│   ├── App.tsx                 # QueryClientProvider + BrowserRouter
│   ├── AppRoutes.tsx           # mapa de rotas
│   ├── api/
│   │   ├── http.ts             # fetch tipado, Bearer, ApiError, downloads autenticados
│   │   ├── auth.ts             # login e /auth/me
│   │   ├── types.ts            # Cliente, Moto, Peca, OrdemDeServico, Page…
│   │   ├── clientes.ts         # endpoints de clientes
│   │   ├── motos.ts            # catálogo/filtro de cilindradas
│   │   ├── pecas.ts            # peças e entrada de estoque
│   │   ├── ordens.ts           # OS, serviços, orçamento, histórico
│   │   └── hooks.ts            # hooks de query/mutation + queryKeys
│   ├── components/
│   │   ├── auth/RequireAuth.tsx# guarda de rota autenticada
│   │   ├── layout/AppLayout.tsx# sidebar, skip link, nav e logout
│   │   ├── layout/navigation.ts
│   │   └── ui/                 # Button, Badge, Dialog, Tabs, TextField, Pagination, States…
│   ├── features/               # formulários em Dialog por domínio
│   ├── lib/                    # cn, format, status, queryClient, session (JWT + papel)
│   ├── pages/                  # Landing, Login, Dashboard, clientes, motos, estoque, ordens
│   ├── styles/                 # globals.css (tokens) e contrast.test.ts
│   └── test/                   # setup.ts e utils.tsx
```

## Rotas

| Rota | Página | Conteúdo |
|---|---|---|
| `/` | `LandingPage` | Página pública do produto |
| `/entrar` | `LoginPage` | Login (pública); guarda o JWT e segue para o destino original |
| `/painel` | `DashboardPage` | Indicadores do pátio, fila de OS e peças em ruptura |
| `/ordens-servico` | `OrdensPage` | Lista com filtros por cliente/status, abertura de OS |
| `/ordens-servico/:osId` | `OrdemDetalhePage` | Serviços, orçamento, confirmar, fechar, histórico |
| `/clientes` | `ClientesPage` | Busca por nome/documento e cadastro |
| `/clientes/:clienteId` | `ClienteDetalhePage` | Dados, motos vinculadas e OS do cliente |
| `/motos` | `MotosPage` | Filtro por cilindradas e cadastro |
| `/estoque` | `EstoquePage` | Abas de peças, ruptura e entrada de estoque |
| `/relatorios` | `RelatoriosPage` | Faturamento, rankings e exportação CSV |
| `*` | `NotFoundPage` | 404 da SPA |

O layout (`AppLayout`) tem link "Pular para o conteúdo", navegação lateral, identificação do usuário
e logout. Todas as rotas exceto `/` e `/entrar` ficam sob `RequireAuth`; sem sessão válida, o
usuário é levado a `/entrar` com o destino preservado. O menu esconde "Relatórios" para `MECANICO`.

## Camada de API

### `src/api/http.ts`

- `API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/+$/, '')`.
- `apiRequest<T>(path, init)`:
  - envia `Accept: application/json, application/problem+json`;
  - adiciona `Authorization: Bearer <token>` quando há sessão;
  - define `Content-Type: application/json` quando há corpo;
  - trata `204` como `undefined`;
  - em `401`, limpa a sessão (o `RequireAuth` redireciona para `/entrar`);
  - lança `ApiError` com `status`, `problem` (RFC 9457) e `fieldErrors` (mapa campo → mensagem);
  - status `0` representa falha de rede (`isNetworkError`).
- `apiDownload` / `baixarArquivo` / `abrirArquivo`: buscam CSV/HTML com Bearer e entregam via blob,
  sem colocar o token na URL.
- `buildQuery` ignora `undefined`, `null` e string vazia.

### Módulos por domínio

| Arquivo | Exemplos de chamadas |
|---|---|
| `clientes.ts` | `listarClientes(nomeOuCpf)`, `buscarCliente`, `criarCliente`, `listarMotosDoCliente`, `vincularMoto`, `desvincularMoto` |
| `motos.ts` | `filtrarMotos(minimo, maximo)` (envia `minimo`/`maximo`), `listarMotos` (= faixa 1..2000), `buscarMoto`, `criarMoto` |
| `pecas.ts` | `listarPecas(busca)`, `buscarPeca`, `criarPeca`, `registrarEntrada`, `listarBaixoEstoque` |
| `ordens.ts` | `listarOrdens(filtros)`, `buscarOrdem`, `criarOrdem`, `adicionarServico`, `calcularOrcamento`, `confirmarOrdem`, `fecharOrdem`, `listarHistorico` |

### Hooks e cache

`src/api/hooks.ts` centraliza `useQuery`/`useMutation` e as chaves:

```ts
queryKeys = {
  clientes(termo, page, size), cliente(id), motosDoCliente(id, page),
  motos(page, size), pecas(busca, page), baixoEstoque(page),
  ordensDoCliente(id, page), ordens(clienteId, status, page),
  ordem(id), orcamento(id), historico(id, page),
}
```

- Mutações invalidam as chaves afetadas (`['clientes']`, `['motos']`, `['pecas']`, `['ordens']`).
- `queryClient` (`src/lib/queryClient.ts`): `retry: 1`, `refetchOnWindowFocus: false`,
  `staleTime: 30_000`.
- Listagens usam `keepPreviousData` para evitar salto de layout ao paginar/filtrar.

## Formulários

Todos os formulários seguem o mesmo padrão:

1. Schema Zod + `zodResolver` (validação imediata, em português).
2. `useMutation` do hook correspondente.
3. Ao receber `ApiError`, os `fieldErrors` são aplicados com `setError(campo, ...)`; se nenhum campo
   for reconhecido, mostra a mensagem geral em `FeedbackMessage`.
4. Fecham o dialog e invalidam o cache em caso de sucesso.

Formulários existentes: `ClienteFormDialog`, `MotoFormDialog`, `OrdemFormDialog`,
`ServicoFormDialog`, `PecaFormDialog`, `EntradaEstoqueDialog`.

Regra de destaque: `ServicoFormDialog` valida no cliente as mesmas condições do servidor
(`pecaFornecidaPelaLoja` obrigatório em `TROCA_PECA`; `pecaId` obrigatório quando a peça é da loja;
`checklistGeral` só para revisão).

## UI e acessibilidade

- Tokens de cor/tipografia em `src/styles/globals.css` (`@theme`), com variantes semânticas
  (`success`, `danger`, `warning`, `info`).
- `StatusBadge` + `src/lib/status.ts` traduzem os status e escolhem ícone/cor.
- Componentes de estado: `LoadingState`, `EmptyState`, `ErrorState` (com retry).
- `Dialog` usa Radix (foco, ESC, `aria-modal`); `Tabs` usa Radix.
- Formatação pt-BR em `src/lib/format.ts`: moeda BRL, data, intervalo de espera (`2h15`), CPF/CNPJ.
- Acessibilidade coberta por:
  - skip link e landmarks no layout;
  - `src/a11y.test.tsx` (axe em telas representativas);
  - `src/styles/contrast.test.ts` (contraste WCAG AA dos tokens);
  - testes de páginas com Testing Library.

## Scripts

```bash
npm run dev        # Vite em :5173 (proxy /api → :8080)
npm run build      # tsc -b && vite build → dist/
npm run preview    # serve dist em :4173 com o mesmo proxy
npm test           # vitest run (todos os testes)
npm run test:watch # vitest em watch
npm run typecheck  # tsc -b
npm run lint       # oxlint
```

O build hoje emite aviso de chunk único acima de 500 kB (`dist/assets/index-*.js`). É um aviso, não
erro; para reduzi-lo, use `import()` dinâmico nas páginas/rotas.

## Desenvolvimento e produção

- **Dev:** o proxy do `vite.config.ts` reescreve `/api/...` para `http://localhost:8080/...`.
- **Preview/build:** o mesmo proxy é definido em `preview`.
- **Produção:** sirva `dist/` atrás de um proxy reverso que encaminhe `/api` ao backend (o backend
  não configura CORS) ou gere o build com `VITE_API_BASE_URL` apontando para a API. Nesse caso, a API
  precisa permitir a origem.

## Testes do frontend

| Arquivo | Foco |
|---|---|
| `src/App.test.tsx` | Renderização e navegação básica |
| `src/a11y.test.tsx` | Violações axe em telas principais |
| `src/api/http.test.ts` | `apiRequest`, erros `problem+json`, `fieldErrors`, 204, rede |
| `src/pages/LoginPage.test.tsx` | Login, persistência da sessão e erro de credenciais |
| `src/pages/DashboardPage.test.tsx` | Indicadores, fila e estoque crítico |
| `src/pages/clientes/ClientesPage.test.tsx` | Busca, cadastro e estados |
| `src/pages/estoque/EstoquePage.test.tsx` | Abas, ruptura e entrada |
| `src/pages/motos/MotosPage.test.tsx` | Filtro de cilindradas e validação |
| `src/pages/ordens/OrdensPage.test.tsx` | Lista, filtros e abertura |
| `src/pages/ordens/OrdemDetalhePage.test.tsx` | Serviços, orçamento, confirmar/fechar e histórico |
| `src/styles/contrast.test.ts` | Contraste WCAG AA dos tokens |

As chamadas de rede são mockadas nos testes de página (`vi.mock` dos módulos de API / hooks); não há
teste E2E de navegador no repositório.
