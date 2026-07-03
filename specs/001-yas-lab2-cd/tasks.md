# Tasks: YAS Lab 2 CD

## Phase 1 - Foundation

- [x] Copy Lab 2 docs into `docs/project02/`.
- [x] Create `AGENTS.md` hard rules.
- [x] Create `docs/project02/project-version.md`.
- [x] Initialize Spec Kit and create SDD artifact placeholders.
- [x] Update SDD and agent context for GCP VM single-node Kubernetes.

## Phase 2 - Service Catalog

- [x] Create `services.yaml` with deployable services and exclusions.
- [x] Add automated catalog validation script.
- [x] Ensure Jenkins reads catalog instead of hardcoding service list.
- [x] Validate service exclusions for `common-library`, `delivery`, and `identity` in catalog/docs evidence.

## Phase 3 - GitOps Foundation

- [x] Create GitOps folder skeleton.
- [x] Create ArgoCD app templates for dev, staging, developer.
- [x] Add environment overlays that render existing YAS Helm charts with Docker Hub image overrides.
- [x] Validate base has no hardcoded namespace.
- [x] Add GitOps readiness gate: `dev`, `staging`, and `developer` overlays must render non-empty manifests before ArgoCD sync.
- [x] Add staging immutability validation so staging cannot render or deploy `latest` or `main`.

## Phase 4 - Jenkins CD

- [x] Add skip-CI path classification for docs/GitOps/spec-only commits.
- [x] Add image build/push stages after Lab 1 gates.
- [ ] Add `developer_build` job implementation.
- [x] Add `developer_build_stub` fallback path if full `developer_build` is blocked.
- [x] Add `deploy_dev`, `release_staging`, and tag-based `rollback_environment` GitOps update actions.
- [ ] Add real `teardown_developer` implementation that removes or disables developer desired state through GitOps.
- [ ] Add real `cluster_smoke_check` implementation using read-only cluster/ArgoCD credentials.
- [x] Enforce `release_staging` tag format `vX.Y.Z` before GitOps updates.

## Phase 5 - GCP Cluster And Mesh Evidence

- [x] Provision one 32 GB Google Cloud VM and reserve or record its external IP.
- [ ] Configure GCP firewall: app/demo ports as needed; admin UI ports restricted to SSH tunnel or admin IP allowlist. (Verify 2026-07-03: port 22 was unreachable until VN added a firewall rule for a team member's IP; recheck 30080/30444 allowlisting is scoped to admin IPs only, not `0.0.0.0/0`.)
- [ ] Verify required tools on the VM/Jenkins host: `yq`, `helm`, `kustomize`, `kubectl`, `argocd`, `istioctl`.
- [x] Execute single-node cluster runbook on the VM — implemented with `k3s` (`v1.35.5+k3s1`), not `kubeadm` as originally planned. See ADR-003 update in `docs/project02/architecture-fix-notes.md`.
- [x] Control-plane taint: not applicable — k3s does not taint the control-plane node by default; confirmed workloads scheduling via `kubectl get pods -A`.
- [x] Install local-path storage, ingress, ArgoCD, Istio, and Kiali — confirmed running (`kubectl get pods -A` shows `ingress-nginx`, `argocd`, `istio-system` namespaces healthy).
- [ ] Label the selected mesh namespace, restart deployments, and verify pods become `READY 2/2`. (Partially observed: `mesh-demo` namespace has `location` and `tax` pods at `2/2`; confirm with TX whether `mesh-demo` is the final scope or `dev` should also be mesh-labeled.)
- [ ] Capture required evidence logs/screenshots.

## Checkpoint

- [x] No committed real secrets.
- [x] No active Tailscale dependency remains in Lab 2 runbooks/specs.
- [x] Docs/GitOps-only commits skip full Maven/image pipeline.
- [ ] ArgoCD apps sync from `lab2/cd-platform`.
- [ ] Developer deployment includes platform dependencies.
