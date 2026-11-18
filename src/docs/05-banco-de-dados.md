# 05 — Banco de dados

## Visão geral

- **SGBD:** PostgreSQL (testado com 14.22 no PostgreSQL embarcado dos testes; Docker Compose usa 16).
- **Schema:** `motoservices` (criado pela V1). As entidades JPA usam `schema = "MOTOSERVICES"`;
  o PostgreSQL normaliza para minúsculas.
- **Migrações:** Flyway em `src/main/resources/db/migration`, aplicadas no boot.
- **Mapeamento:** Hibernate com `ddl-auto: validate` — ele **não cria nem altera** tabelas; apenas
  confere se entidades e schema batem. Divergência = falha de inicialização.
- **Nomenclatura:** tabelas `tb_*`, colunas `snake_case`, PK `id`, FKs `<entidade>_id`, índices
  `idx_*`, unicidade `uk_*`, constraints `ck_*`/`fk_*`/`pk_*`.

## Tabelas

### `tb_moto`

| Coluna | Tipo | Restrições |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `marca` | `VARCHAR(100)` | NOT NULL |
| `modelo` | `VARCHAR(100)` | NOT NULL |
| `cilindradas` | `INTEGER` | NOT NULL |

Índice único funcional `uk_moto_marca_modelo` em `(lower(marca), lower(modelo))`.

### `tb_cliente`

| Coluna | Tipo | Restrições |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `nome` | `VARCHAR(200)` | NOT NULL |
| `cpf_cnpj` | `VARCHAR(20)` | NOT NULL, UNIQUE |
| `contato` | `VARCHAR(100)` | NOT NULL |
| `proxima_revisao` | `DATE` | NULL |

Índice GIN `idx_cliente_nome_trgm` em `lower(nome)` com `pg_trgm` para busca por trecho.

### `tb_peca`

| Coluna | Tipo | Restrições |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `codigo` | `VARCHAR(50)` | NOT NULL, UNIQUE |
| `descricao` | `VARCHAR(255)` | NOT NULL |
| `valor` | `NUMERIC(12,2)` | NOT NULL, CHECK `>= 0` |
| `quantidade_estoque` | `INTEGER` | NOT NULL, CHECK `>= 0` |
| `estoque_minimo` | `INTEGER` | NOT NULL, CHECK `>= 0` |
| `versao` | `BIGINT` | NOT NULL DEFAULT 0 (`@Version`) |

### `tb_ordem_de_servico`

| Coluna | Tipo | Restrições |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `cliente_id` | `BIGINT` | NOT NULL, FK → `tb_cliente` |
| `moto_id` | `BIGINT` | NOT NULL, FK → `tb_moto` |
| `status` | `VARCHAR(30)` | NOT NULL, CHECK nos 5 status |
| `data_abertura` | `TIMESTAMP` | NOT NULL |
| `data_fechamento` | `TIMESTAMP` | NULL |
| `total_pecas` | `NUMERIC(12,2)` | NULL (snapshot) |
| `total_mao_de_obra` | `NUMERIC(12,2)` | NULL (snapshot) |
| `desconto_aplicado` | `NUMERIC(12,2)` | NULL (snapshot) |
| `total_final` | `NUMERIC(12,2)` | NULL (snapshot) |
| `forma_pagamento` | `VARCHAR(30)` | NULL, CHECK nas formas suportadas |
| `versao` | `BIGINT` | NOT NULL DEFAULT 0 (`@Version`) |

### `tb_servico_da_oficina` (raiz da herança)

| Coluna | Tipo | Restrições |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `descricao` | `VARCHAR(255)` | NOT NULL |
| `tempo_estimado` | `INTEGER` | NULL |
| `valor_base_mao_de_obra` | `NUMERIC(12,2)` | NOT NULL, CHECK `>= 0` |
| `ordem_de_servico_id` | `BIGINT` | NULL, FK → `tb_ordem_de_servico` |

### `tb_servico_revisao`

| Coluna | Tipo | Restrições |
|---|---|---|
| `servico_id` | `BIGINT` | PK, FK → `tb_servico_da_oficina` |
| `checklist_geral` | `BOOLEAN` | NULL |

### `tb_servico_troca_peca`

| Coluna | Tipo | Restrições |
|---|---|---|
| `servico_id` | `BIGINT` | PK, FK → `tb_servico_da_oficina` |
| `peca_id` | `BIGINT` | NULL, FK → `tb_peca` |
| `peca_fornecida_pela_loja` | `BOOLEAN` | NOT NULL |

### `tb_os_status_historico`

| Coluna | Tipo | Restrições |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `ordem_de_servico_id` | `BIGINT` | NOT NULL, FK → `tb_ordem_de_servico` `ON DELETE CASCADE` |
| `status_anterior` | `VARCHAR(30)` | NULL, CHECK nos 5 status |
| `status_novo` | `VARCHAR(30)` | NOT NULL, CHECK nos 5 status |
| `data_hora` | `TIMESTAMP` | NOT NULL |

### `tb_usuario`

| Coluna | Tipo | Restrições |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `username` | `VARCHAR(60)` | NOT NULL, UNIQUE, índice `idx_usuario_username` |
| `nome` | `VARCHAR(200)` | NOT NULL |
| `senha` | `VARCHAR(100)` | NOT NULL, hash BCrypt (nunca texto puro) |
| `papel` | `VARCHAR(30)` | NOT NULL, CHECK em `ADMIN`, `ATENDENTE`, `MECANICO` |
| `ativo` | `BOOLEAN` | NOT NULL, default `TRUE`; usuário inativo não autentica |

### `tb_cliente_moto` (junção N:N)

| Coluna | Tipo | Restrições |
|---|---|---|
| `cliente_id` | `BIGINT` | PK, FK → `tb_cliente` `ON DELETE CASCADE` |
| `moto_id` | `BIGINT` | PK, FK → `tb_moto` `ON DELETE CASCADE` |

## Migrações Flyway

| Versão | Arquivo | Conteúdo |
|---|---|---|
| V1 | `V1__init_schema.sql` | Schema + tabelas base, FKs e hierarquia de serviços |
| V2 | `V2__indices_e_constraints.sql` | Índices de FK, unicidade case-insensitive de moto, `pg_trgm`, índice trigram de cliente e CHECKs |
| V3 | `V3__versao_otimista.sql` | Coluna `versao` em `tb_peca` e `tb_ordem_de_servico` |
| V4 | `V4__snapshot_orcamento.sql` | Colunas de snapshot do orçamento na OS |
| V5 | `V5__historico_status_os.sql` | Tabela e índice do histórico de status |
| V6 | `V6__cliente_moto.sql` | Tabela de vínculo cliente↔moto |
| V7 | `V7__cancelamento_e_pagamento.sql` | Status `CANCELADA`, forma de pagamento e índices de revisão/fechamento |
| V8 | `V8__usuarios.sql` | Tabela `tb_usuario` com papel (`ADMIN`/`ATENDENTE`/`MECANICO`), senha BCrypt, flag `ativo` e índice de username |

Configuração em `application.yml`:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    default-schema: motoservices
    schemas: motoservices
  jpa:
    hibernate:
      ddl-auto: validate
```

### Regras para novas migrações

1. **Nunca edite** um arquivo já aplicado. O Flyway valida o checksum e a aplicação falha no boot
   (`FlywayValidateException`/checksum mismatch) em bancos que já rodaram a versão antiga.
2. Crie `V9__descricao_curta.sql` (ou a próxima versão livre) com DDL idempotente quando possível.
3. Atualize a entidade JPA correspondente **e** o teste `MigrationsTest`, que hoje verifica
   explicitamente `migrationsExecuted == 8`, além de índices, CHECKs, `pg_trgm` e colunas.
4. Se a mudança afetar o domínio, atualize VOs/mappers, a API e esta documentação.
5. Rode `./mvnw verify` — o teste de migração usa PostgreSQL real embarcado, não H2.

## Índices e por que existem

| Índice | Motivo |
|---|---|
| `idx_os_cliente`, `idx_os_moto`, `idx_os_status`, `idx_os_data_abertura` | Filtros/listagens de OS. No PostgreSQL, FK **não** cria índice automaticamente. |
| `idx_servico_os` | Carregar serviços de uma OS. |
| `idx_troca_peca_peca` | Consultas por peça usada. |
| `idx_moto_cilindradas` | Filtro por faixa de cilindradas. |
| `uk_moto_marca_modelo` | Unicidade case-insensitive do catálogo. |
| `idx_cliente_nome_trgm` | Busca por nome com `contains` usando trigramas (`pg_trgm`). |
| `idx_historico_os_data` | Histórico por OS ordenado por data. |
| `idx_cm_moto` | Navegação reversa moto → clientes. |
| `idx_cliente_proxima_revisao` | Agenda de revisões próximas. |
| `idx_os_data_fechamento`, `idx_os_status_fechamento` | Relatórios por período de fechamento. |

## CHECKs

- `ck_peca_valor_nao_negativo`, `ck_peca_estoque_nao_negativo`,
  `ck_peca_estoque_minimo_nao_negativo`.
- `ck_servico_valor_base_nao_negativo`.
- `ck_os_status_valido` e os CHECKs de `status_anterior`/`status_novo` no histórico.

Essas constraints são a última linha de defesa; o BO também valida (mensagens melhores e 400 em vez
de 409). Uma violação que escape do BO chega como `DataIntegrityViolationException` → **409**.

## Concorrência

### Optimistic locking (`@Version`)

`Peca` e `OrdemDeServico` têm `@Version`. Duas transações que carregam e gravam a mesma linha: a
segunda recebe `OptimisticLockException`/`ObjectOptimisticLockingFailureException`. O
`GlobalExceptionHandler` **não** trata esse tipo especificamente; ele cai no fallback **500**. É uma
limitação conhecida (o esperado seria 409).

### Baixa de estoque atômica

`PecaDAO.baixarEstoque` não faz `read-modify-write` em Java:

```sql
UPDATE motoservices.tb_peca
   SET quantidade_estoque = quantidade_estoque - :quantidade,
       versao = versao + 1
 WHERE id = :pecaId
   AND quantidade_estoque >= :quantidade
```

- Retorna `1` se baixou e `0` se não havia saldo (ou outra transação já consumiu).
- `versao` é incrementada manualmente porque bulk updates não passam pelo ciclo do Hibernate.
- `@Modifying(clearAutomatically = true, flushAutomatically = true)` evita entidade obsoleta no
  contexto de persistência.
- `EstoqueBO.baixarEstoque` traduz `0` linhas em `EstoqueInsuficienteException` (409).

Assim, duas confirmações simultâneas da mesma OS não conseguem deixar o saldo negativo.

## Inspeção e manutenção

Com um PostgreSQL local (`psql`) ou dentro do container:

```bash
# tabelas e índices
psql -U postgres -d motoservices -c '\dt motoservices.*'
psql -U postgres -d motoservices -c '\di motoservices.*'

# versão aplicada das migrações
psql -U postgres -d motoservices -c 'select version, description, success from motoservices.flyway_schema_history;'

# peças em ruptura (mesma regra do endpoint)
psql -U postgres -d motoservices -c \
  'select codigo, quantidade_estoque, estoque_minimo from motoservices.tb_peca where quantidade_estoque < estoque_minimo;'

# recriar o schema do zero (destrutivo, apenas em dev)
psql -U postgres -d motoservices -c 'drop schema motoservices cascade;'
```

Não há *seed*/dados iniciais em migrações: o catálogo de motos e as peças começam vazios. Testes e
smoke tests inserem os próprios dados.
