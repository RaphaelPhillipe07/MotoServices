# Documentação técnica — MotoServices

Esta pasta é o ponto de entrada para quem está pegando o projeto pela primeira vez. O
[`README.md`](../../README.md) da raiz apresenta o produto e o passo a passo de execução; aqui
está o detalhamento técnico do que existe **hoje no código** (não do que está planejado).

> **Regra de ouro:** quando esta documentação divergir do código, o código é a fonte da verdade.
> Ao alterar comportamento, atualize o documento correspondente na mesma mudança.

## Por onde começar

| Se você quer… | Leia |
|---|---|
| Entender o problema, o escopo e o vocabulário | [01 — Visão geral](01-visao-geral.md) |
| Saber como o código está organizado e por quê | [02 — Arquitetura](02-arquitetura.md) |
| Entender entidades, status e cálculo de preço | [03 — Modelo de domínio](03-modelo-de-dominio.md) |
| Chamar ou alterar a API | [04 — API REST](04-api-rest.md) |
| Mexer no banco ou criar uma migração | [05 — Banco de dados](05-banco-de-dados.md) |
| Rodar o projeto localmente, em Docker ou diagnosticar falhas de boot | [06 — Configuração e execução](06-configuracao-e-execucao.md) |
| Rodar, escrever ou entender os testes | [07 — Testes e qualidade](07-testes-e-qualidade.md) |
| Trabalhar na SPA React | [08 — Frontend](08-frontend.md) |
| Ter um roteiro de primeiro dia e saber onde mexer | [09 — Guia do desenvolvedor](09-guia-do-desenvolvedor.md) |
| Saber o que falta, revisão da landing e recomendações | [10 — Pendências e recomendações](10-pendencias-e-recomendacoes.md) |

## Resumo em 5 minutos

- **Produto:** gestão de ordens de serviço (OS) de oficinas de motocicletas — clientes, catálogo de
  motos, estoque de peças, orçamento e ciclo de vida da OS.
- **Backend:** Java 17 + Spring Boot 3.3.5, PostgreSQL, Flyway, Spring Data JPA, Spring Security
  (JWT), SpringDoc. Organizado em camadas `controller → bo → dao`, com VOs de entrada/saída.
- **Frontend:** React 19 + TypeScript + Vite + Tailwind 4 + TanStack Query, consumindo a API em
  `/api` (proxy do Vite em desenvolvimento), com login em `/entrar`.
- **Banco:** schema `motoservices`, migrações Flyway `V1` a `V8`, `ddl-auto: validate`.
- **Qualidade:** `./mvnw verify` (testes, Checkstyle, JaCoCo ≥ 50% de linhas) e
  `npm test && npm run build` no frontend (55 testes).
- **Ainda não existe:** emissão fiscal de NF (NF-e/NFS-e) e notificação de revisão por calendário.
  Ver [01 — Visão geral](01-visao-geral.md#escopo-entregue-vs-roadmap).

## Convenções desta documentação

- Caminhos e comandos assumem a raiz do repositório (`MotoServices/`), salvo indicação contrária.
- No Windows, troque `./mvnw` por `mvnw.cmd`; os demais comandos (npm, curl) são equivalentes.
- Exemplos de API assumem a aplicação em `http://localhost:8080`.
- Valores monetários são `BigDecimal` com 2 casas e arredondamento `HALF_UP`; a API os expõe como
  número JSON (`120.00`).
