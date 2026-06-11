# Implementation Plan: YAS Lab 2 CD

## Architecture Decisions

- Use the existing fork and branch `lab2/cd-platform`.
- Keep Java 25 and Spring Boot 4.0.2.
- Use Docker Hub for Lab 2 images.
- Use GitOps as the source of truth for `dev`, `staging`, and `developer`.
- Use kubeadm 2-node cluster over Tailscale for lab evidence.

## Implementation Order

1. Foundation docs: copy Lab 2 docs, add `project-version.md`, `AGENTS.md`, Spec Kit artifacts.
2. Service catalog: create and validate `services.yaml`.
3. GitOps skeleton: create base/overlays and ArgoCD app manifests.
4. Jenkins skip-CI: classify docs/GitOps/spec-only commits and skip full Maven/image work.
5. Jenkins CD: extend image build/push and parameterized jobs.
6. Cluster runbook: document kubeadm, NFS, ingress, ArgoCD, and evidence commands.
7. Mesh runbook: document Istio/Kiali policies and retry evidence.

## Jenkins Jobs

- `yas-ci-multibranch`: CI gates plus image build/push for changed deployable services.
- `developer_build`: branch parameters, resolve commit SHA, update developer overlay, sync ArgoCD.
- `teardown_developer`: remove developer overlay state and prune through ArgoCD.
- `deploy_dev`: update dev from `main`.
- `release_staging`: update staging from `vX.Y.Z`.
- `rollback_environment`: revert dev/staging overlay by tag or GitOps commit.
- `cluster_smoke_check`: read-only cluster and URL checks.

## GitOps Layout

```text
deploy/gitops/
  base/
  overlays/dev/
  overlays/staging/
  overlays/developer/
  argocd/apps/
```

## Risks

- Dependency layer missing in developer environment: mitigate by checking required platform services before success.
- GitOps commits causing CI loop: mitigate by path classification and lightweight validation only.
- Staging accidentally using mutable tag: mitigate by release job requiring `vX.Y.Z`.
- Secrets leak: mitigate by Jenkins credential IDs and staged diff checks.

## Verification

- Static catalog validation.
- Helm/Kustomize render.
- Jenkins dry-run or smoke run.
- ArgoCD sync/health.
- Kubernetes node/pod/PVC/ingress checks.
- Curl app and mesh policy tests.
