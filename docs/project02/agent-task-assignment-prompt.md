# Agent Task Assignment Prompt - YAS Lab 2 CD

## Context Bắt Buộc

Repo làm việc: `https://github.com/tzin1401/yas.git`  
Branch bắt buộc: `lab2/cd-platform`  
Thư mục tài liệu Lab 2: `docs/project02/`  
Service catalog: `services.yaml`  
GitOps source of truth: `deploy/gitops/**`

Lab 1 đã có Jenkins CI với changed-module detection, Gitleaks, test, JaCoCo, coverage gate, Maven build, SonarQube và Snyk. Lab 2 không thay thế Lab 1, chỉ mở rộng thêm CD.

Version source of truth: giữ Java 25 + Spring Boot 4.0.2 theo repo hiện tại. Không downgrade về Java 21/Spring Boot 3.2 trừ khi cả nhóm cập nhật lại `docs/project02/project-version.md`.

## Guardrail Cho Agent Và Thành Viên

- Không commit secret thật, token, kubeconfig thật, SSH key, Docker Hub token hoặc ArgoCD token.
- Không dùng `kubectl set image`, `kubectl apply` hoặc `kubectl delete` trực tiếp vào namespace do ArgoCD quản lý: `dev`, `staging`, `developer`.
- Jenkins chỉ được sửa file GitOps trong `deploy/gitops/**`, commit lên branch `lab2/cd-platform`, sau đó để ArgoCD sync.
- Staging không được deploy image tag `latest`.
- Tất cả service/image scope phải đọc từ `services.yaml`, không hardcode danh sách service trong Jenkinsfile hoặc script.
- Commit GitOps/docs/agent/spec-only phải kích hoạt skip full CI theo rule hiện có.
- Mọi thay đổi Jenkins/CD phải giữ nguyên các gate của Lab 1.

## Mục Tiêu Bàn Giao

Hoàn thành Lab 2 CD cho YAS theo 4 lớp:

1. Cluster platform: kubeadm 2 node qua Tailscale, storage, ingress, ArgoCD, Istio/Kiali.
2. Jenkins CD pipeline: build/push Docker Hub, developer preview, dev/staging release, rollback, smoke check.
3. GitOps/K8s manifests: base/overlays/apps cho `dev`, `staging`, `developer`.
4. Evidence/report: ảnh chụp, log, command output, production reality check.

## Phân Công Tổng Quan

| Người | Vai Trò Chính | Phạm Vi Sở Hữu | Output Cuối |
|---|---|---|---|
| Trí Xuân | CD + Cluster Owner | Cluster, Spec Kit, service catalog, ArgoCD/Istio platform, end-to-end demo | Cluster chạy được, ArgoCD Healthy, mesh evidence, demo developer_build |
| Vinh Nhỏ | Jenkins + Image Pipeline Owner | Jenkinsfile, Jenkins jobs, Docker Hub image pipeline, credentials binding | CI/CD jobs chạy được, image tag đúng, deploy/rollback/smoke job có log |
| Vinh Bự | GitOps + Security + Report Owner | GitOps manifests, K8s policy/security audit, report/evidence | Overlays/app YAML đúng, secret/RBAC audit, báo cáo hoàn chỉnh |

## Checkpoint 0 - Kickoff Và Đồng Bộ Nguồn Sự Thật

Owner: cả 3 người  
Thời điểm: trước khi code tiếp

### Việc Cần Làm

- Checkout branch `lab2/cd-platform`.
- Đọc các file:
  - `AGENTS.md`
  - `services.yaml`
  - `docs/project02/final-plan-lab2-cd.md`
  - `docs/project02/project-version.md`
  - `docs/project02/cluster-runbook.md`
  - `docs/project02/jenkins-jobs.md`
  - `docs/project02/mesh-runbook.md`
  - `specs/001-yas-lab2-cd/spec.md`
  - `specs/001-yas-lab2-cd/plan.md`
  - `specs/001-yas-lab2-cd/tasks.md`

### Expected Output

- Mỗi người xác nhận scope của mình không conflict với người khác.
- Không ai tạo branch mới khác `lab2/cd-platform` cho phần Lab 2 chính.
- Nếu phát hiện service nào thiếu Dockerfile/Helm chart, cập nhật lại `services.yaml` và ghi lý do.

### Evidence Cần Lưu

- `git status`
- `git branch --show-current`
- Link commit/PR đang làm việc

## Trí Xuân - CD + Cluster Owner

### TX-1: Xác Nhận Service Catalog Và Artifact Mapping

Dependencies: Checkpoint 0  
Files liên quan: `services.yaml`, `docs/project02/service-catalog.md` nếu có

#### Việc Cần Làm

- Kiểm tra từng service deployable trong `services.yaml` có đủ:
  - Maven module hoặc frontend package tương ứng.
  - Dockerfile.
  - Helm chart hoặc chart path hợp lệ.
- Xác nhận các service không deploy:
  - `common-library`: Maven dependency, không build image riêng.
  - `delivery`: chưa đủ Dockerfile/Helm chart trong repo hiện tại.
- Nếu repo thay đổi, cập nhật catalog trước khi Vinh Nhỏ viết Jenkins logic.

#### Expected Output

- `services.yaml` là nguồn sự thật duy nhất cho Jenkins/GitOps/docs.
- Danh sách service deployable khớp repo thật.
- Có ghi chú rõ service nào excluded và lý do.

#### Verification

- Chạy kiểm tra path Dockerfile/chart cho toàn bộ service deployable.
- Không có service deployable nào trỏ tới path không tồn tại.

### TX-2: Setup Cụm Kubeadm 2 Node Qua Tailscale

Dependencies: TX-1  
Tài liệu chuẩn: `docs/project02/cluster-runbook.md`

#### Việc Cần Làm

- Chuẩn bị master/control-plane:
  - Ubuntu 22.04 hoặc 24.04.
  - 2-4 CPU, 8 GB RAM, 50 GB disk.
  - Cài `containerd`, `kubeadm`, `kubelet`, `kubectl`, `helm`, `yq`, `git`, `tailscale`.
- Chuẩn bị worker:
  - Ubuntu 22.04 hoặc 24.04.
  - 4 CPU, 8-12 GB RAM, 40 GB disk.
  - Cài `containerd`, `kubeadm`, `kubelet`, `tailscale`.
- Kết nối master/worker/laptop bằng Tailscale.
- Ghi lại:
  - `MASTER_TAILSCALE_IP`
  - `WORKER_TAILSCALE_IP`
- Chạy `kubeadm init` với:
  - `--apiserver-advertise-address=$MASTER_TAILSCALE_IP`
  - `--apiserver-cert-extra-sans=$MASTER_TAILSCALE_IP`
  - `--pod-network-cidr=10.244.0.0/16`
- Cài Flannel.
- Join worker vào cluster.

#### Expected Output

- Cluster có 1 master và 1 worker ở trạng thái `Ready`.
- CNI chạy ổn trong `kube-system`.
- Laptop dev truy cập được Kubernetes API qua Tailscale.

#### Verification

```bash
tailscale status
kubectl get nodes -o wide
kubectl get pods -n kube-system
```

#### Evidence Cần Lưu

- Screenshot hoặc log `tailscale status`.
- Screenshot hoặc log `kubectl get nodes -o wide`.
- Screenshot hoặc log `kubectl get pods -n kube-system`.

### TX-3: Setup NFS Storage Và Ingress/ArgoCD

Dependencies: TX-2

#### Việc Cần Làm

- Trên master export `/srv/nfs/yas` qua NFS cho worker.
- Cài `nfs-subdir-external-provisioner`.
- Verify PVC test `Bound` trước khi deploy dependency.
- Cài Nginx Ingress Controller với NodePort:
  - HTTP `30080`
  - HTTPS `30081`
- Cài ArgoCD với UI NodePort `30444`.
- Tạo hoặc apply ArgoCD apps:
  - `yas-dev`
  - `yas-staging`
  - `yas-developer`
- Đảm bảo app trỏ về:
  - repo `tzin1401/yas`
  - branch `lab2/cd-platform`
  - path `deploy/gitops/overlays/<env>`

#### Expected Output

- StorageClass `nfs-client` tồn tại.
- PVC test `Bound`.
- ArgoCD UI truy cập được.
- ArgoCD app objects tồn tại.
- Không conflict NodePort.

#### Verification

```bash
kubectl get storageclass
kubectl get pvc -A
kubectl get svc -A
kubectl get pods -n argocd
argocd app list
```

#### Evidence Cần Lưu

- `kubectl get storageclass,pvc -A`
- `kubectl get svc -A` thể hiện NodePort.
- ArgoCD UI screenshot.
- `argocd app list` hoặc ArgoCD apps screenshot.

### TX-4: Setup Istio/Kiali Và Mesh Demo

Dependencies: basic CD dev/developer đã chạy ổn  
Tài liệu chuẩn: `docs/project02/mesh-runbook.md`

#### Việc Cần Làm

- Cài Istio.
- Cài Kiali với NodePort `30201`.
- Bật sidecar injection cho namespace demo.
- Bật mTLS STRICT.
- Tạo AuthorizationPolicy:
  - Allow ingress gateway gọi BFF/API.
  - Allow flow hợp lệ như `order -> tax`, `order -> customer`, `order -> payment`.
  - Deny pod không được phép gọi service.
- Tạo VirtualService retry cho service demo `tax`.
- Retry evidence dùng fault abort 50%, không dùng 100% abort làm bằng chứng duy nhất.

#### Expected Output

- Pod trong namespace mesh demo READY `2/2`.
- mTLS STRICT có hiệu lực.
- Có bằng chứng allow và deny.
- Có retry evidence.
- Kiali topology hiển thị service graph.

#### Verification

```bash
kubectl get pods -n <mesh-namespace>
istioctl authn tls-check
kubectl get peerauthentication,authorizationpolicy,virtualservice -A
```

#### Evidence Cần Lưu

- Pod READY `2/2`.
- `istioctl authn tls-check`.
- Curl allow log.
- Curl deny log.
- Retry log.
- Kiali topology screenshot.

### TX-5: End-to-End CD Demo Owner

Dependencies: Vinh Nhỏ hoàn tất `developer_build`; Vinh Bự hoàn tất GitOps overlay

#### Việc Cần Làm

- Chạy demo developer preview:
  - Một service dùng branch dev.
  - Các service còn lại dùng main/latest hoặc tag được chỉ định.
- Xác nhận ArgoCD sync.
- Truy cập app qua:
  - Nginx NodePort `30080`, hoặc
  - Istio Gateway NodePort `30090` khi mesh mode bật.

#### Expected Output

- `developer_build` deploy được một tổ hợp service branch riêng.
- App truy cập được qua browser/curl.
- ArgoCD `yas-developer` ở trạng thái `Synced/Healthy`.

#### Evidence Cần Lưu

- Jenkins build log của `developer_build`.
- Docker Hub image tag tương ứng.
- ArgoCD app `yas-developer` screenshot.
- Browser/curl output.

## Vinh Nhỏ - Jenkins + Image Pipeline Owner

### VN-1: Preserve Lab 1 CI Và Skip-CI Rule

Dependencies: Checkpoint 0

#### Việc Cần Làm

- Đọc `Jenkinsfile` hiện tại và `ci/detect-changed-modules.sh`.
- Đảm bảo các gate Lab 1 vẫn còn:
  - Gitleaks.
  - Test.
  - JaCoCo.
  - Coverage gate.
  - Maven build.
  - SonarQube.
  - Snyk.
- Verify skip-CI rule:
  - Nếu chỉ đổi `deploy/gitops/**`, `docs/**`, `.agents/**`, `.specify/**`, chỉ chạy validation nhẹ và Gitleaks.
  - Nếu đổi source code/module, vẫn chạy full CI.

#### Expected Output

- Lab 1 CI không bị yếu đi.
- GitOps/docs-only commit không chạy full Maven/image pipeline.
- Source-code commit vẫn chạy full CI.

#### Verification

```bash
bash -n ci/detect-changed-modules.sh
```

Kiểm tra log Jenkins cho 2 loại commit:

- docs/GitOps-only.
- code/module change.

### VN-2: Docker Hub Image Pipeline Cho `yas-ci-multibranch`

Dependencies: VN-1, TX-1

#### Việc Cần Làm

- Jenkins đọc `services.yaml`.
- Detect service thay đổi dựa trên module/path.
- Chỉ build/push image cho service deployable bị đổi.
- Dùng Docker Hub credential:
  - credential id `dockerhub-creds`
  - type username/password
  - password là Docker Hub access token.
- Image format:
  - `docker.io/$DOCKERHUB_USERNAME/yas-<service>:<tag>`
- Tag rule:
  - feature branch: commit SHA.
  - `main`: commit SHA, `main`, `latest`.
  - `vX.Y.Z`: commit SHA, `vX.Y.Z`.

#### Expected Output

- Docker Hub có image đúng service/tag.
- CI không build image cho service không đổi.
- Log Jenkins thể hiện service scope đọc từ `services.yaml`.

#### Verification

- Push test commit vào một module nhỏ.
- Kiểm tra Docker Hub tag.
- Kiểm tra Jenkins console log.

### VN-3: Job `developer_build`

Dependencies: VN-2, Vinh Bự có developer overlay khả dụng

#### Việc Cần Làm

- Tạo manual parameterized job `developer_build`.
- Parameters:
  - Branch cho từng service hoặc map service->branch.
  - `TARGET_ENV=developer`.
  - Optional `SERVICE_SCOPE`.
- Resolve từng branch sang commit SHA.
- Nếu Docker Hub chưa có image tag commit SHA, build/push image.
- Update `deploy/gitops/overlays/developer`.
- Commit GitOps change vào branch `lab2/cd-platform`.
- Commit message phải chứa marker skip CI, ví dụ `[skip ci]`, hoặc bảo đảm path classifier skip full CI hoạt động.
- Trigger `argocd app sync yas-developer` hoặc để auto-sync xử lý.
- Output URL app.

#### Expected Output

- Một service chạy branch dev riêng.
- Các service còn lại chạy main/latest hoặc tag được chỉ định.
- ArgoCD `yas-developer` Synced/Healthy.
- Không deploy trực tiếp bằng `kubectl set image`.

#### Verification

- Jenkins parameters screenshot.
- Jenkins console log resolve branch->SHA.
- GitOps commit diff.
- ArgoCD app screenshot.
- App URL curl/browser output.

### VN-4: Job `teardown_developer`

Dependencies: VN-3

#### Việc Cần Làm

- Tạo manual job `teardown_developer`.
- Parameters:
  - `TARGET_ENV=developer`.
  - Optional `PRUNE=true`.
- Không dùng `kubectl delete` trực tiếp vào namespace ArgoCD quản lý.
- Disable hoặc remove developer app/overlay bằng GitOps commit.
- Để ArgoCD prune resource.

#### Expected Output

- Developer env được teardown sạch.
- ArgoCD không còn resource developer hoặc app ở trạng thái đã disable theo thiết kế.
- Có log Jenkins và ArgoCD evidence.

#### Verification

```bash
argocd app get yas-developer
kubectl get all -n developer
```

### VN-5: Jobs `deploy_dev`, `release_staging`, `rollback_environment`, `cluster_smoke_check`

Dependencies: VN-2, Vinh Bự hoàn tất dev/staging overlays

#### Việc Cần Làm

- `deploy_dev`:
  - Trigger sau khi `main` pass CI hoặc manual replay.
  - Update `deploy/gitops/overlays/dev`.
  - Sync `yas-dev`.
- `release_staging`:
  - Trigger bởi Git tag `vX.Y.Z`.
  - Manual fallback `RELEASE_TAG=vX.Y.Z`.
  - Update `deploy/gitops/overlays/staging`.
  - Không dùng `latest`.
- `rollback_environment`:
  - Parameters `TARGET_ENV=dev|staging`.
  - Dùng `ROLLBACK_TAG` hoặc `GITOPS_COMMIT`.
  - Revert overlay và sync ArgoCD.
- `cluster_smoke_check`:
  - Read-only.
  - Chạy `kubectl get nodes`, `kubectl get pods -A`, `kubectl get ingress,svc -A`, `argocd app list`, curl health/frontend URL.

#### Expected Output

- Dev tự cập nhật khi main có image mới.
- Staging deploy đúng release tag `vX.Y.Z`.
- Rollback quay về tag/commit cũ.
- Smoke check tạo log dùng được trong báo cáo.

#### Evidence Cần Lưu

- Jenkins job config screenshot.
- Jenkins logs cho mỗi job.
- Docker Hub tag screenshot.
- GitOps commit link.
- ArgoCD app status screenshot.

## Vinh Bự - GitOps + Security + Report Owner

### VB-1: Hoàn Thiện GitOps Base Và Overlays

Dependencies: TX-1

#### Việc Cần Làm

- Hoàn thiện `deploy/gitops/base`.
- Hoàn thiện overlays:
  - `deploy/gitops/overlays/dev`
  - `deploy/gitops/overlays/staging`
  - `deploy/gitops/overlays/developer`
- Không đặt `namespace` cố định trong base nếu overlay cần namespace riêng.
- Base chỉ chứa resource dùng chung.
- Overlay chịu trách nhiệm namespace, image tag, ingress/gateway config.
- Developer overlay phải có dependency layer đầy đủ:
  - PostgreSQL.
  - Redis.
  - Keycloak.
  - Kafka.
  - Zookeeper nếu Kafka chart cần.
  - Elasticsearch.
  - `yas-configuration` hoặc config service tương ứng.

#### Expected Output

- `kustomize build deploy/gitops/overlays/dev` render được.
- `kustomize build deploy/gitops/overlays/staging` render được.
- `kustomize build deploy/gitops/overlays/developer` render được.
- Namespace mapping đúng cho từng environment.

#### Verification

```bash
kustomize build deploy/gitops/overlays/dev
kustomize build deploy/gitops/overlays/staging
kustomize build deploy/gitops/overlays/developer
```

### VB-2: ArgoCD App YAML Và Sync Policy

Dependencies: VB-1

#### Việc Cần Làm

- Kiểm tra app YAML trong `deploy/gitops/argocd/apps/`.
- Mỗi app phải có:
  - `repoURL`.
  - `targetRevision: lab2/cd-platform`.
  - `path: deploy/gitops/overlays/<env>`.
  - `destination.namespace`.
  - `syncPolicy.automated.prune`.
  - `syncPolicy.automated.selfHeal`.
  - `CreateNamespace=true`.
- Nếu dùng private SSH repo URL, đảm bảo ArgoCD có deploy key nhưng không commit key.

#### Expected Output

- `yas-dev`, `yas-staging`, `yas-developer` sync được.
- App path và namespace không lẫn nhau.
- ArgoCD là controller duy nhất quản lý deploy state.

#### Verification

```bash
kubectl apply --dry-run=client -f deploy/gitops/argocd/apps/yas-dev.yaml
kubectl apply --dry-run=client -f deploy/gitops/argocd/apps/yas-staging.yaml
kubectl apply --dry-run=client -f deploy/gitops/argocd/apps/yas-developer.yaml
```

### VB-3: Security And Production Reality Check

Dependencies: VB-1, VN-2, TX-3

#### Việc Cần Làm

- Audit secret handling:
  - Không có secret thật trong repo.
  - Jenkins credentials dùng đúng type.
  - K8s secret dùng placeholder hoặc external injection.
- Audit RBAC:
  - Jenkins kubeconfig nếu có chỉ read-only cho smoke check.
  - ArgoCD có quyền sync namespace cần thiết.
  - Không cấp cluster-admin cho pipeline nếu không cần.
- Audit production reality:
  - NFS là lab-only, production dùng CSI/managed storage.
  - NodePort là lab/demo, production dùng LoadBalancer/Ingress controller chuẩn.
  - Tailscale là lab network, production dùng VPC/VPN/private network.
  - Docker Hub public image phù hợp lab, production cần registry governance/scanning/signing.
  - ArgoCD GitOps phù hợp production hơn direct deploy.

#### Expected Output

- Có section security + production reality check trong report.
- Không có secret leak khi chạy Gitleaks.
- Có danh sách điểm lab-only vs production-grade.

#### Verification

```bash
gitleaks detect --source=. --no-git --config=gitleaks.toml --exit-code=1 --redact
```

### VB-4: Evidence Và Report Cuối

Dependencies: TX-5, VN-5, VB-3

#### Việc Cần Làm

- Tổng hợp evidence theo thư mục hoặc bảng:
  - Jenkins CI/CD logs.
  - Docker Hub images/tags.
  - ArgoCD apps Synced/Healthy.
  - Kubernetes nodes/pods/services/ingress.
  - Developer preview.
  - Dev deploy.
  - Staging release.
  - Rollback.
  - Teardown.
  - Istio mTLS/AuthorizationPolicy/retry.
  - Kiali topology.
- Viết báo cáo `.docx` hoặc Markdown trước khi convert.
- Đối chiếu với đề bài `Project02_HKII_25_26.md`.

#### Expected Output

- Report có đủ mô tả kiến trúc, pipeline, CD flow, security, production reality check và evidence.
- Mỗi requirement của đề bài có bằng chứng tương ứng.
- Không đưa secret vào report screenshot/log.

#### Verification

- Checklist report pass 100%.
- Cả 3 người review chéo trước khi nộp.

## Checkpoint 1 - Foundation Ready

Điều kiện pass:

- `services.yaml` đã được xác nhận.
- Cluster kubeadm có master/worker Ready.
- NFS StorageClass/PVC test Bound.
- Nginx Ingress và ArgoCD truy cập được.
- GitOps app YAML tồn tại và trỏ đúng branch/path.
- Jenkins giữ nguyên Lab 1 gate.

Expected output:

- Log/screenshot cluster.
- Log/screenshot ArgoCD.
- Jenkins CI baseline pass.
- Git commit link cho thay đổi foundation.

## Checkpoint 2 - Basic CD Ready

Điều kiện pass:

- `yas-ci-multibranch` build/push image Docker Hub theo commit SHA.
- `deploy_dev` cập nhật overlay dev.
- ArgoCD `yas-dev` Synced/Healthy.
- App dev truy cập được qua NodePort/Ingress.

Expected output:

- Docker Hub screenshot.
- Jenkins build log.
- GitOps commit diff.
- ArgoCD dev screenshot.
- App curl/browser evidence.

## Checkpoint 3 - Developer Preview Ready

Điều kiện pass:

- `developer_build` nhận branch/service parameters.
- Một service chạy branch riêng.
- Các service còn lại chạy main/latest hoặc tag chỉ định.
- `teardown_developer` xóa/disable developer env qua GitOps.

Expected output:

- Jenkins parameter screenshot.
- Branch->SHA resolve log.
- ArgoCD developer Synced/Healthy.
- App URL output.
- Teardown evidence.

## Checkpoint 4 - Staging Release And Rollback Ready

Điều kiện pass:

- Tag `vX.Y.Z` build/push image.
- `release_staging` deploy đúng tag release.
- Staging không dùng `latest`.
- `rollback_environment` quay về tag/commit cũ.

Expected output:

- Git tag evidence.
- Docker Hub `vX.Y.Z` tag screenshot.
- Staging ArgoCD screenshot.
- Rollback Jenkins log.
- Trước/sau image tag evidence.

## Checkpoint 5 - Mesh And Final Report Ready

Điều kiện pass:

- Istio sidecar injection hoạt động, pod READY `2/2`.
- mTLS STRICT bật.
- AuthorizationPolicy có allow/deny evidence.
- Retry evidence cho `tax` service.
- Kiali topology screenshot.
- Report cuối mapping đủ yêu cầu đề bài.

Expected output:

- `istioctl authn tls-check`.
- Curl allow/deny/retry logs.
- Kiali screenshot.
- Report final.
- Review checklist có chữ ký/xác nhận của cả 3 người.

## Handoff Format Cho Mỗi Pull Request Hoặc Commit Lớn

Mỗi người khi bàn giao phải ghi:

```markdown
## Handoff - <Tên người> - <Ngày>

### Scope đã làm
- ...

### Files thay đổi
- ...

### Commands đã chạy
- ...

### Evidence
- ...

### Known issues
- ...

### Next owner cần làm
- ...
```

## Definition Of Done Toàn Lab 2

- Branch `lab2/cd-platform` chứa toàn bộ docs/spec/agent/GitOps/Jenkins changes.
- Không có secret thật trong repo.
- Lab 1 CI gate vẫn tồn tại.
- Docker Hub có image đúng tag policy.
- Dev, staging, developer deploy qua ArgoCD.
- Jenkins không deploy trực tiếp vào namespace ArgoCD quản lý.
- Developer preview và teardown chạy được.
- Staging release và rollback chạy được.
- Istio/Kiali demo có evidence.
- Báo cáo cuối có production reality check và evidence đầy đủ.
