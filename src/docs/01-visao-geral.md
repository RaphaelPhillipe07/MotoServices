# 01 — Visão geral

## O problema

Oficinas de motocicleta vendem peças, executam revisões e trocas de peça, e precisam lembrar o
histórico de cada cliente e de cada moto. Planilhas não distinguem com segurança **venda de
produto** de **mão de obra**, não dão baixa confiável no estoque e não preservam o valor cobrado
quando o preço de uma peça muda depois do fechamento.

O MotoServices organiza esse fluxo:

1. O cliente e a moto são identificados (a moto precisa existir no catálogo).
2. Serviços são adicionados a uma OS, indicando se a peça é da loja ou trazida pelo cliente.
3. O orçamento é calculado por regras explícitas (fator de risco por cilindrada + desconto
   progressivo).
4. A confirmação baixa o estoque uma única vez e inicia o serviço.
5. O fechamento congela o orçamento, encerra a OS e agenda a próxima revisão do cliente.

## Atores

| Ator | O que faz no sistema atual |
|---|---|
| **Mecânico / chefe de oficina** | Cadastra cliente/moto, abre a OS, adiciona serviços e confirma o orçamento. |
| **Atendente / caixa** | Consulta a OS, confere o orçamento congelado e fecha a OS. Registro de pagamento e NF ainda não existem. |
| **Cliente** | É cadastrado, vinculado a motos e tem a próxima revisão agendada ao fechar uma OS. |
| **Administrador** | Mantém o catálogo de motos, as peças e o estoque, além de usuários e relatórios. |

O acesso é autenticado por login (JWT) e autorizado por papel: `ADMIN`, `ATENDENTE` e `MECANICO`.
A API só aceita requisições com token Bearer válido, exceto login, health e documentação interativa.

## Escopo entregue vs. roadmap

### Entregue

- Cadastro e busca de clientes (nome, CPF ou CNPJ), com validação de dígito verificador.
- Catálogo de motos (marca, modelo, cilindradas) com unicidade case-insensitive.
- Vínculo cliente ↔ moto, criado automaticamente na primeira OS e gerenciável por endpoint.
- Estoque de peças: cadastro, busca, entrada de compra, alerta de ruptura e baixa automática.
- OS: abertura, múltiplos serviços, orçamento, confirmação, fechamento e histórico de status.
- Precificação: fator de risco por cilindrada, peça da loja vs. peça do cliente, desconto de 5%.
- Snapshot do orçamento no fechamento e controle de concorrência otimista.
- API documentada via OpenAPI/Swagger e erros em RFC 9457.
- SPA React: painel, ordens, clientes, motos e estoque.
- Login JWT, autorização por papéis e sessão persistida no frontend.

### Roadmap (não implementado)

- Emissão de comprovante/nota fiscal (NF) e registro de forma de pagamento.
- Relatórios e faturamento por período (a query `OrdemDeServicoDAO.buscarPorPeriodo` já existe,
  porém não é usada por nenhum endpoint).
- Notificação de revisão por calendário (hoje só a data `proximaRevisao` é gravada).
- Cancelamento de OS e endpoints de exclusão de cliente/moto/peça.
- Testes de integração de DAO e um smoke test E2E automatizado da API.

## Vocabulário do domínio

| Termo | Significado no projeto |
|---|---|
| **OS** | Ordem de serviço: unidade de trabalho de uma moto para um cliente. |
| **Peça da loja** | Peça do estoque da oficina; entra no total de peças e sofre baixa ao confirmar. |
| **Peça do cliente** | Peça trazida pelo cliente; não é cobrada e não movimenta estoque, só a mão de obra. |
| **Serviço** | Linha da OS: `REVISAO` ou `TROCA_PECA`, com descrição e valor base de mão de obra. |
| **Orçamento** | Totais calculados: peças, mão de obra, desconto e total final. |
| **Snapshot** | Cópia do orçamento gravada na OS no fechamento; não muda se preços mudarem depois. |
| **Ruptura** | Peça cuja `quantidadeEstoque` está abaixo de `estoqueMinimo`. |
| **BO** | *Business Object*: camada de regras de negócio (equivalente a *service*). |
| **DAO** | *Data Access Object*: interfaces Spring Data JPA. |
| **VO** | Objeto de transporte entre camadas/API; aqui inclui requests e responses. |
| **Fator de risco** | Multiplicador da mão de obra conforme a cilindrada da moto. |

## Princípios que o código segue

1. **O domínio não conhece HTTP.** Entidades e `Precificacao` calculam; controllers só traduzem
   requisição/resposta e devolvem status.
2. **Nenhum controller acessa DAO.** A ordem é sempre `controller → BO → DAO`.
3. **Entrada e saída têm contratos próprios** (`*RequestVO`, `*ResponseVO`), convertidos por
   mappers estáticos; entidades JPA não são serializadas na API.
4. **Erros são parte do contrato:** toda falha esperada vira `application/problem+json` com um
   `type` estável (ver [04 — API REST](04-api-rest.md#erros-rfc-9457)).
5. **Dinheiro é `BigDecimal`** com escala 2 e `HALF_UP`; nunca `double`.
6. **O banco valida o que é estrutural** (unicidade, FK, CHECK, versão) e o BO valida regra de
   negócio — as duas camadas são intencionais, não redundantes.

## Limitações estruturais conscientes

- Monólito de um único processo, sem filas, cache ou eventos.
- Sem multi-tenancy: uma instalação atende uma oficina.
- Paginação por offset (não há cursor).
- A API não tem versionamento de URL (`/v1`); o contrato é o OpenAPI gerado.
- O frontend precisa de proxy reverso para `/api` em produção, pois o backend não configura CORS.
