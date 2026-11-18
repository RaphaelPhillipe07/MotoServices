# 03 — Modelo de domínio

## Entidades e relacionamentos

```text
Cliente (tb_cliente) 1 ──── * OrdemDeServico (tb_ordem_de_servico) * ──── 1 Moto (tb_moto)
   │                                   │
   │ *                                 │ 1
   │                                   │
   │                             * ServicoDaOficina (tb_servico_da_oficina)
   │                                   △
   │                          ┌────────┴─────────┐
   │                   ServicoRevisao      ServicoTrocaPeca * ──── 0..1 Peca
   │                   (tb_servico_revisao) (tb_servico_troca_peca)     (tb_peca)
   │
   └── * Moto  (via tb_cliente_moto, N:N)

OrdemDeServico 1 ──── * HistoricoStatusOS (tb_os_status_historico, ON DELETE CASCADE)
```

- `Cliente.motos` é um `@ManyToMany` com tabela de junção `tb_cliente_moto`.
- `OrdemDeServico.servicos` é `@OneToMany(mappedBy = "ordemDeServico", cascade = ALL,
  orphanRemoval = true)`; adicionar/remover serviços é feito pelos métodos da entidade.
- `ServicoDaOficina` é abstrata com herança `JOINED`; o subtipo é decidido pelo tipo enviado na
  requisição, não por um campo discriminador do cliente.
- `HistoricoStatusOS` é imutável por convenção: só é inserido, nunca alterado.

## Campos das entidades

### `Cliente` → `motoservices.tb_cliente`

| Campo | Tipo Java | Coluna | Regra |
|---|---|---|---|
| `id` | `Long` | `id` | `BIGSERIAL` |
| `nome` | `String` | `nome` | obrigatório, ≤ 200 |
| `cpfCnpj` | `String` | `cpf_cnpj` | obrigatório, único, sem pontuação, 11 ou 14 dígitos válidos |
| `contato` | `String` | `contato` | obrigatório, ≤ 100 |
| `proximaRevisao` | `LocalDate` | `proxima_revisao` | opcional; no cadastro não pode estar no passado |
| `motos` | `Set<Moto>` | — | N:N via `tb_cliente_moto` |

### `Moto` → `motoservices.tb_moto`

`id`, `marca` (≤ 100), `modelo` (≤ 100), `cilindradas` (inteiro > 0). O par marca+modelo é único
sem diferenciar maiúsculas (`lower(marca), lower(modelo)`).

### `Peca` → `motoservices.tb_peca`

| Campo | Tipo | Regra |
|---|---|---|
| `id` | `Long` | PK |
| `codigo` | `String` | obrigatório, ≤ 50, único |
| `descricao` | `String` | obrigatório, ≤ 255 |
| `valor` | `BigDecimal(12,2)` | ≥ 0 |
| `quantidadeEstoque` | `Integer` | ≥ 0 |
| `estoqueMinimo` | `Integer` | ≥ 0 |
| `versao` | `Long` | `@Version` (concorrência otimista) |

`isAbaixoDoMinimo()` retorna `quantidadeEstoque < estoqueMinimo`.

### `OrdemDeServico` → `motoservices.tb_ordem_de_servico`

| Campo | Tipo | Observação |
|---|---|---|
| `id` | `Long` | PK |
| `cliente`, `moto` | `ManyToOne` | obrigatórios |
| `status` | `StatusOS` | persistido como texto (`VARCHAR(30)`) |
| `servicos` | `List<ServicoDaOficina>` | cascade total |
| `dataAbertura` | `LocalDateTime` | preenchida ao abrir |
| `dataFechamento` | `LocalDateTime` | preenchida na transição para `FECHADA` |
| `totalPecas`, `totalMaoDeObra`, `descontoAplicado`, `totalFinal` | `BigDecimal(12,2)` | nulos até o fechamento (snapshot) |
| `versao` | `Long` | `@Version` |

### `ServicoDaOficina` e subtipos

| Classe | Tabela | Campos próprios |
|---|---|---|
| `ServicoDaOficina` (abstrata) | `tb_servico_da_oficina` | `descricao` (obrigatória), `tempoEstimado` (opcional), `valorBaseMaoDeObra` (≥ 0), FK para a OS |
| `ServicoRevisao` | `tb_servico_revisao` | `checklistGeral` (booleano opcional) |
| `ServicoTrocaPeca` | `tb_servico_troca_peca` | `pecaEnvolvida` (opcional), `pecaFornecidaPelaLoja` (obrigatório) |

### `HistoricoStatusOS` → `motoservices.tb_os_status_historico`

`id`, `ordemDeServico` (FK, cascade delete), `statusAnterior` (nulo na abertura), `statusNovo`,
`dataHora`. Uma linha por transição, inclusive a abertura (`null → ABERTA`).

## Máquina de estados da OS

```text
ABERTA ──► EM_ANDAMENTO ──► CONCLUIDA ──► FECHADA
                 ▲   │           ▲
                 │   ▼           │
          AGUARDANDO_CLIENTE ────┘
```

| De | Para (permitido) |
|---|---|
| `ABERTA` | `EM_ANDAMENTO`, `CANCELADA` |
| `EM_ANDAMENTO` | `AGUARDANDO_CLIENTE`, `CONCLUIDA`, `CANCELADA` |
| `AGUARDANDO_CLIENTE` | `EM_ANDAMENTO`, `CONCLUIDA`, `CANCELADA` |
| `CONCLUIDA` | `FECHADA` |
| `FECHADA` | — (terminal) |
| `CANCELADA` | — (terminal) |

Regras associadas:

- Transição não listada → `TransicaoStatusInvalidaException` → HTTP **409**.
- Mesmo status para o mesmo status → inválido.
- Serviços só podem ser adicionados em `ABERTA`, `EM_ANDAMENTO` ou `AGUARDANDO_CLIENTE`.
- `confirmarOS` faz apenas `ABERTA → EM_ANDAMENTO`. Confirmar de novo (ou de outro status) → 409,
  o que impede baixa dupla de estoque.
- `fecharOS` conclui (`→ CONCLUIDA`, se necessário) e encerra (`→ FECHADA`) na mesma transação;
  fechar uma OS `ABERTA` diretamente é rejeitado porque `ABERTA → CONCLUIDA` não é permitido.
- Ao entrar em `FECHADA`, `dataFechamento` é preenchida (se ainda não estava).

> Não existe endpoint para mover a OS para `AGUARDANDO_CLIENTE` ou `CONCLUIDA` isoladamente. Esses
> estados são alcançáveis apenas por código/evolução futura; via API, o fluxo é
> abrir → confirmar → fechar.

## Precificação

Fonte única: `br.com.motoservices.model.Precificacao`.

### Fator de risco da mão de obra

| Cilindradas | Fator | Exemplo sobre R$ 100,00 |
|---|---|---|
| ≤ 400 | `1,00` | R$ 100,00 |
| 401 a 650 | `1,50` | R$ 150,00 |
| > 650 | `1,70` | R$ 170,00 |

Aplicado **por serviço**, sobre `valorBaseMaoDeObra`, com arredondamento para 2 casas
(`HALF_UP`). Revisão e troca de peça recebem o mesmo fator.

### Peças

- `ServicoTrocaPeca` com `pecaFornecidaPelaLoja = true`: soma o `valor` da peça ao total de peças.
- `pecaFornecidaPelaLoja = false`: nenhuma peça é cobrada; resta apenas a mão de obra.
- Cada serviço de troca com peça da loja consome **1 unidade** do estoque (`QUANTIDADE_POR_SERVICO`).
- O desconto conta **serviços de troca com peça da loja**, não quantidades.

#### Exemplo — peça da loja

Moto 160 cc, 1 revisão com base R$ 150 e 1 troca com base R$ 80 + peça de R$ 120:

```text
mão de obra = 150 × 1,00 + 80 × 1,00 = 230,00
peças       = 120,00
desconto    = 0,00 (apenas 1 peça da loja)
total final = 120,00 − 0,00 + 230,00 = 350,00
```

#### Exemplo — peça do cliente

Moto 700 cc, troca com base R$ 100 e peça do cliente:

```text
mão de obra = 100 × 1,70 = 170,00
peças       = 0,00
total final = 170,00
```

### Desconto progressivo

| Peças da loja na OS | Desconto |
|---|---|
| 0 a 4 | 0% |
| ≥ 5 | 5% sobre o total de peças |

`totalFinal = totalPecas − desconto + totalMaoDeObra`.

#### Exemplo — 5 peças da loja, moto 700 cc

5 serviços de troca, cada um com base R$ 100 e peça de R$ 100:

```text
peças       = 500,00
desconto    = 25,00
mão de obra = 5 × (100 × 1,70) = 850,00
total final = 500,00 − 25,00 + 850,00 = 1.325,00
```

### Arredondamento

- Cada mão de obra é arredondada para 2 casas antes de somar.
- Totais de peças e mão de obra são arredondados ao final do cálculo.
- `desconto` também é arredondado; `totalFinal` é arredondado após a soma.

## Regras por agregado

### Cliente

- `cpfCnpj` é normalizado (remove tudo que não é dígito) antes de validar e persistir.
- CPF (11) e CNPJ (14) precisam ter dígitos verificadores válidos; sequências de dígitos iguais são
  rejeitadas. O mesmo documento não pode ser cadastrado duas vezes.
- Busca: `nomeOuCpf` é obrigatório. Se, após normalizar, tiver 11 ou 14 dígitos, faz busca exata por
  documento e devolve no máximo 1 item. Caso contrário, busca por nome (`contains`, ignorando
  maiúsculas) de forma paginada.
- Vincular uma moto é idempotente; desvincular também. A primeira OS de um cliente com uma moto cria
  o vínculo automaticamente.
- Ao fechar uma OS, `proximaRevisao = hoje + 6 meses` (`PERIODICIDADE_REVISAO_MESES`).

### Moto

- Só pode ser usada em OS se existir no catálogo (`MotoInvalidaException` → 404).
- Marca+modelo não podem se repetir com diferença apenas de caixa.
- Filtro por faixa inclusiva `minimo..maximo`; `minimo > 0` e `maximo >= minimo`.

### Peça / estoque

- Código único; `valor`, `quantidadeEstoque` e `estoqueMinimo` não podem ser negativos.
- Entrada de estoque exige quantidade positiva e soma ao saldo.
- Ao adicionar troca com peça da loja, o BO **checa** o saldo; a **baixa** acontece somente na
  confirmação da OS.
- Baixa concorrente sem saldo → `EstoqueInsuficienteException` → 409.
- Ruptura = peças com `quantidadeEstoque < estoqueMinimo` (comparação coluna a coluna).

### Ordem de serviço

- Só é aberta para cliente e moto existentes; o par cliente+moto é vinculado.
- `TROCA_PECA` exige `pecaFornecidaPelaLoja`; se `true`, exige `pecaId` de peça existente.
- `REVISAO` aceita `checklistGeral` (opcional).
- Serviços não podem ser adicionados após `CONCLUIDA`/`FECHADA`.
- `fecharOS` grava o snapshot, agenda a próxima revisão do cliente e devolve o orçamento final.

## Exceções de domínio → HTTP

| Exceção | HTTP | Quando |
|---|---|---|
| `ValidacaoException` | 400 | Regra de negócio/entrada inválida detectada no BO |
| `MotoInvalidaException` | 404 | Moto inexistente no catálogo |
| `ClienteNaoEncontradoException` | 404 | Cliente inexistente |
| `PecaNaoEncontradaException` | 404 | Peça inexistente |
| `OrdemDeServicoNaoEncontradaException` | 404 | OS inexistente |
| `EstoqueInsuficienteException` | 409 | Saldo insuficiente/alterado concorrentemente |
| `TransicaoStatusInvalidaException` | 409 | Transição não permitida |

O mapeamento completo (incluindo Bean Validation, JSON malformado, 405, 415 e fallback 500) está em
[04 — API REST](04-api-rest.md#erros-rfc-9457).
