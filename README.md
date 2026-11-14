# Comulink

API de encurtamento de URLs construída com Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA e PostgreSQL.

Ela recebe uma URL longa, gera um código curto aleatório de 7 caracteres e, ao acessar esse código, redireciona para a URL original.

## Tecnologias

- Java 25 (toolchain do Gradle)
- Spring Boot 4.1.1 (Spring Web MVC, Spring Data JPA)
- PostgreSQL 17
- Liquibase (migrações do banco)
- Bucket4j + Caffeine (rate limit por IP na criação de links)
- Lombok
- Testcontainers (PostgreSQL 17 nos testes de integração)
- Gradle (wrapper incluído)

## Pré-requisitos

- JDK 25
- Docker e Docker Compose (para subir o PostgreSQL localmente e para rodar os testes)

## Configuração

A aplicação lê a conexão com o banco das variáveis de ambiente abaixo, sem valores padrão:

| Variável            | Exemplo (desenvolvimento)                              |
|---------------------|--------------------------------------------------------|
| `DATABASE_URL`      | `jdbc:postgresql://localhost:5433/shortener`           |
| `DATABASE_USERNAME` | `shortener`                                            |
| `DATABASE_PASSWORD` | `shortener`                                            |

O limite de criação de links pode ser ajustado em `application.yaml`:

| Propriedade                                   | Padrão | Descrição                                   |
|-----------------------------------------------|--------|---------------------------------------------|
| `shortener.rate-limit.create-url.capacity`    | `10`   | Requisições permitidas por IP...            |
| `shortener.rate-limit.create-url.refill-period`| `1m`   | ...a cada período de reposição              |

## Como executar

1. Suba o PostgreSQL:

   ```bash
   docker compose -f docker-compose.dev.yml up -d
   ```

   O PostgreSQL fica em `localhost:5433` (porta do host; dentro do container é `5432`).

2. Defina as variáveis de conexão e inicie a aplicação:

   ```bash
   export DATABASE_URL='jdbc:postgresql://localhost:5433/shortener'
   export DATABASE_USERNAME='shortener'
   export DATABASE_PASSWORD='shortener'
   SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
   ```

   O perfil `prod` (`application-prod.yaml`) é o que lê as variáveis `DATABASE_*`. A imagem Docker já o ativa por padrão.

   A API sobe na porta padrão do Spring Boot, `8080`. As migrações do Liquibase são aplicadas automaticamente na inicialização.

### Executar com Docker

```bash
docker build -t shortener .

docker run --rm -p 8080:8080 \
  -e DATABASE_URL='jdbc:postgresql://host.docker.internal:5433/shortener' \
  -e DATABASE_USERNAME='shortener' \
  -e DATABASE_PASSWORD='shortener' \
  shortener
```

A imagem roda com usuário não-root. Para ajustar a memória da JVM, sobrescreva `JAVA_OPTS` (padrão: `-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError`). No Linux, `host.docker.internal` pode não existir; use o IP da máquina ou uma rede Docker compartilhada com o PostgreSQL.

## Testes

```bash
./gradlew test
```

Os testes de integração sobem um PostgreSQL 17 descartável com Testcontainers. Não é preciso definir as variáveis `DATABASE_*`, mas o Docker precisa estar disponível. Para forçar a reexecução após trocar o ambiente, use `./gradlew cleanTest test`.

## API

Erros de validação e de regra de negócio seguem `application/problem+json` (RFC 9457, `ProblemDetail`). Os erros de domínio trazem também a propriedade `code`.

### Criar um link curto

```http
POST /api/urls
Content-Type: application/json

{
  "originalUrl": "https://www.exemplo.com/um/caminho/bem/longo"
}
```

Resposta `201 Created`:

```json
{
  "id": 1,
  "shortCode": "aB3dE9z",
  "originalUrl": "https://www.exemplo.com/um/caminho/bem/longo",
  "createdAt": "2026-11-03T12:00:00Z"
}
```

Respostas de erro:

| Status | Quando | `code` |
|--------|--------|--------|
| `400 Bad Request` | `originalUrl` ausente, em branco ou inválida. São aceitas apenas URLs absolutas `http` ou `https`, com no máximo 2048 caracteres. Espaços no início e no fim são removidos antes da validação. | `S001` |
| `429 Too Many Requests` | Limite de criação por IP excedido. Inclui o cabeçalho `Retry-After` (em segundos). | não |
| `500 Internal Server Error` | Não foi possível gerar um código único após 5 tentativas (`S002`), ou erro inesperado. | `S002` ou ausente |

Exemplo de erro `400`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "A URL original deve ser absoluta, com esquema http ou https.",
  "code": "S001"
}
```

### Consultar um link curto

```http
GET /api/urls/{shortCode}
```

Retorna `200 OK` com os dados do link ou `404 Not Found` se o código não existir.

### Acessar e redirecionar

```http
GET /{shortCode}
```

Retorna `302 Found` com o cabeçalho `Location` apontando para a URL original, ou `404 Not Found` se o código não existir.

### Saúde da aplicação

```http
GET /actuator/health
GET /actuator/health/liveness
GET /actuator/health/readiness
```

Retornam `200 OK` com `{"status":"UP"}` quando a aplicação está saudável. Os detalhes dos componentes não são expostos. Os demais endpoints do Actuator não estão disponíveis (`404`).

## Estrutura do projeto

```
src/main/java/com/wittho/shortener/
├── ShortenerApplication.java
├── api/
│   ├── controller/
│   │   ├── UrlController.java        # POST /api/urls e GET /api/urls/{shortCode}
│   │   └── RedirectController.java   # GET /{shortCode} (redirecionamento)
│   ├── exception/
│   │   └── GlobalExceptionHandler.java  # Mapeia erros para ProblemDetail
│   └── ratelimit/
│       └── CreateUrlRateLimitFilter.java  # Limite de POST /api/urls por IP
└── domain/
    ├── exception/                    # DomainException e códigos de erro (ErrorCode, UrlErrorCode)
    ├── model/Url.java                # Entidade JPA (tabela "urls")
    ├── repository/UrlRepository.java
    ├── service/UrlService.java       # Geração do código curto e retry em colisão
    └── validation/OriginalUrlValidator.java

src/main/resources/db/changelog/
├── db.changelog-master.yaml          # Changelog principal do Liquibase
└── changes/001-create-urls-table.sql

src/test/java/com/wittho/shortener/
├── api/                              # Testes de integração da API (MockMvc + PostgreSQL)
├── domain/                           # Testes de serviço, validação e repositório
└── support/PostgresTestcontainerConfig.java
```

## Observações

- O código curto é gerado com `SecureRandom` a partir do alfabeto base62 (`0-9`, `a-z`, `A-Z`).
- A coluna `short_code` possui restrição `UNIQUE` (`uk_urls_short_code`). Em caso de colisão, `UrlService` tenta outro código, até 5 vezes, antes de responder erro.
- O rate limit é mantido em memória, por instância da aplicação. Com mais de uma instância, cada uma aplica o próprio limite.
- O schema é gerenciado pelo Liquibase. O Hibernate apenas valida o mapeamento (`ddl-auto: validate`); alterações de tabela devem ser feitas com um novo changeset.
