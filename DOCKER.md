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

`DOCKERHUB_TOKEN` should be a Docker Hub access token.

## Recommended GitHub repository variables

Optional but recommended:

- `DOCKERHUB_NAMESPACE`

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
