# Docker Usage

This image is published on Docker Hub as:

- `<dockerhub-namespace>/bolso-em-dia-api`

## Pull

```bash
docker pull <dockerhub-namespace>/bolso-em-dia-api:latest
```

## Run

```bash
docker run --rm \
  -p 8081:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/bolso_em_dia \
  -e DB_USERNAME=bolso_em_dia \
  -e DB_PASSWORD=bolso_em_dia \
  -e APP_ALLOWED_ORIGINS=http://localhost:4173 \
  -e APP_ADMIN_EMAIL=admin@bolso-em-dia.local \
  -e APP_ADMIN_PASSWORD=admin123456 \
  -e APP_JWT_SECRET=change-this-secret-change-this-secret \
  <dockerhub-namespace>/bolso-em-dia-api:latest
```

The container always listens on port `8080` internally. The published host port
is yours to choose.

Example with a custom published port:

```bash
docker run --rm \
  -p 18081:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/bolso_em_dia \
  -e DB_USERNAME=bolso_em_dia \
  -e DB_PASSWORD=bolso_em_dia \
  -e APP_ALLOWED_ORIGINS=http://localhost:14173 \
  -e APP_ADMIN_EMAIL=admin@bolso-em-dia.local \
  -e APP_ADMIN_PASSWORD=admin123456 \
  -e APP_JWT_SECRET=change-this-secret-change-this-secret \
  <dockerhub-namespace>/bolso-em-dia-api:latest
```

## Runtime Variables

Required runtime variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `APP_ALLOWED_ORIGINS`
- `APP_ADMIN_EMAIL`
- `APP_ADMIN_PASSWORD`
- `APP_JWT_SECRET`

## Health Check

The image publishes its own Docker health check against:

- `http://localhost:8080/actuator/health`

## Source Repository

- GitHub repository: [github.com/bolso-em-dia/app-api](https://github.com/bolso-em-dia/app-api)
