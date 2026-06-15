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

- [ ] Provision one 32 GB Google Cloud VM and reserve or record its external IP.
- [ ] Configure GCP firewall: app/demo ports as needed; admin UI ports restricted to SSH tunnel or admin IP allowlist.
- [ ] Verify required tools on the VM/Jenkins host: `yq`, `helm`, `kustomize`, `kubectl`, `argocd`, `istioctl`.
- [ ] Execute kubeadm single-node cluster runbook on the VM.
- [ ] Remove the control-plane taint so workloads can schedule on the single node.
- [ ] Install local-path storage, ingress, ArgoCD, Istio, and Kiali.
- [ ] Label the selected mesh namespace, restart deployments, and verify pods become `READY 2/2`.
- [ ] Capture required evidence logs/screenshots.

## Checkpoint

- [x] No committed real secrets.
- [x] No active Tailscale dependency remains in Lab 2 runbooks/specs.
- [x] Docs/GitOps-only commits skip full Maven/image pipeline.
- [ ] ArgoCD apps sync from `lab2/cd-platform`.
- [ ] Developer deployment includes platform dependencies.
