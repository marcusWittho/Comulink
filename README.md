# Comulink

API de encurtamento de URLs construída com Spring Boot 4.1.1, Spring Web MVC e Spring Data MongoDB.

Ela recebe uma URL longa, gera um código curto aleatório de 7 caracteres e, ao acessar esse código, redireciona para a URL original.

## Tecnologias

- Java 25 (toolchain do Gradle)
- Spring Boot 4.1.1 (Spring Web MVC, Spring Data MongoDB)
- MongoDB 8
- Lombok
- Gradle (wrapper incluído)

## Pré-requisitos

- JDK 25
- Docker e Docker Compose (para subir o MongoDB localmente)

## Como executar

1. Suba o MongoDB e o Mongo Express:

   ```bash
   docker compose up -d
   ```

   O MongoDB fica em `localhost:27017` e o Mongo Express em `http://localhost:8081`.

2. Inicie a aplicação:

   ```bash
   ./gradlew bootRun
   ```

   A API sobe na porta padrão do Spring Boot, `8080`.

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
  "id": "66f1c2a9e4b0a1b2c3d4e5f6",
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
    ├── model/Url.java            # Entidade @Document("urls")
    └── repository/UrlRepository.java
```

## Observações

- O código curto é gerado com `SecureRandom` a partir do alfabeto base62 (`0-9`, `a-z`, `A-Z`).
- O campo `shortCode` possui índice único no MongoDB. Em caso de colisão, a inserção falha com erro de chave duplicada; não há nova tentativa automática.
