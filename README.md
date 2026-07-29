# task-management-java

API REST demonstrativa para gerenciamento de tarefas, construída para portfólio técnico com autenticação JWT, PostgreSQL, Flyway, Spring Security, validação, tratamento global de erros, Swagger/OpenAPI e rate limiting em memória.

## Tecnologias

- Java 25
- Spring Boot 4.1
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Bean Validation
- PostgreSQL
- Flyway
- JJWT
- Springdoc OpenAPI / Swagger UI
- Spring Boot Actuator
- JUnit, Mockito, MockMvc e H2 apenas para testes

## Pre-requisitos

- Java compatível com a versão configurada no `pom.xml`
- Maven ou Maven Wrapper
- PostgreSQL instalado diretamente no host

Nao ha Docker, Docker Compose ou Testcontainers para PostgreSQL nesta etapa.

## Banco local

Crie o banco no PostgreSQL local:

```sql
CREATE DATABASE task_management;
```

Opcionalmente, crie um usuario dedicado:

```sql
CREATE USER task_management_user WITH PASSWORD 'change-me';
GRANT ALL PRIVILEGES ON DATABASE task_management TO task_management_user;
\c task_management
GRANT USAGE, CREATE ON SCHEMA public TO task_management_user;
```

As tabelas sao criadas pelo Flyway em:

- `src/main/resources/db/migration/V1__create_users_table.sql`
- `src/main/resources/db/migration/V2__create_tasks_table.sql`

## Variaveis de ambiente

Valores principais:

```bash
export DATABASE_URL="jdbc:postgresql://localhost:5432/task_management"
export DATABASE_USERNAME="postgres"
export DATABASE_PASSWORD="postgres"
export JWT_SECRET="change-this-local-secret-with-at-least-32-bytes"
export JWT_EXPIRATION="3600"
export SERVER_PORT="8080"
```

`JWT_SECRET` possui um valor padrao inseguro apenas para desenvolvimento. Em producao, defina obrigatoriamente um segredo forte via variavel de ambiente.

Tambem podem ser ajustados:

```bash
export RATE_LIMIT_SIGNUP_CAPACITY="5"
export RATE_LIMIT_SIGNUP_WINDOW="60s"
export RATE_LIMIT_LOGIN_CAPACITY="10"
export RATE_LIMIT_LOGIN_WINDOW="60s"
export RATE_LIMIT_API_CAPACITY="100"
export RATE_LIMIT_API_WINDOW="60s"
```

## Execucao

```bash
./mvnw spring-boot:run
```

Testes e build:

```bash
./mvnw clean test
./mvnw clean package
java -jar target/*.jar
```

## Swagger

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

O Swagger UI esta configurado com o esquema `Bearer Authentication` para informar o JWT pelo botao de autorizacao.

## Rotas

Publicas:

- `GET /health`
- `POST /api/signup`
- `POST /api/login`
- `GET /actuator/health`
- `GET /actuator/info`
- Swagger/OpenAPI

Protegidas por Bearer JWT:

- `GET /api/tasks`
- `GET /api/tasks/{id}`
- `POST /api/tasks`
- `PUT /api/tasks/{id}`
- `PATCH /api/tasks/{id}/status`
- `DELETE /api/tasks/{id}`

`/health` e um controller proprio com resposta amigavel. O Actuator permanece disponivel em `/actuator/health` e `/actuator/info`, e somente esses endpoints sao expostos.

## Exemplos com curl

Health:

```bash
curl http://localhost:8080/health
```

Cadastro:

```bash
curl -X POST http://localhost:8080/api/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"Leonardo Correa","email":"leonardo@example.com","password":"SenhaSegura123"}'
```

Login:

```bash
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"email":"leonardo@example.com","password":"SenhaSegura123"}'
```

Use o token retornado:

```bash
TOKEN="cole-o-token-aqui"
```

Criar tarefa:

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Preparar demo","description":"Finalizar a API","status":"PENDING","dueDate":"2026-08-01"}'
```

Listar tarefas:

```bash
curl "http://localhost:8080/api/tasks?page=0&size=20&sort=createdAt,desc" \
  -H "Authorization: Bearer $TOKEN"
```

Filtrar por status:

```bash
curl "http://localhost:8080/api/tasks?status=PENDING&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

Atualizar tarefa:

```bash
TASK_ID="uuid-da-tarefa"

curl -X PUT "http://localhost:8080/api/tasks/$TASK_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Preparar demo atualizada","description":"Revisar README","status":"IN_PROGRESS","dueDate":"2026-08-02"}'
```

Alterar status:

```bash
curl -X PATCH "http://localhost:8080/api/tasks/$TASK_ID/status" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"COMPLETED"}'
```

Excluir:

```bash
curl -X DELETE "http://localhost:8080/api/tasks/$TASK_ID" \
  -H "Authorization: Bearer $TOKEN"
```

## Paginacao

`GET /api/tasks` retorna um `Page<TaskResponse>` do Spring Data. Parametros suportados:

- `page`: pagina iniciando em 0
- `size`: tamanho da pagina
- `sort`: campo e direcao, por exemplo `createdAt,desc`
- `status`: filtro opcional por `PENDING`, `IN_PROGRESS` ou `COMPLETED`

## Rate limiting

O rate limit atual e local e em memoria:

- `POST /api/signup`: 5 requisicoes por minuto por IP
- `POST /api/login`: 10 requisicoes por minuto por IP
- demais rotas `/api/**`: 100 requisicoes por minuto por usuario autenticado ou IP

Quando o limite e excedido, a API retorna `429 Too Many Requests` com resposta padronizada e header `Retry-After`.

Essa abordagem funciona adequadamente para uma unica instancia. Em ambientes com multiplas instancias, o estado precisaria ser compartilhado. Redis, API Gateway, reverse proxy ou WAF sao alternativas recomendadas para rate limiting distribuido e limites adicionais.

## Seguranca

- Senhas armazenadas com BCrypt
- Autenticacao JWT stateless
- Sessao HTTP, form login e HTTP Basic desabilitados
- Isolamento horizontal de tarefas por usuario usando consultas com `taskId` e `userId`
- DTOs para entrada e saida, sem expor entidades JPA nos controllers
- Validacao com Bean Validation
- Tratamento global de erros sem stack trace ou detalhes internos
- Rate limiting em memoria

## Estrutura

```text
src/main/java/br/dev/jcorrea/taskmanagement
├── auth
├── config
├── exception
├── health
├── ratelimit
├── security
├── task
└── user
```

## Testes

Os testes unitarios cobrem `AuthService`, `JwtService` e `TaskService`. Os testes de integração com MockMvc validam health publico, protecao das tarefas, validacao, login, isolamento entre usuarios e rate limit. H2 e usado apenas no profile de teste para evitar dependencia de PostgreSQL externo durante a suíte.
