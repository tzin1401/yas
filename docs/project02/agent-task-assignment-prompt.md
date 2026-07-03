# Agent Task Assignment Prompt - Lab 2 CD On GCP VM

## Context

Lab 1 đã có Jenkins CI với changed-module detection, Gitleaks, test, JaCoCo, coverage gate, Maven build, SonarQube và Snyk. Lab 2 không thay thế Lab 1; chỉ mở rộng CD.

Version source of truth: giữ Java 25 + Spring Boot 4.0.2 theo repo hiện tại. Không downgrade về Java 21/Spring Boot 3.2 trừ khi cả nhóm cập nhật lại `docs/project02/project-version.md`.

Runtime source of truth: một Google Cloud Compute Engine VM 32 GB RAM, Ubuntu 24.04 LTS, `k3s` single-node Kubernetes (node `gcp-ci-cd-agent`, `v1.35.5+k3s1`). Ban đầu plan là `kubeadm`, TX đã đổi sang `k3s` khi provision — xem cập nhật ADR-003 trong `architecture-fix-notes.md`. Không dùng Tailscale.

## Hard Rules

- Không commit secret thật, token, kubeconfig thật, SSH key, Docker Hub token, Snyk token, SonarQube token, ArgoCD token, hoặc Google Cloud service account key.
- Không dùng `kubectl set image`, `kubectl apply`, hoặc `kubectl delete` trực tiếp vào namespace do ArgoCD quản lý: `dev`, `staging`, `developer`.
- Jenkins chỉ được sửa file GitOps trong `deploy/gitops/**`, commit lên branch `lab2/cd-platform`, sau đó để ArgoCD sync.
- Staging không dùng tag mutable như `latest`, `main`, branch name.
- Tất cả service/image scope phải đọc từ `services.yaml`, không hardcode danh sách service trong Jenkinsfile/script.
- Admin UI Jenkins/ArgoCD/Kiali không public rộng; dùng SSH tunnel hoặc GCP firewall allowlist admin IP.

## Team Ownership

| Member | Role | Scope | Done When |
|---|---|---|---|
| Trí Xuân | CD + Cluster Owner | GCP VM, firewall, k3s, local-path, ingress, ArgoCD/Istio platform | VM/cluster chạy được, ArgoCD Healthy, mesh evidence, demo developer_build |
| Vinh Nhỏ | Jenkins + Image Pipeline Owner | Jenkinsfile, Jenkins jobs, Docker Hub image pipeline, credentials binding | CI/CD jobs chạy được, image tag đúng, deploy/rollback/smoke job có log |
| Vinh Bự | GitOps + Security + Report Owner | GitOps manifests, K8s policy/security audit, report/evidence | Overlays/app YAML đúng, secret/RBAC audit, báo cáo hoàn chỉnh |

## TX Tasks - Cluster Platform

### TX-1: Provision GCP VM

Acceptance:

- Ubuntu 24.04 LTS.
- 32 GB RAM.
- 4-8 vCPU recommended.
- 150 GB+ disk.
- External IP recorded.
- Firewall documented.

Evidence:

- GCP VM machine type screenshot/command.
- Firewall rule screenshot/command.
- `free -h`, `df -h`, `lsb_release -a`.

### TX-2: Setup k3s Single-Node (originally planned as kubeadm)

Follow `docs/project02/cluster-runbook.md`.

Acceptance:

- `kubectl get nodes -o wide` shows one Ready node.
- Control-plane taint removed.
- `kubectl get pods -A` is healthy enough to continue.
- local-path StorageClass is default.

Evidence:

- `kubectl get nodes -o wide`
- `kubectl describe node gcp-ci-cd-agent`
- `kubectl get storageclass,pvc -A`

### TX-3: Setup Ingress And ArgoCD

Acceptance:

- Nginx Ingress exposes `30080/30081`.
- ArgoCD exists in namespace `argocd`.
- ArgoCD UI is reachable only through SSH tunnel or admin-IP firewall.
- ArgoCD app objects exist for `yas-dev`, `yas-staging`, `yas-developer`.

Evidence:

- `kubectl get svc -A`
- `argocd app list`
- SSH tunnel command/screenshot for ArgoCD UI.

### TX-4: Setup Mesh

Acceptance:

- Istio and Kiali installed after basic CD works.
- Istio Gateway uses `30090/30490`.
- Kiali access is admin-restricted.
- mTLS, AuthorizationPolicy, retry, and Kiali evidence collected.

## VN Tasks - Jenkins + Image Pipeline

### VN-1: Keep Lab 1 Gates

Acceptance:

- Gitleaks, tests, JaCoCo, coverage gate, build, SonarQube, and Snyk remain active for code changes.
- Docs/GitOps/spec/agent-only changes run lightweight validation only.

### VN-2: Docker Hub Images

Acceptance:

- Feature branch: commit SHA tag.
- Main: commit SHA, `main`, `latest`.
- Release: commit SHA, `vX.Y.Z`.
- Image name format: `docker.io/$DOCKERHUB_USERNAME/yas-<service>:<tag>`.

### VN-3: CD Jobs

Acceptance:

- `developer_build` resolves branch to SHA, updates developer overlay, syncs ArgoCD.
- `teardown_developer` removes developer resources through GitOps/ArgoCD prune.
- `deploy_dev` updates dev from main.
- `release_staging` updates staging from release tag and rejects mutable tags.
- `rollback_environment` reverts overlay and syncs ArgoCD.
- `cluster_smoke_check` is read-only.

## VB Tasks - GitOps + Security + Report

### VB-1: GitOps Overlays

Acceptance:

- Base has no hardcoded namespace.
- Overlays set namespace.
- App manifests point to branch `lab2/cd-platform`.
- Docker Hub image overrides are used for final CD.

### VB-2: Security Audit

Acceptance:

- No real secrets in repo.
- Jenkins credentials referenced by ID only.
- K8s demo secrets are documented as lab placeholders or injected outside Git.
- Admin UI access is not public-open.
- GCP firewall evidence is included.

### VB-3: Evidence Pack

Required evidence:

- GCP VM and firewall.
- Kubernetes node/pod/storage.
- Jenkins CI/CD logs.
- Docker Hub tags.
- GitOps diffs.
- ArgoCD apps Synced/Healthy.
- App URL through GCP VM external IP and hosts file/Host header.
- Mesh mTLS/authorization/retry/Kiali.
- Production reality check.

## Final Demo Flow

1. Show GCP VM, firewall, and cluster evidence.
2. Show Jenkins Lab 1 CI gates still active.
3. Push feature branch and build image tag.
4. Run `developer_build`; show ArgoCD developer sync and app URL.
5. Push/merge main; show dev sync.
6. Push release tag; show staging sync with immutable tag.
7. Run rollback and teardown.
8. Show mesh evidence.
