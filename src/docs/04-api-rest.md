# 04 — API REST

## Convenções gerais

| Item | Valor |
|---|---|
| Base URL (dev) | `http://localhost:8080` |
| Formato | JSON UTF-8 (`application/json`) |
| Erros | `application/problem+json` (RFC 9457) |
| Datas | ISO-8601 sem fuso (`LocalDateTime`/`LocalDate`), ex.: `2026-11-17T21:26:49.2204589` |
| Dinheiro | número JSON com 2 casas, ex.: `350.00` |
| Paginação | query `page`, `size`, `sort` (Spring Data) |
| Tamanho máximo de página | 100 |
| Autenticação | `Authorization: Bearer <JWT>`, exceto login, health e OpenAPI |
| Documentação viva | `GET /v3/api-docs`, UI em `/swagger-ui.html` (apenas perfil `dev`) |

### Envelope de paginação

Toda listagem devolve:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

`sort` usa o formato `campo,asc|desc` (ex.: `sort=dataAbertura,desc`). Cada endpoint tem um padrão
descrito abaixo.

### Erros RFC 9457

Estrutura:

```json
{
  "type": "urn:motoservices:problem:validacao",
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Um ou mais campos estão inválidos.",
  "instance": "/motos",
  "errors": [
    { "field": "marca", "message": "marca é obrigatória" },
    { "field": "cilindradas", "message": "cilindradas deve ser positiva" }
  ]
}
```

`errors[]` só aparece em erros de validação de campos/parâmetros. Tipos estáveis:

| `type` | HTTP típico | Origem |
|---|---|---|
| `urn:motoservices:problem:validacao` | 400 | `ValidacaoException`, Bean Validation, `ConstraintViolationException` |
| `urn:motoservices:problem:requisicao-invalida` | 400 | JSON malformado, parâmetro ausente ou com tipo errado |
| `urn:motoservices:problem:nao-autenticado` | 401 | Login inválido, token ausente, expirado ou inválido |
| `urn:motoservices:problem:acesso-negado` | 403 | Papel sem permissão para a rota |
| `urn:motoservices:problem:nao-encontrado` | 404 | Cliente/moto/peça/OS inexistente ou rota desconhecida |
| `urn:motoservices:problem:conflito` | 409 | Estoque insuficiente, transição inválida, violação de integridade |
| `urn:motoservices:problem:metodo-nao-suportado` | 405 | Método HTTP não suportado na rota |
| `urn:motoservices:problem:midia-nao-suportada` | 415 | `Content-Type` não suportado |
| `urn:motoservices:problem:erro-interno` | 500 | Exceção não tratada |

Observação importante: `MotoInvalidaException` é mapeada para **404** (recurso do catálogo não
encontrado), mesmo sendo usada também para validar a moto de uma OS.

---

## Autenticação

### `POST /auth/login` → 200 / 401

Rota pública. Corpo:

```json
{ "username": "admin", "senha": "admin123" }
```

Resposta `200`:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "usuario": { "id": 1, "username": "admin", "nome": "Administrador", "papel": "ADMIN" }
}
```

Credenciais inválidas ou usuário inativo → `401` com
`urn:motoservices:problem:nao-autenticado` e a mesma mensagem para usuário inexistente e senha
errada.

### `GET /auth/me` → 200 / 401

Devolve o `usuario` do token (`Authorization: Bearer <token>`). Útil para validar a sessão após
recarregar a SPA.

### Regras de autorização

| Rota | Papéis |
|---|---|
| `POST /auth/login`, `/actuator/health/**`, `/v3/api-docs/**`, `/swagger-ui/**` | público |
| `GET /relatorios/**` | `ADMIN`, `ATENDENTE` |
| `POST /ordens-servico/{id}/fechar`, `/cancelar` | `ADMIN`, `ATENDENTE` |
| `POST /pecas`, `POST /pecas/{id}/entrada` | `ADMIN` |
| `DELETE /clientes/{id}/motos/{motoId}` | `ADMIN`, `ATENDENTE` |
| Demais rotas | qualquer usuário autenticado |

O token carrega o claim `papel`, convertido em `ROLE_<PAPEL>`. Sem token válido → `401`; papel sem
permissão → `403` (`urn:motoservices:problem:acesso-negado`).

## Clientes

### `POST /clientes` → 201

Cria um cliente. `Location: /clientes/{id}`.

```json
{
  "nome": "Maria Silva",
  "cpfCnpj": "529.982.247-25",
  "contato": "11999999999",
  "proximaRevisao": "2027-01-15"
}
```

- `nome` obrigatório (≤ 200), `cpfCnpj` obrigatório (≤ 20), `contato` obrigatório (≤ 100).
- `proximaRevisao` opcional e não pode estar no passado (`@FutureOrPresent`).
- CPF/CNPJ é normalizado para apenas dígitos e validado por dígito verificador no BO.
- Documento duplicado detectado no BO → **400** `validacao`; corrida de unicidade no banco → **409**.

Resposta:

```json
{
  "id": 1,
  "nome": "Maria Silva",
  "cpfCnpj": "52998224725",
  "contato": "11999999999",
  "proximaRevisao": null
}
```

### `GET /clientes?nomeOuCpf=...` → 200

`nomeOuCpf` é **obrigatório**. Comportamento:

- Se o termo normalizado tiver **11 ou 14 dígitos**, busca exata por documento e devolve 0 ou 1 item
  (envelope com `size` igual a `max(quantidade, 1)` e uma única página).
- Caso contrário, busca por nome contendo o termo, ignorando maiúsculas, de forma paginada
  (padrão `size=20`, `sort=nome`).

### `GET /clientes/{id}` → 200 / 404

### `GET /clientes/{id}/motos` → 200 / 404

Motos vinculadas ao cliente, ordenadas por marca e modelo. A paginação é feita **em memória**
(coleção pequena); aceita `page` e `size`.

### `POST /clientes/{id}/motos/{motoId}` → 204

Cria o vínculo se não existir (idempotente). `404` se cliente ou moto não existirem.

### `DELETE /clientes/{id}/motos/{motoId}` → 204

Remove o vínculo se existir (idempotente). `404` se o cliente não existir.

---

## Motos

### `POST /motos` → 201

`Location: /motos/{id}`.

```json
{ "marca": "Honda", "modelo": "CG 160", "cilindradas": 160 }
```

- `marca`/`modelo` obrigatórios (≤ 100); `cilindradas` obrigatório e positivo.
- Marca+modelo duplicados (ignorando caixa) → **400** `validacao`.

### `GET /motos/{id}` → 200 / 404

### `GET /motos/filtrar?minimo=1&maximo=2000` → 200 / 400

- **`minimo` e `maximo` são obrigatórios.** Os limites são **inclusivos** (equivalente a
  `cilindradas BETWEEN minimo AND maximo`).
- `minimo` deve ser `> 0`; `maximo` deve ser `>= minimo`.
- Parâmetro ausente → 400 `requisicao-invalida` com
  `"detail": "Parâmetro obrigatório ausente: minimo."`.
- Faixa inválida → 400 `validacao` com a mensagem da regra.
- Paginado, `size=20` por padrão, ordenado por `cilindradas` (asc).

> Os nomes corretos são `minimo` e `maximo`. **Não use** `minCilindradas`/`maxCilindradas`.
> O frontend (`src/api/motos.ts`) já envia os nomes corretos; o Swagger os declara como
> obrigatórios com exemplo.

---

## Peças e estoque

### `POST /pecas` → 201

`Location: /pecas/{id}`.

```json
{
  "codigo": "P-001",
  "descricao": "Pastilha de freio",
  "valor": 120.00,
  "quantidadeEstoque": 10,
  "estoqueMinimo": 2
}
```

- `codigo` obrigatório, ≤ 50 e único; `descricao` obrigatória, ≤ 255.
- `valor` (até 2 casas), `quantidadeEstoque` e `estoqueMinimo` não podem ser negativos.
- Código repetido → **400** `validacao`.

### `GET /pecas?busca=...` → 200

`busca` é opcional; quando informada, procura em `descricao` **ou** `codigo` (case-insensitive).
Padrão `size=20`, `sort=codigo`.

### `GET /pecas/{id}` → 200 / 404

### `POST /pecas/{id}/entrada` → 200 / 400 / 404

```json
{ "quantidade": 5 }
```

Soma a quantidade ao saldo atual. `quantidade` deve ser positiva.

### `GET /estoque/baixo` → 200

Peças com `quantidadeEstoque < estoqueMinimo` (comparação entre colunas). Padrão `size=20`,
`sort=codigo`.

---

## Ordens de serviço

### `POST /ordens-servico` → 201

`Location: /ordens-servico/{id}`.

```json
{ "clienteId": 1, "marcaMoto": "Honda", "modeloMoto": "CG 160" }
```

- Cliente e moto precisam existir (`404` caso contrário).
- A moto é vinculada ao cliente automaticamente, se ainda não estiver.
- A OS nasce `ABERTA` e registra a primeira linha do histórico (`null → ABERTA`).

Resposta (campos de snapshot nulos até o fechamento):

```json
{
  "id": 1,
  "clienteId": 1,
  "clienteNome": "Maria Silva",
  "motoId": 1,
  "motoMarca": "Honda",
  "motoModelo": "CG 160",
  "motoCilindradas": 160,
  "status": "ABERTA",
  "dataAbertura": "2026-11-17T21:26:49.220459",
  "dataFechamento": null,
  "totalPecas": null,
  "totalMaoDeObra": null,
  "descontoAplicado": null,
  "totalFinal": null,
  "servicos": []
}
```

### `GET /ordens-servico?clienteId=&status=` → 200

Filtros opcionais e combináveis. Se `clienteId` for informado, o cliente precisa existir (`404`).
Padrão `size=20`, ordenado por `dataAbertura` **desc**.

### `GET /ordens-servico/{id}` → 200 / 404

### `GET /ordens-servico/{id}/historico` → 200 / 404

Transições da OS, mais recentes primeiro (`sort=dataHora,desc`; `size=20`).

```json
{
  "id": 4, "osId": 1,
  "statusAnterior": "CONCLUIDA", "statusNovo": "FECHADA",
  "dataHora": "2026-11-17T21:26:50.046609"
}
```

### `POST /ordens-servico/{id}/servicos` → 200 / 400 / 404 / 409

Adiciona um serviço e devolve a OS atualizada. Só permitido em `ABERTA`, `EM_ANDAMENTO` ou
`AGUARDANDO_CLIENTE` (**400** se a OS estiver encerrada).

**Revisão:**

```json
{
  "tipo": "REVISAO",
  "descricao": "Revisão de 10.000 km",
  "tempoEstimado": 60,
  "valorBaseMaoDeObra": 150.00,
  "checklistGeral": true
}
```

**Troca de peça com peça da loja:**

```json
{
  "tipo": "TROCA_PECA",
  "descricao": "Troca de pastilha",
  "tempoEstimado": 30,
  "valorBaseMaoDeObra": 80.00,
  "pecaId": 1,
  "pecaFornecidaPelaLoja": true
}
```

Regras:

- `tipo`, `descricao` e `valorBaseMaoDeObra` são obrigatórios; `valorBaseMaoDeObra` ≥ 0.
- Em `TROCA_PECA`, `pecaFornecidaPelaLoja` é obrigatório.
- Se `pecaFornecidaPelaLoja = true`, `pecaId` é obrigatório e a peça precisa existir (**404**).
  O saldo é conferido ao adicionar e pode retornar **409** `conflito`.
- Se `pecaFornecidaPelaLoja = false`, uma `pecaId` opcional pode ser registrada, mas **não** é
  cobrada nem movimenta estoque.
- Em `REVISAO`, `checklistGeral` é opcional (default lógico `false`).

Cada resposta de serviço inclui `valorFinal` já com o fator de risco aplicado:

```json
{
  "id": 2,
  "tipo": "TROCA_PECA",
  "descricao": "Troca de pastilha",
  "tempoEstimado": 30,
  "valorBaseMaoDeObra": 80.00,
  "valorFinal": 200.00,
  "pecaId": 1,
  "pecaFornecidaPelaLoja": true
}
```

(`valorFinal = 80 × 1,00 + 120 = 200` para moto de 160 cc.)

### `GET /ordens-servico/{id}/orcamento` → 200 / 404

Calcula sem persistir:

```json
{ "totalPecas": 120.00, "totalMaoDeObra": 230.00, "descontoAplicado": 0.00, "totalFinal": 350.00 }
```

As fórmulas estão em [03 — Modelo de domínio](03-modelo-de-dominio.md#precificação).

### `POST /ordens-servico/{id}/confirmar` → 200 / 404 / 409

`ABERTA → EM_ANDAMENTO`, baixa **uma unidade** de cada peça da loja usada em serviços de troca e
devolve a OS atualizada. Confirmar novamente → **409** (transição inválida), o que impede baixa
dupla. Estoque insuficiente no momento da baixa → **409**.

### `POST /ordens-servico/{id}/fechar` → 200 / 404 / 409

Conclui/encerra a OS, grava o snapshot do orçamento, agenda `proximaRevisao = hoje + 6 meses` no
cliente e devolve **o orçamento**, não a OS:

```json
{ "totalPecas": 120.00, "totalMaoDeObra": 230.00, "descontoAplicado": 0.00, "totalFinal": 350.00 }
```

Fechar uma OS `ABERTA` diretamente → **409**, porque `ABERTA → CONCLUIDA` não é permitida. Feche
depois de confirmar (fluxo normal) ou a partir de `EM_ANDAMENTO`/`AGUARDANDO_CLIENTE`.

---

## Fluxo completo com `curl`

Assumindo banco vazio e aplicação em `http://localhost:8080`:

```bash
B=http://localhost:8080

# 0. login (admin criado no boot em dev) e token
TOKEN=$(curl -s -X POST "$B/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"admin","senha":"admin123"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
AUTH="Authorization: Bearer $TOKEN"

# 1. cliente (CPF válido)
curl -s -X POST "$B/clientes" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"nome":"Maria Silva","cpfCnpj":"52998224725","contato":"11999999999"}'

# 2. moto no catálogo
curl -s -X POST "$B/motos" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"marca":"Honda","modelo":"CG 160","cilindradas":160}'

# 3. peça em estoque
curl -s -X POST "$B/pecas" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"codigo":"P-001","descricao":"Pastilha de freio","valor":120.00,"quantidadeEstoque":10,"estoqueMinimo":2}'

# 4. abrir OS
curl -s -X POST "$B/ordens-servico" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"clienteId":1,"marcaMoto":"Honda","modeloMoto":"CG 160"}'

# 5. revisão
curl -s -X POST "$B/ordens-servico/1/servicos" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"tipo":"REVISAO","descricao":"Revisão 10k","tempoEstimado":60,"valorBaseMaoDeObra":150.00,"checklistGeral":true}'

# 6. troca de peça da loja
curl -s -X POST "$B/ordens-servico/1/servicos" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"tipo":"TROCA_PECA","descricao":"Troca pastilha","tempoEstimado":30,"valorBaseMaoDeObra":80.00,"pecaId":1,"pecaFornecidaPelaLoja":true}'

# 7. orçamento → 120.00 / 230.00 / 0.00 / 350.00
curl -s "$B/ordens-servico/1/orcamento" -H "$AUTH"

# 8. confirmar (estoque vai de 10 para 9)
curl -s -X POST "$B/ordens-servico/1/confirmar" -H "$AUTH"

# 9. fechar (snapshot + próxima revisão)
curl -s -X POST "$B/ordens-servico/1/fechar" -H "$AUTH"

# 10. histórico e motos do cliente
curl -s "$B/ordens-servico/1/historico" -H "$AUTH"
curl -s "$B/clientes/1/motos" -H "$AUTH"
```

Saídas esperadas: `201` nos cadastros, `200` nas ações, orçamento `350.00`, estoque final `9`,
histórico com `ABERTA → EM_ANDAMENTO → CONCLUIDA → FECHADA`.

## Observações para consumidores

- Aceite `application/json, application/problem+json` no header `Accept`; o cliente HTTP do
  frontend já faz isso.
- Execute `POST /auth/login` antes de qualquer rota protegida e reenvie o Bearer em cada chamada.
  Downloads (CSV/comprovante) também exigem o header — não coloque o token na URL.
- `204` não tem corpo — trate antes de tentar parsear JSON.
- Não há versionamento de API; mudanças de contrato devem ser refletidas no OpenAPI e nos testes.
- `GET /v3/api-docs` e `/swagger-ui.html` ficam **desabilitados** no perfil `prod`.
- CORS é restrito a `CORS_ALLOWED_ORIGINS` (default `http://localhost:5173` e
  `http://localhost:4173`). Em produção, prefira servir a SPA atrás do mesmo domínio/proxy reverso.
