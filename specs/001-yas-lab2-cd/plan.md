# Implementation Plan: YAS Lab 2 CD

## Architecture Decisions

- Use the existing fork and branch `lab2/cd-platform`.
- Keep Java 25 and Spring Boot 4.0.2.
- Use Docker Hub for Lab 2 images.
- Use GitOps as the source of truth for `dev`, `staging`, and `developer`.
- Use one Google Cloud Compute Engine VM with 32 GB RAM.
- Use `k3s` single-node Kubernetes (node `gcp-ci-cd-agent`); originally planned as `kubeadm` (see ADR-003 update in `docs/project02/architecture-fix-notes.md`). k3s does not taint the control-plane node by default, so no taint-removal step is needed for YAS workloads to run on the same node.
- Use local-path dynamic storage for the lab. Document it as single-node and non-production.
- Do not use Tailscale. Admin UIs use SSH tunnel or GCP firewall allowlisting.

## Implementation Order

1. Foundation docs: update `AGENTS.md`, Spec Kit artifacts, agent context, and Lab 2 docs for the GCP VM target.
2. Service catalog: validate `services.yaml` remains the shared source for Jenkins/GitOps/docs.
3. GitOps skeleton: keep base/overlays and ArgoCD app manifests; render overlays before committing.
4. Jenkins skip-CI: keep docs/GitOps/spec-only commits on lightweight validation.
5. Jenkins CD: extend image build/push and parameterized jobs without direct namespace mutation.
6. Cluster runbook: document GCP VM provisioning, k3s single-node, local-path storage, ingress, ArgoCD, admin access, and evidence commands.
7. Mesh runbook: document Istio/Kiali policies and retry evidence for the single-node cluster.

## Jenkins Jobs

- `yas-ci-multibranch`: CI gates plus image build/push for changed deployable services.
- `developer_build`: branch parameters, resolve commit SHA, update developer overlay, sync ArgoCD.
- `teardown_developer`: remove developer overlay state and prune through ArgoCD.
- `deploy_dev`: update dev from `main`.
- `release_staging`: update staging from `vX.Y.Z`.
- `rollback_environment`: revert dev/staging overlay by tag or GitOps commit.
- `cluster_smoke_check`: read-only cluster and URL checks through `kubeconfig-readonly`.

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

- Single-node resource pressure: mitigate by using the 32 GB VM, small replica counts, and staged dependency rollout.
- Local-path storage is node-bound: document it as lab-only and keep backups out of scope.
- GitOps commits causing CI loop: mitigate by path classification and lightweight validation only.
- Staging accidentally using mutable tag: mitigate by release job requiring `vX.Y.Z`.
- Admin UI exposure: mitigate by SSH tunnel or admin-CIDR firewall rules only.
- Secrets leak: mitigate by Jenkins credential IDs, Kubernetes secret stores, Gitleaks, and staged diff checks.

## Verification

- Static catalog validation.
- Helm/Kustomize render.
- Jenkins dry-run or smoke run.
- GCP VM and firewall evidence.
- Kubernetes node/pod/PVC/ingress checks.
- ArgoCD sync/health.
- Curl app and mesh policy tests through the VM external IP and expected Host header.
