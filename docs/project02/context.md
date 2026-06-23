# Đồ án 2 — Hệ thống CD cho YAS: Context

> File này lưu context & quyết định đã chốt cho Đồ án 2, để đi theo repo (chia sẻ
> được với đồng đội, dùng được ở máy/đường dẫn khác). Đề bài gốc:
> `docs/project02/Project02_HKII_25_26.md`.

## Tổng quan

- **Môn học:** DevOps (HCMUS, HK2 năm 3). Đồ án 2 = xây hệ thống **CD** cho app
  microservices Java **YAS (Yet Another Shop)**.
- **Nhóm:** 4 SV. Báo cáo `.docx` đặt tên theo `<MSSV…>` tăng dần.
- **Branch làm việc:** `lab2/cd-platform`.

## Lab 1 (đã xong)

CI phân tán Master–Agent trên **AWS**: Jenkins Master trên EC2 (Docker Compose),
3 agent local (Agent-Tri, Agent-QVinh, Agent-TVinh) qua JNLP. Pipeline 8 stage:
Checkout, Monorepo Path Filter, Test+JaCoCo, Build, Gitleaks, SonarQube, Snyk.
Từng gặp lỗi OOM Snyk exit -13 do thiếu RAM.

## Kiến trúc Lab 2 (đã CHỐT)

- **Hạ tầng mới:** thêm 1 **GCP VM 32GB RAM** làm Jenkins Agent `GCP-Agent-32GB`
  (chạy CI nặng, giải quyết OOM).
- **Cluster: K3s single-node** trên GCP VM (gộp control-plane + worker).
  ✅ Đã xác nhận dùng **K3s** (KHÔNG dùng kubeadm — dù docs cũ viết theo kubeadm).
- **GitOps split-repo:** tạo **GitOps Repo** riêng chứa YAML manifests, tách khỏi
  Source Repo.
- **Luồng CD:** code đổi ở Source Repo → Jenkins Master (AWS) điều phối xuống
  GCP Agent chạy CI → build image → push **Docker Hub** (tag = commit-id) →
  stage cuối chạy script update tag image vào YAML trên GitOps Repo →
  **ArgoCD** (trong K3s) watch GitOps Repo → auto sync → K3s rolling update.
- **Service Mesh (nâng cao):** **Istio + Kiali** trên K3s — mTLS STRICT, retry khi
  service trả 500, AuthorizationPolicy (giới hạn service-to-service), Kiali topology.

## Yêu cầu đề (điểm)

- **Cơ bản (6đ):** image tag main/latest mặc định (KHÔNG cần Grafana/Prometheus);
  K8s cluster (K3s OK); CI build image tag commit-id push Docker Hub mỗi branch;
  job `developer_build` (chọn branch/service để deploy, trả domain:port NodePort,
  dev tự thêm /etc/hosts trỏ worker node); job xóa deploy; (mục 6, bỏ qua nếu làm
  nâng cao) job dev (auto deploy main→ns dev) + staging (tag vX.Y.Z → ns staging).
- **Nâng cao:** ArgoCD handle dev/staging (2đ) + Service Mesh Istio/Kiali (2đ).

## Hiện trạng repo

**Đã có (scaffold/docs):**
- `services.yaml` — catalog service deployable (loại `common-library`, `delivery`);
  template image `docker.io/${DOCKERHUB_USERNAME}/yas-${service}:${tag}`; chiến lược
  tag commit-sha / main+latest / vX.Y.Z.
- `k8s/charts/` — 27 Helm charts (service + BFF + UI + config) + platform deps
  trong `k8s/deploy/` (Postgres, Kafka, Keycloak, ES, Redis, observability).
- `deploy/gitops/` — skeleton base + overlays dev/staging/developer (đang RỖNG,
  `resources: []`) + 3 ArgoCD App `argocd/apps/yas-{dev,staging,developer}.yaml`
  (auto-sync + prune + self-heal, trỏ branch `lab2/cd-platform`).
- `Jenkinsfile` — CI Lab 1 (8 stage) + logic skip-CI cho commit docs/gitops/spec.
- Docs: `docs/project02/{jenkins-jobs,cluster-runbook,mesh-runbook,
  development-roadmap-fixed}.md`; spec/plan/tasks trong `specs/001-yas-lab2-cd/`.
- GitHub Actions hiện push image lên `ghcr.io:latest` (KHÁC yêu cầu: phải Docker Hub
  + tag commit-id).

**Còn thiếu (phần triển khai thực):**
- Jenkins CD stages: build+push Docker Hub theo tag commit-id; script update tag vào
  GitOps repo.
- Jenkins jobs: `developer_build`, `teardown_developer`, `deploy_dev`,
  `release_staging`, rollback, cluster smoke-check.
- Populate overlays (render charts + image override theo tag).
- Provision GCP VM + cài K3s + ArgoCD + ingress + storage.
- Triển khai Istio/Kiali + policies (mới có runbook).

## ⚠️ Lưu ý khi bắt đầu triển khai

Docs `docs/project02/cluster-runbook.md` & các docs liên quan hiện viết theo
**kubeadm** (commit `190eb0a2`). Đã chốt **K3s** → cần cập nhật lại các runbook/docs
này cho khớp.

## Roadmap milestone (development-roadmap-fixed.md)

- **M0** ✅ docs/spec foundation
- **M1** cluster (GCP VM + K8s + storage)
- **M2** ingress + ArgoCD + 3 app synced
- **M3** Jenkins CD jobs + Docker Hub tags
- **M4** deploy evidence (developer/dev/staging)
- **M5** Istio/Kiali mesh evidence
- **M6** báo cáo
