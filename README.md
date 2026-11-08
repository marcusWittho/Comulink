# Shortener

API de encurtamento de URLs construída com Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA e PostgreSQL.

Ela recebe uma URL longa, gera um código curto aleatório de 7 caracteres e, ao acessar esse código, redireciona para a URL original.

## Tecnologias

- Java 25 (toolchain do Gradle)
- Spring Boot 4.1.1 (Spring Web MVC, Spring Data JPA)
- PostgreSQL 17
- Liquibase (migrações do banco)
- Lombok
- Gradle (wrapper incluído)

## Pré-requisitos

- JDK 25
- Docker e Docker Compose (para subir o PostgreSQL localmente)

## Configuração

A aplicação lê a conexão com o banco das variáveis de ambiente abaixo, sem valores padrão:

| Variável            | Exemplo (desenvolvimento)                              |
|---------------------|--------------------------------------------------------|
| `DATABASE_URL`      | `jdbc:postgresql://localhost:5433/shortener`           |
| `DATABASE_USERNAME` | `shortener`                                            |
| `DATABASE_PASSWORD` | `shortener`                                            |

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
   ./gradlew bootRun
   ```

   A API sobe na porta padrão do Spring Boot, `8080`. As migrações do Liquibase são aplicadas automaticamente na inicialização.

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

Retorna `400 Bad Request` quando `originalUrl` é nulo ou vazio.

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

## Estrutura do projeto

```
src/main/java/com/wittho/shortener/
├── ShortenerApplication.java
├── api/controller/
│   ├── UrlController.java        # POST /api/urls e GET /api/urls/{shortCode}
│   └── RedirectController.java   # GET /{shortCode} (redirecionamento)
└── domain/
    ├── model/Url.java            # Entidade JPA @Entity (tabela "urls")
    └── repository/UrlRepository.java

src/main/resources/db/changelog/
├── db.changelog-master.yaml      # Changelog principal do Liquibase
└── changes/001-create-urls-table.sql
```

## Observações

- O código curto é gerado com `SecureRandom` a partir do alfabeto base62 (`0-9`, `a-z`, `A-Z`).
- A coluna `short_code` possui restrição `UNIQUE` no PostgreSQL. Em caso de colisão, a inserção falha com erro de violação de chave única; não há nova tentativa automática.
- O schema é gerenciado pelo Liquibase. O Hibernate apenas valida o mapeamento (`ddl-auto: validate`); alterações de tabela devem ser feitas com um novo changeset.
