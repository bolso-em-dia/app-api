# Docker Publishing

This repository publishes one Docker Hub image:

- `bolso-em-dia-api`

The GitHub Actions workflow is defined in `.github/workflows/docker.yml`.

## Published image name

The workflow publishes:

- `<dockerhub-namespace>/bolso-em-dia-api`

The namespace comes from:

1. repository variable `DOCKERHUB_NAMESPACE`, if present
2. otherwise `DOCKERHUB_USERNAME`

## Workflow triggers

The Docker workflow runs in two modes:

- only after the backend validation job in the same workflow succeeds

- automatically on pushed Git tags that match `v*`
- manually through `workflow_dispatch`

## Tagging model

Release tags must use semantic versioning in this format:

- `vMAJOR.MINOR.PATCH`

Examples:

- `v1.0.0`
- `v1.4.2`

For a release tag such as `v1.4.2`, the image receives:

- `latest`
- `1.4.2`
- `1.4`
- `1`
- `sha-<commit>`

For manual `workflow_dispatch`, the workflow publishes only the `sha-<commit>`
variant.

## Required GitHub secrets

The workflow requires these repository secrets:

- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`

`DOCKERHUB_TOKEN` should be a Docker Hub access token. The same token is used
for image push and repository description sync — no additional scopes needed.

## Recommended GitHub repository variables

Optional but recommended:

- `DOCKERHUB_NAMESPACE`

## Docker Hub description sync

The workflow automatically updates the Docker Hub repository description page
from `DOCKER.md` after a successful image publish. This keeps the public
documentation in sync with the repository-maintained source.

The sync step uses `peter-evans/dockerhub-description@v4` and fails the
workflow if the description update is rejected — out-of-sync descriptions are
treated as a publish failure.

## Using the published image from Docker Hub

Pull the published API image with:

```bash
docker pull <dockerhub-namespace>/bolso-em-dia-api:latest
```

Run it directly with:

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

The published container always listens on port `8080` internally. The host-side
published port is yours to choose.

The image publishes its own Docker health check against:

- `http://localhost:8080/actuator/health`

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

## Release tagging process

Use this process for an official Docker release:

1. make sure the target commit is already in the branch you want to release
2. make sure validation is green for that commit
3. create the release tag locally
4. push the tag to GitHub
5. wait for the Docker workflow to publish the image
6. verify the generated tags on Docker Hub

Example:

```bash
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

## Manual publish process

Use `workflow_dispatch` when you want to publish a commit-addressable image
without creating a formal release tag.

Expected result:

- `sha-<commit>` tags are published
- semver tags and `latest` are not published

## Multi-architecture output

The image is published for:

- `linux/amd64`
- `linux/arm64`
