# Jenkins Jobs - Lab 2 CD

## Credentials

- `dockerhub-creds`: username/password, where password is a Docker Hub access token.
- `github-gitops-ssh`: SSH private key for pushing GitOps commits to `tzin1401/yas`.
- `argocd-token`: secret text for `argocd app sync/get`.
- `kubeconfig-readonly`: secret file for read-only cluster smoke checks.
- Existing Lab 1 credentials: `sonarqube-token`, `snyk-token`.

Do not commit any credential material, kubeconfig content, Google Cloud service account key, or SSH private key.

## Jobs

### `yas-ci-multibranch`

- Trigger: PR, branch push, Git tag.
- Keeps Lab 1 gates.
- Builds/pushes Docker Hub images for changed deployable services.
- Tags:
  - feature branch: commit SHA
  - `main`: commit SHA, `main`, `latest`
  - `vX.Y.Z`: commit SHA, `vX.Y.Z`

### `developer_build`

- Manual parameters:
  - branch per service, default `main`
  - `TARGET_ENV=developer`
  - optional `SERVICE_SCOPE`
- Resolves each branch to commit SHA.
- Builds/pushes missing image tags.
- Updates `deploy/gitops/overlays/developer`.
- Commits to `lab2/cd-platform`.
- Syncs ArgoCD app `yas-developer`.
- Outputs app URL using the GCP VM external IP and the selected ingress mode, for example `http://yas.developer.local:30080`.

### `teardown_developer`

- Manual parameters:
  - `TARGET_ENV=developer`
  - optional `PRUNE=true`
- Disables or removes developer overlay through GitOps.
- Lets ArgoCD prune resources.
- Does not directly delete resources from ArgoCD-managed namespace.

### `deploy_dev`

- Triggered after `main` passes CI.
- Updates `deploy/gitops/overlays/dev`.
- Syncs `yas-dev`.

### `release_staging`

- Trigger: Git tag `vX.Y.Z`.
- Manual fallback parameter: `RELEASE_TAG=vX.Y.Z`.
- Updates `deploy/gitops/overlays/staging`.
- Syncs `yas-staging`.
- Must not deploy `latest`.

### `rollback_environment`

- Manual parameters:
  - `TARGET_ENV=dev|staging`
  - `ROLLBACK_TAG` or `GITOPS_COMMIT`
- Reverts overlay and syncs ArgoCD.

### `cluster_smoke_check`

- Manual read-only job against the GCP VM cluster.
- Runs:
  - `kubectl get nodes`
  - `kubectl get pods -A`
  - `kubectl get ingress,svc -A`
  - `kubectl get storageclass,pvc -A`
  - `argocd app list`
  - curl health/frontend URL through `GCP_VM_EXTERNAL_IP` and expected Host header

## Skip-CI Rule

If only `deploy/gitops/**`, `docs/**`, `.agents/**`, or `.specify/**` changed, skip full Maven/image work and run lightweight validation only.
