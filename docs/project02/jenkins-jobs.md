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
- Validates `services.yaml` before CD work.
- Builds/pushes Docker Hub images for changed deployable services after Gitleaks, tests, coverage, build, SonarQube, and Snyk complete.
- Reads deployable services from `services.yaml`; `common-library` changes rebuild all deployable service images because it is a shared Maven dependency.
- Tags:
  - feature branch: commit SHA
  - `main`: commit SHA, `main`, `latest`
  - `vX.Y.Z`: commit SHA, `vX.Y.Z`
- GitOps updates:
  - `main` updates `deploy/gitops/overlays/dev` to the `main` image tag.
  - `vX.Y.Z` updates `deploy/gitops/overlays/staging` to the immutable release tag.
  - staging rejects non-`vX.Y.Z`, `main`, and `latest` tags.
- Jenkins does not run `kubectl set image` or direct `kubectl apply` for `dev`, `staging`, or `developer`.

### `developer_build`

- Manual parameters:
  - branch per service, default `main` in the full job configuration
  - `TARGET_ENV=developer`
  - optional `SERVICE_SCOPE`
- Resolves each branch to commit SHA.
- Builds/pushes missing image tags.
- Updates `deploy/gitops/overlays/developer`.
- Commits to `lab2/cd-platform`.
- Syncs ArgoCD app `yas-developer`.
- Outputs app URL using the GCP VM external IP and the selected ingress mode, for example `http://yas.developer.local:30080`.

Current Jenkinsfile fallback:

- Set `CD_ACTION=developer_build_stub`.
- Set `IMAGE_TAG=<already-built-tag>`, default `main`.
- Set `SERVICE_SCOPE=cart,product` or leave empty for all deployable catalog services.
- The stub validates catalog service names, updates `deploy/gitops/overlays/developer`, commits GitOps desired state, and lets ArgoCD own sync.
- It intentionally does not mutate Kubernetes directly. Branch-to-commit resolution remains part of the full per-service branch matrix job.

### `teardown_developer`

- Manual parameters:
  - `TARGET_ENV=developer`
  - optional `PRUNE=true`
- Disables or removes developer overlay through GitOps.
- Lets ArgoCD prune resources.
- Does not directly delete resources from ArgoCD-managed namespace.
- Current Jenkinsfile fallback (`CD_ACTION=teardown_developer`) is a documented no-mutation guard stage until the GitOps overlay owner completes the removable developer state.

### `deploy_dev`

- Triggered after `main` passes CI.
- Updates `deploy/gitops/overlays/dev`.
- Syncs `yas-dev`.
- Can be run manually with `CD_ACTION=deploy_dev` and optional `SERVICE_SCOPE`.

### `release_staging`

- Trigger: Git tag `vX.Y.Z`.
- Manual fallback parameter: `RELEASE_TAG=vX.Y.Z`.
- Updates `deploy/gitops/overlays/staging`.
- Syncs `yas-staging`.
- Must not deploy `latest`.
- Can be run manually with `CD_ACTION=release_staging`; the Jenkinsfile fails before GitOps changes unless `RELEASE_TAG` matches `vX.Y.Z`.

### `rollback_environment`

- Manual parameters:
  - `TARGET_ENV=dev|staging`
  - `ROLLBACK_TAG` or `GITOPS_COMMIT`
- Reverts overlay and syncs ArgoCD.
- Current Jenkinsfile support writes `ROLLBACK_TAG` through the same catalog-driven GitOps image update path. Commit-based revert remains a documented operator procedure until the full job is split out.

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
