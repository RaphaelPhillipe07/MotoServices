# 06 — Configuração e execução

## Pré-requisitos

| Ferramenta | Versão | Observação |
|---|---|---|
| JDK | 17 ou superior | O CI e o Docker usam 17; o projeto também roda em 21. |
| Maven | não precisa instalar | Use o wrapper (`./mvnw` / `mvnw.cmd`), que baixa o Maven. |
| PostgreSQL | 14 ou superior | O Compose usa 16. Não é necessário para rodar os testes. |
| Docker + Docker Compose | opcional | Atalho para subir banco + aplicação. |
| Node.js | 22 (recomendado) | O CI usa Node 22; `frontend/package-lock.json` deve ser respeitado (`npm ci`). |
| npm | incluso no Node | — |

> Neste repositório não há Maven instalado globalmente nem dependência de Docker para os testes:
> a suíte de integração usa PostgreSQL embarcado (ver [07 — Testes](07-testes-e-qualidade.md)).

## Perfis Spring

A aplicação sempre parte de `application.yml` (base) e sobrepõe com o perfil ativo.
O perfil padrão é **`dev`**.

| Perfil | Arquivo | Datasource | Swagger | Actuator | Log |
|---|---|---|---|---|---|
| `dev` | `application-dev.yml` | `DB_HOST`/`DB_PORT`/`DB_NAME` com defaults `localhost`/`5432`/`motoservices`; usuário/senha default `postgres`/`postgres` | habilitado | `health`, `info` | Hibernate SQL em `debug` |
| `prod` | `application-prod.yml` | exige `DB_URL`, `DB_USER`, `DB_PASS` (sem default) | desabilitado | somente `health` | root `warn`, app `info` |

Base (`application.yml`), independente do perfil:

- `spring.jpa.hibernate.ddl-auto: validate` (o schema é responsabilidade do Flyway).
- `spring.jpa.open-in-view: false` (não há lazy loading fora da transação).
- `default_schema: motoservices`.
- Flyway habilitado, `locations: classpath:db/migration`.
- Paginação: página 20 por padrão, máximo 100.

### Variáveis de ambiente

| Variável | Usada em | Default (dev) | Obrigatória em `prod` |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | ambos | `dev` | — (o Compose define `prod`) |
| `DB_HOST` | dev | `localhost` | — |
| `DB_PORT` | dev | `5432` | — |
| `DB_NAME` | dev | `motoservices` | — |
| `DB_USER` | dev e prod | `postgres` | **sim** |
| `DB_PASS` | dev e prod | `postgres` | **sim** |
| `DB_URL` | prod | — | **sim** (ex.: `jdbc:postgresql://postgres:5432/motoservices`) |
| `JWT_SECRET` | ambos | segredo local de 32+ caracteres | **sim** |
| `JWT_EXPIRATION_MINUTES` | ambos | `60` | — |
| `ADMIN_USERNAME` | ambos | `admin` | — |
| `ADMIN_PASSWORD` | ambos | `admin123` (dev) | recomendada (se a tabela estiver vazia) |
| `ADMIN_NOME` | ambos | `Administrador` | — |
| `ADMIN_BOOTSTRAP` | ambos | `true` | — |
| `CORS_ALLOWED_ORIGINS` | ambos | `http://localhost:5173,http://localhost:4173` | conforme o deploy |

## Execução local (backend)

```bash
# 1. criar o banco uma vez (o schema é criado pelo Flyway)
createdb motoservices

# 2. opcional: sobrescrever os defaults
export DB_HOST=localhost DB_PORT=5432 DB_NAME=motoservices DB_USER=postgres DB_PASS=postgres

# 3. subir
./mvnw spring-boot:run
# Windows: mvnw.cmd spring-boot:run
```

Alternativa com jar empacotado:

```bash
./mvnw -DskipTests package
DB_HOST=localhost DB_PORT=5432 DB_NAME=motoservices DB_USER=postgres DB_PASS=postgres \
  java -jar target/motoservices-0.0.1-SNAPSHOT.jar
```

O que acontece no boot:

1. Spring carrega `application.yml` + `application-dev.yml`.
2. O datasource é criado; se o banco não existir, a conexão falha antes de qualquer migração.
3. O Flyway aplica as migrações pendentes (`V1`…`V8`) no schema `motoservices`.
4. O Hibernate valida o mapeamento (`ddl-auto: validate`). Divergência → falha de boot.
5. Se a tabela de usuários estiver vazia, o `AdminBootstrap` cria o administrador a partir de
   `ADMIN_USERNAME`/`ADMIN_PASSWORD` (hash BCrypt).
6. O Tomcat sobe na porta 8080.
7. Log esperado: `Started MotoServicesApplication in ... seconds`.

URLs úteis:

| Recurso | URL |
|---|---|
| API | `http://localhost:8080` |
| Swagger UI (dev) | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON (dev) | `http://localhost:8080/v3/api-docs` |
| Health | `http://localhost:8080/actuator/health` |
| Info | `http://localhost:8080/actuator/info` (somente dev) |

## Docker Compose (perfil `prod`)

```bash
docker compose up --build
```

O `docker-compose.yml` define:

- `postgres`: imagem `postgres:16-alpine`, banco/usuário/senha `motoservices`, volume `pgdata`,
  `healthcheck` com `pg_isready` e porta `5432`.
- `app`: build do `Dockerfile`, depende do banco saudável, recebe
  `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USER`, `DB_PASS`, `JWT_SECRET` (default local) e
  `ADMIN_PASSWORD`, publica `8080` e reinicia sozinho. Troque o segredo e a senha antes de expor.

O `Dockerfile` é multi-stage: `eclipse-temurin:17-jdk` para build (baixa dependências e empacota sem
testes) e `eclipse-temurin:17-jre` para runtime, com usuário não-root `app`.

Em `prod`, o Swagger fica desligado e o Actuator expõe apenas `/actuator/health`.

## Execução do frontend

```bash
cd frontend
npm ci            # instala exatamente o lockfile
npm run dev       # http://localhost:5173, proxy /api → http://localhost:8080
```

O proxy do Vite remove o prefixo `/api` (`/api/clientes` → `http://localhost:8080/clientes`),
então o backend não precisa de configuração de CORS em desenvolvimento. O painel abre em
`http://localhost:5173/entrar`; em dev, o administrador inicial é `admin` / `admin123`.

Build e pré-visualização:

```bash
npm run build     # tsc + vite build → frontend/dist
npm run preview   # http://localhost:4173, mesmo proxy /api → :8080
```

Para apontar para outra API, defina `VITE_API_BASE_URL` no momento do build
(ex.: `VITE_API_BASE_URL=https://api.exemplo.com`). Sem essa variável, o padrão é `/api`.

## Testes rápidos de fumaça

```bash
curl -s http://localhost:8080/actuator/health
# {"status":"UP"}

curl -s http://localhost:8080/v3/api-docs | head -c 200
```

## Diagnóstico de falhas de inicialização

| Sintoma | Causa provável | Como resolver |
|---|---|---|
| `Connection refused` / `FATAL: database "motoservices" does not exist` | Banco não está rodando ou não foi criado | Suba o PostgreSQL, crie o banco ou ajuste `DB_*`. |
| `Schema-validation: missing table [...]` | Entidade JPA sem migração correspondente (ou migração não aplicada) | Crie/ajuste a migração, não use `ddl-auto: update`. |
| `Schema-validation: wrong column type` | Tipo da coluna divergente da entidade | Corrija a migração (nova versão) ou o mapeamento. |
| `FlywayValidateException: Checksum mismatch` | Arquivo de migração já aplicado foi editado | Restaure o arquivo original e crie uma nova versão `V7__...`. |
| `Port 8080 was already in use` | Outra instância no ar | Encerre o processo ou use `--server.port=8081`. |
| `Could not resolve placeholder 'DB_URL'` | Perfil `prod` sem variáveis obrigatórias | Defina `DB_URL`, `DB_USER` e `DB_PASS`. |
| `/swagger-ui.html` retorna 404 | Perfil `prod` (Swagger desabilitado) | Use `dev` ou consulte `/v3/api-docs` apenas em dev. |
| Build falha por linha > 160 ou import não usado | Checkstyle | Corrija o código; o `verify` só passa com 0 violações. |
| `npm ci` falha | `package-lock.json` divergente do `package.json` | Rode `npm install` e commite o lockfile atualizado. |

## Discrepâncias de versão conhecidas

O projeto declara versões diferentes em pontos distintos; isso é inofensivo, mas confunde:

| Onde | Versão |
|---|---|
| `pom.xml` | `0.0.1-SNAPSHOT` |
| `OpenApiConfig` (`info.version`) | `0.2.0` |
| `application.yml` (`info.app.version`) | `0.4.0` |

Ao publicar uma release, alinhe as três.

## Dicas de ambiente

- **Sem PostgreSQL local e sem Docker?** Os testes continuam rodando (PostgreSQL embarcado). Para
  subir a aplicação manualmente, instale o PostgreSQL ou use o container apenas do banco
  (`docker compose up postgres`).
- **Windows:** use `mvnw.cmd`; o Flyway/PostgreSQL embarcado nos testes extrai binários em diretório
  temporário e o primeiro boot é mais lento.
- **Porta 5432 ocupada:** ajuste `DB_PORT` (dev) ou o mapeamento de portas no Compose.
- **Logs:** em `dev`, o SQL do Hibernate aparece em `debug`; para reduzir ruído, rode com
  `--logging.level.org.hibernate.SQL=info`.
