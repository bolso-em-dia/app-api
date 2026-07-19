# app-api

Spring Boot API for Bolso em Dia.

## Stack

- Java 21
- Spring Boot 3
- Spring Security
- JWT access token + refresh token in an `HttpOnly` cookie
- Spring Data JPA
- Flyway
- PostgreSQL
- Testcontainers for integration tests
- Maven

## Prerequisites

For manual local execution:

- Java 21
- Maven 3.9+
- PostgreSQL 16+
- Docker available whenever you want to run the Testcontainers integration tests

## Port and endpoints

When the API runs manually, the default port is:

- `http://localhost:8080`

Useful endpoints:

- health: `http://localhost:8080/actuator/health`
- in the root `docker compose` stack, the API is published at `http://localhost:8081`

## Environment variables

Current defaults from `application.yml`:

- `DB_URL=jdbc:postgresql://localhost:5432/bolso_em_dia`
- `DB_USERNAME=bolso_em_dia`
- `DB_PASSWORD=bolso_em_dia`
- `APP_JWT_SECRET=change-this-secret-change-this-secret`
- `APP_ACCESS_TOKEN_MINUTES=15`
- `APP_REFRESH_TOKEN_DAYS=7`
- `APP_REFRESH_COOKIE_NAME=bolso_em_dia_refresh_token`
- `APP_REFRESH_COOKIE_SECURE=true`
- `APP_ALLOWED_ORIGINS=http://localhost:4173,http://localhost:5173`
- `APP_ADMIN_NAME=Admin`
- `APP_ADMIN_EMAIL=admin@bolso-em-dia.local`
- `APP_ADMIN_PASSWORD=admin123456`

## Manual execution

### 1. Start a local PostgreSQL

Example with Docker:

```bash
docker run --rm \
  --name bolso-em-dia-postgres \
  -e POSTGRES_DB=bolso_em_dia \
  -e POSTGRES_USER=bolso_em_dia \
  -e POSTGRES_PASSWORD=bolso_em_dia \
  -p 5432:5432 \
  postgres:16-alpine
```

### 2. Start the API

With the current defaults, this is enough:

```bash
cd app-api
mvn spring-boot:run
```

If you want to make the environment explicit:

```bash
cd app-api
DB_URL=jdbc:postgresql://localhost:5432/bolso_em_dia \
DB_USERNAME=bolso_em_dia \
DB_PASSWORD=bolso_em_dia \
APP_ALLOWED_ORIGINS=http://localhost:4173,http://localhost:5173 \
APP_ADMIN_EMAIL=admin@bolso-em-dia.local \
APP_ADMIN_PASSWORD=admin123456 \
APP_JWT_SECRET=change-this-secret-change-this-secret \
mvn spring-boot:run
```

Open:

- API: `http://localhost:8080`
- health: `http://localhost:8080/actuator/health`


## CI

- push validation workflow: `.github/workflows/ci.yml`
- Docker publishing workflow: `.github/workflows/docker.yml`

## Docker releases

- workflow: `.github/workflows/docker.yml`
- release and tagging guide: `DOCKER.md`
- published image: `danielarrais/bolso-em-dia-api`
- pull example: `docker pull danielarrais/bolso-em-dia-api:latest`
- run example: `docker run --rm -p 8081:8080 -e DB_URL=jdbc:postgresql://host.docker.internal:5432/bolso_em_dia -e DB_USERNAME=bolso_em_dia -e DB_PASSWORD=bolso_em_dia -e APP_JWT_SECRET=change-this-secret-change-this-secret danielarrais/bolso-em-dia-api:latest`

## Build, tests, and quality

Run tests:

```bash
cd app-api
mvn test
```

Run the full verification pipeline:

```bash
cd app-api
mvn verify
```

Apply Spotless formatting:

```bash
cd app-api
mvn spotless:apply
```

## Running with Docker Compose

Start the integrated stack from the repository root:

```bash
docker compose up --build
```

In this mode:

- PostgreSQL starts with ephemeral storage
- the API runs internally on port `8080`
- the host publishes the API on port `8081`
- the frontend in the compose stack consumes the API through `http://localhost:8081`

### Example `postgres` + `app-api`

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: bolso_em_dia
      POSTGRES_USER: bolso_em_dia
      POSTGRES_PASSWORD: bolso_em_dia
    ports:
      - "5432:5432"
    tmpfs:
      - /var/lib/postgresql/data

  app-api:
    build:
      context: ./app-api
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/bolso_em_dia
      DB_USERNAME: bolso_em_dia
      DB_PASSWORD: bolso_em_dia
      APP_ALLOWED_ORIGINS: http://localhost:4173
      APP_ADMIN_EMAIL: admin@bolso-em-dia.local
      APP_ADMIN_PASSWORD: admin123456
      APP_JWT_SECRET: change-this-secret-change-this-secret
    ports:
      - "8081:8080"
    depends_on:
      - postgres
```

### Example full stack

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: bolso_em_dia
      POSTGRES_USER: bolso_em_dia
      POSTGRES_PASSWORD: bolso_em_dia
    ports:
      - "5432:5432"
    tmpfs:
      - /var/lib/postgresql/data

  app-api:
    build:
      context: ./app-api
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/bolso_em_dia
      DB_USERNAME: bolso_em_dia
      DB_PASSWORD: bolso_em_dia
      APP_ALLOWED_ORIGINS: http://localhost:4173
      APP_ADMIN_EMAIL: admin@bolso-em-dia.local
      APP_ADMIN_PASSWORD: admin123456
      APP_JWT_SECRET: change-this-secret-change-this-secret
    ports:
      - "8081:8080"
    depends_on:
      - postgres

  app-web:
    build:
      context: ./app-web
    environment:
      API_BASE_URL: http://localhost:8081
    ports:
      - "4173:80"
    depends_on:
      - app-api
```

## Initial bootstrap

At startup, the API guarantees:

- the initial admin user
- basic reference data when it does not exist yet

Default credentials:

- email: `admin@bolso-em-dia.local`
- password: `admin123456`

## Integration tests

Integration tests use Testcontainers.

That requires:

- a working Docker environment
- a Docker client/daemon version compatible with the Testcontainers version in this project

If Docker is not available, `mvn test` or `mvn verify` may fail specifically in
integration suites even if the application itself can run manually.
