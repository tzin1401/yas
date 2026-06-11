# Đồ Án 02 — Architecture Fix Notes

> File bổ sung để nhóm dùng khi review, chia task, hoặc viết phần “vấn đề gặp phải và cách khắc phục” trong báo cáo.

---

## 1. Danh Sách Lỗi Đã Sửa

| # | Lỗi cũ | Rủi ro khi chạy thật | Cách sửa |
|---|---|---|---|
| 1 | Jenkins `developer_build` deploy trực tiếp vào namespace do ArgoCD quản lý | ArgoCD self-heal revert image tag | `developer_build` update GitOps repo, ArgoCD sync |
| 2 | ArgoCD NodePort `30443` trùng Nginx HTTPS `30443` | Service patch/install fail | Đổi ArgoCD sang `30444`, Nginx HTTPS `30081` |
| 3 | Nginx Ingress gọi vào namespace bật mTLS STRICT | Request bị reject vì Nginx không có sidecar | Mesh mode dùng Istio IngressGateway |
| 4 | AuthorizationPolicy chỉ allow service nội bộ, không allow ingress/gateway | Browser gọi API bị 403 | Allow principal của Istio Gateway |
| 5 | `developer_build` chỉ apply PVC, không deploy stateful dependencies | Backend crash do thiếu Postgres/Kafka/Keycloak/ES | Overlay phải chứa dependency layer đầy đủ |
| 6 | NFS export thư mục con nhưng provisioner mount thư mục cha | PVC không Bound hoặc mount fail | Export `/srv/nfs/yas` đúng với `nfs.path=/srv/nfs/yas` |
| 7 | Ghi disk NFS nằm ở Worker | Sai capacity planning | NFS server nằm Master nên Master cần disk lớn |
| 8 | Base manifest hardcode `namespace: dev` | Staging apply nhầm namespace | Bỏ namespace trong base, set ở overlay |
| 9 | Jenkins image thiếu Docker/kubectl/helm/kustomize | Pipeline fail giữa chừng | Custom Jenkins image |
| 10 | `env.BRANCH_NAME` dùng trong Pipeline thường | Branch logic null/sai | Dùng Multibranch Pipeline |
| 11 | Feature branch auto update dev | Dev bị deploy lung tung | Feature branch chỉ build image, deploy qua manual `developer_build` |
| 12 | Staging update tag nhưng image tag release chưa được push | ImagePullBackOff | Git tag phải build/push image `vX.Y.Z` trước khi update staging |
| 13 | `no-sidecar` pod vẫn bị inject | Test mTLS sai | Thêm annotation `sidecar.istio.io/inject: "false"` |
| 14 | Retry test bằng fault abort 100% thiếu thuyết phục | Khó chứng minh retry upstream thật | Ưu tiên service/canary trả 500 thật hoặc ghi rõ limitation |
| 15 | Hardcode 6 service mẫu | CI/CD không build/deploy đủ YAS thật | Audit repo, tạo `services.yaml`, để Jenkins/GitOps đọc cùng catalog |
| 16 | Không pin commit/tag YAS | Build toolchain lệch đề bài Java 21/Spring Boot 3.2 | Chọn commit/tag/fork cố định hoặc ghi rõ deviation nếu dùng main |
| 17 | Lab setup bị mô tả như production | Người đọc hiểu nhầm NodePort/NFS/Docker socket là chuẩn production | Tách rõ lab/demo và production-realistic notes |

---

## 2. Architecture Decision Records

### ADR-001 — Dùng GitOps làm source of truth

**Decision:** Khi làm full 10 điểm, mọi deploy `dev/staging/developer` đi qua GitOps repo.

**Reason:** ArgoCD chỉ hoạt động đúng khi Git là desired state. Jenkins deploy trực tiếp sẽ tạo drift.

**Consequence:** Jenkins phải có quyền push vào config repo.

---

### ADR-002 — Tách Basic Ingress và Mesh Ingress

**Decision:** Basic CI/CD dùng Nginx Ingress; Service Mesh dùng Istio IngressGateway.

**Reason:** Nginx Ingress không có sidecar có thể không gọi được service trong namespace bật `mTLS STRICT`.

**Consequence:** Báo cáo cần mô tả 2 giai đoạn demo khác nhau.

---

### ADR-003 — ArgoCD UI dùng NodePort 30444

**Decision:** ArgoCD dùng `30444`.

**Reason:** Tránh trùng với `30443` thường được dùng cho HTTPS ingress/gateway.

**Consequence:** URL ArgoCD là `https://<node-ip>:30444`.

---

### ADR-004 — NFS Server nằm trên Master

**Decision:** Master làm NFS server.

**Reason:** Plan ban đầu đã đặt NFS Server trên Master; đơn giản cho nhóm 2 node.

**Consequence:** Master cần disk lớn hơn Worker.

---

### ADR-005 — Base Kustomize không chứa namespace

**Decision:** Namespace chỉ set tại overlay.

**Reason:** Cùng base cần dùng cho dev/staging/developer.

**Consequence:** Review YAML phải check không còn `metadata.namespace: dev` trong base.

---

### ADR-006 — Dùng `services.yaml` làm service catalog

**Decision:** Jenkins, GitOps overlay và báo cáo đều đọc/đối chiếu từ một file `services.yaml`.

**Reason:** Repo YAS có nhiều module hơn ví dụ `tax/product/inventory/order/customer/storefront`; hardcode danh sách trong Jenkinsfile dễ thiếu service hoặc deploy sai tên folder.

**Consequence:** Trước khi demo phải chốt rõ service nào in scope, service nào excluded và lý do.

---

### ADR-007 — Lab setup không đại diện đầy đủ cho production

**Decision:** NodePort, hosts file, Tailscale, NFS trên master, Jenkins root + Docker socket chỉ dùng cho đồ án/lab.

**Reason:** Đề bài yêu cầu domain:port NodePort, nhưng production thực tế cần DNS/TLS/Ingress hoặc Gateway, storage/secret/RBAC/build isolation nghiêm ngặt hơn.

**Consequence:** Báo cáo phải có mục "Production reality check" để phân biệt yêu cầu môn học và cách vận hành thật.

---

## 3. Final Flow Nên Demo

### 3.1 Basic flow

```text
git push branch dev_tax_service
  → Jenkins CI build image tag commit id
  → DockerHub có yas-tax-service:<commit-id>

Manual developer_build
  → chọn tax_branch=dev_tax_service
  → Jenkins update overlays/developer
  → ArgoCD sync
  → browser/curl truy cập yas.dev.local:30080 hoặc namespace developer
```

### 3.2 Dev flow

```text
git push main
  → Jenkins build image
  → push main/latest
  → Jenkins commit overlays/dev
  → ArgoCD yas-dev sync
```

### 3.3 Staging flow

```text
git tag v0.1.0
git push origin v0.1.0
  → Jenkins build image v0.1.0
  → DockerHub có tag v0.1.0
  → Jenkins commit overlays/staging
  → ArgoCD yas-staging sync
```

### 3.4 Mesh flow

```text
Browser
  → Istio IngressGateway :30090
  → VirtualService
  → order
  → mTLS
  → tax/customer
```

---

## 4. Những Chỗ Cần Nhóm Chia Task

| Thành viên | Task |
|---|---|
| A | K8s cluster, Tailscale, NFS, Ingress |
| B | Jenkins custom image, CI Jenkinsfile, DockerHub |
| C | GitOps repo, Kustomize, ArgoCD apps |
| D | Istio, Kiali, mTLS, AuthorizationPolicy, retry evidence |

---

## 5. Review Checklist Trước Khi Demo

```text
[ ] Không còn NodePort trùng nhau
[ ] ArgoCD app Synced/Healthy
[ ] Jenkins không kubectl set image vào namespace do ArgoCD quản lý
[ ] Base YAML không hardcode namespace
[ ] `services.yaml` khớp repo YAS thật và Jenkins/GitOps cùng dùng catalog này
[ ] Commit/tag YAS được pin hoặc deviation version được ghi rõ
[ ] NFS export đúng /srv/nfs/yas
[ ] PVC Bound
[ ] DockerHub có tag commit id
[ ] DockerHub có tag main/latest
[ ] DockerHub có tag vX.Y.Z
[ ] developer_build update GitOps repo thành công
[ ] App truy cập được qua domain:port
[ ] Istio sidecar READY 2/2
[ ] mTLS STRICT verify được
[ ] AuthorizationPolicy có cả allow và deny evidence
[ ] Retry có evidence
[ ] Kiali graph có traffic và security badge
[ ] Báo cáo có production reality check cho NodePort, NFS, Jenkins Docker socket, secrets/RBAC
```
