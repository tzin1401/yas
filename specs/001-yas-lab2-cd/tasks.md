# Tasks: YAS Lab 2 CD

## Phase 1 - Foundation

- [x] Copy Lab 2 docs into `docs/project02/`.
- [x] Create `AGENTS.md` hard rules.
- [x] Create `docs/project02/project-version.md`.
- [x] Initialize Spec Kit and create SDD artifact placeholders.
- [x] Update SDD and agent context for GCP VM single-node Kubernetes.

## Phase 2 - Service Catalog

- [x] Create `services.yaml` with deployable services and exclusions.
- [ ] Add automated catalog validation script.
- [ ] Ensure Jenkins reads catalog instead of hardcoding service list.

## Phase 3 - GitOps Foundation

- [x] Create GitOps folder skeleton.
- [x] Create ArgoCD app templates for dev, staging, developer.
- [ ] Add environment overlays that render existing YAS Helm charts with Docker Hub image overrides.
- [ ] Validate base has no hardcoded namespace.

## Phase 4 - Jenkins CD

- [x] Add skip-CI path classification for docs/GitOps/spec-only commits.
- [ ] Add image build/push stages after Lab 1 gates.
- [ ] Add `developer_build` job implementation.
- [ ] Add `teardown_developer`, `deploy_dev`, `release_staging`, `rollback_environment`, and `cluster_smoke_check`.

## Phase 5 - GCP Cluster And Mesh Evidence

- [ ] Provision one 32 GB Google Cloud VM and reserve or record its external IP.
- [ ] Configure GCP firewall: app/demo ports as needed; admin UI ports restricted to SSH tunnel or admin IP allowlist.
- [ ] Execute kubeadm single-node cluster runbook on the VM.
- [ ] Remove the control-plane taint so workloads can schedule on the single node.
- [ ] Install local-path storage, ingress, ArgoCD, Istio, and Kiali.
- [ ] Capture required evidence logs/screenshots.

## Checkpoint

- [ ] No committed real secrets.
- [ ] No active Tailscale dependency remains in Lab 2 runbooks/specs.
- [ ] Docs/GitOps-only commits skip full Maven/image pipeline.
- [ ] ArgoCD apps sync from `lab2/cd-platform`.
- [ ] Developer deployment includes platform dependencies.
