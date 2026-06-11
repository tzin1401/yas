# Đồ Án 02 — Development Roadmap Đã Sửa Lỗi

> File này thay thế cho `development-roadmap.md`.  
> Mục tiêu: triển khai theo thứ tự ít lỗi nhất, có evidence rõ ràng cho báo cáo, và tách bạch Basic CI/CD, ArgoCD, Service Mesh.

---

## 0. Chiến Lược Triển Khai

### 0.1 Chia thành 4 milestone

| Milestone | Mục tiêu | Kết quả cần có |
|---|---|---|
| M0 | Audit repo YAS | Biết chính xác service nào cần build/deploy |
| M1 | Hạ tầng K8s + NFS + Jenkins | Cluster chạy ổn, Jenkins build được |
| M2 | CI/CD + GitOps | branch/main/tag/developer_build chạy đúng |
| M3 | Istio Service Mesh | mTLS, AuthorizationPolicy, retry, Kiali evidence |

### 0.2 Scope production vs lab

```text
Lab/demo theo đề bài:
- NodePort + hosts file
- Tailscale để nối máy nhóm
- NFS server tự dựng trên master
- Jenkins compose có Docker CLI/kubectl/helm/kustomize

Production thực tế:
- Ingress/Gateway + DNS + TLS certificate thật
- RBAC least privilege cho CI/CD
- Không mount /var/run/docker.sock vào Jenkins lâu dài
- Không dùng NFS lab/no_root_squash/0777 cho dữ liệu stateful quan trọng
- Dùng managed service hoặc operator + StorageClass phù hợp cho Postgres/Kafka/Elasticsearch
- Secrets đi qua Kubernetes Secret đã mã hóa/SOPS/External Secrets, không commit secret thật
```

### 0.3 Luật tránh lỗi kiến trúc

```text
1. ArgoCD quản lý namespace nào thì Jenkins không kubectl set image trực tiếp namespace đó.
2. Không dùng cùng NodePort cho 2 service.
3. Base Kustomize không hardcode namespace.
4. NFS provisioner mount path nào thì /etc/exports phải export đúng path đó.
5. Khi mTLS STRICT, traffic vào app nên đi qua Istio IngressGateway.
6. AuthorizationPolicy phải allow cả gateway principal nếu service nhận request từ browser.
7. developer_build phải deploy cả dependency layer hoặc dùng overlay đã chứa dependency layer.
```

---

## M0 — Audit Repo YAS

### Mục tiêu

Xác định commit/tag YAS dùng để demo, danh sách service thật, đường dẫn build, port, dependency, Dockerfile, biến môi trường.

### Việc cần làm

```bash
git clone https://github.com/nashtech-garage/yas.git
cd yas
git rev-parse HEAD
git log -1 --oneline

find . -maxdepth 3 -iname "Dockerfile" -print
find . -maxdepth 3 -iname "pom.xml" -print
find . -maxdepth 3 -iname "package.json" -print
grep -R "server.port" -n . | head -50
grep -R "SPRING_DATASOURCE\|KAFKA\|ELASTIC\|KEYCLOAK" -n . | head -100
```

### Output cần tạo

`services.yaml`:

```yaml
services:
  - name: tax
    path: tax
    image: YOUR_DOCKERHUB/yas-tax-service
    port: 8080
    type: spring
    dependencies: [postgres]
  - name: product
    path: product
    image: YOUR_DOCKERHUB/yas-product-service
    port: 8080
    type: spring
    dependencies: [postgres, elasticsearch]
  - name: inventory
    path: inventory
    image: YOUR_DOCKERHUB/yas-inventory-service
    port: 8080
    type: spring
    dependencies: [postgres, kafka]
  - name: order
    path: order
    image: YOUR_DOCKERHUB/yas-order-service
    port: 8080
    type: spring
    dependencies: [postgres, kafka, tax, customer]
  - name: customer
    path: customer
    image: YOUR_DOCKERHUB/yas-customer-service
    port: 8080
    type: spring
    dependencies: [postgres]
  - name: cart
    path: cart
    image: YOUR_DOCKERHUB/yas-cart-service
    port: 8080
    type: spring
    dependencies: [postgres, redis]
  - name: search
    path: search
    image: YOUR_DOCKERHUB/yas-search-service
    port: 8080
    type: spring
    dependencies: [elasticsearch, kafka]
  - name: storefront-bff
    path: storefront-bff
    image: YOUR_DOCKERHUB/yas-storefront-bff
    port: 8080
    type: spring
    dependencies: [backend-services]
  - name: storefront
    path: storefront
    image: YOUR_DOCKERHUB/yas-storefront
    port: 3000
    type: nextjs
    dependencies: [backend-services]
```

Sau khi audit repo thật, bổ sung hoặc loại khỏi scope có chủ đích các module: `backoffice`, `backoffice-bff`, `delivery`, `identity`, `location`, `media`, `payment`, `payment-paypal`, `promotion`, `rating`, `recommendation`, `webhook`.

`project-version.md`:

```markdown
# YAS Version Pin

- Source repo: https://github.com/nashtech-garage/yas
- Commit/tag demo: <commit-sha-or-tag>
- Java version used by this commit: <java-version>
- Spring Boot version used by this commit: <spring-boot-version>
- Deviation from assignment, if any: <none-or-explanation>
```

### Evidence

```text
- Screenshot danh sách Dockerfile/service folder
- Bảng service/path/port/dependency trong báo cáo
- Commit/tag YAS được pin trong báo cáo
```

---

## M1 — Hạ Tầng K8s + NFS + Jenkins

## 1.1 Setup Tailscale

Chạy trên Master, Worker, Laptop dev:

```bash
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up
tailscale status
tailscale ip -4
```

Evidence:

```text
- Screenshot tailscale status
- Ghi rõ Master IP và Worker IP
```

---

## 1.2 Dựng K8s Cluster

Master:

```bash
MASTER_IP=<MASTER_TAILSCALE_IP>

sudo kubeadm init \
  --apiserver-advertise-address=${MASTER_IP} \
  --apiserver-cert-extra-sans=${MASTER_IP} \
  --pod-network-cidr=10.244.0.0/16 \
  --node-name=master

mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config

kubectl apply -f https://github.com/flannel-io/flannel/releases/latest/download/kube-flannel.yml
kubeadm token create --print-join-command
```

Worker:

```bash
sudo kubeadm join <MASTER_TAILSCALE_IP>:6443 \
  --token <token> \
  --discovery-token-ca-cert-hash sha256:<hash> \
  --node-name=worker
```

Verify:

```bash
kubectl get nodes -o wide
kubectl get pods -n kube-system
```

Evidence:

```text
- Screenshot kubectl get nodes -o wide
- Screenshot kube-system pods Running
```

---

## 1.3 Cài NFS đúng cách

Master:

```bash
sudo apt-get update
sudo apt-get install -y nfs-kernel-server

sudo mkdir -p /srv/nfs/yas
sudo chown nobody:nogroup /srv/nfs/yas
sudo chmod 0777 /srv/nfs/yas

cat <<EOF | sudo tee /etc/exports
/srv/nfs/yas <WORKER_TAILSCALE_IP>(rw,sync,no_subtree_check,no_root_squash)
EOF

sudo exportfs -rav
sudo systemctl enable --now nfs-kernel-server
sudo exportfs -v
```

Worker:

```bash
sudo apt-get install -y nfs-common
sudo mkdir -p /mnt/yas-nfs-test
sudo mount -t nfs <MASTER_TAILSCALE_IP>:/srv/nfs/yas /mnt/yas-nfs-test
df -h | grep yas
sudo umount /mnt/yas-nfs-test
```

K8s:

```bash
helm repo add nfs-subdir-external-provisioner \
  https://kubernetes-sigs.github.io/nfs-subdir-external-provisioner/
helm repo update

helm upgrade --install nfs-provisioner \
  nfs-subdir-external-provisioner/nfs-subdir-external-provisioner \
  --namespace kube-system \
  --set nfs.server=<MASTER_TAILSCALE_IP> \
  --set nfs.path=/srv/nfs/yas \
  --set storageClass.name=nfs-client \
  --set storageClass.defaultClass=true \
  --set storageClass.reclaimPolicy=Retain
```

Verify:

```bash
kubectl get storageclass
kubectl get pod -n kube-system | grep nfs
```

Evidence:

```text
- sudo exportfs -v
- kubectl get storageclass
- kubectl get pvc -n dev sau khi deploy
```

---

## 1.4 Cài Nginx Ingress cho Basic Mode

```bash
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update

helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace \
  --set controller.service.type=NodePort \
  --set controller.service.nodePorts.http=30080 \
  --set controller.service.nodePorts.https=30081
```

Verify:

```bash
kubectl get svc -n ingress-nginx
kubectl get pods -n ingress-nginx
```

Evidence:

```text
- Screenshot service có 80:30080/TCP
```

---

## 1.5 Cài Jenkins

Build custom Jenkins image theo file `solution-approach-fixed.md`, sau đó:

```bash
cd /opt/jenkins
docker compose up -d
docker exec -it jenkins docker --version
docker exec -it jenkins kubectl version --client
docker exec -it jenkins helm version
docker exec -it jenkins kustomize version
```

Cấu hình Jenkins:

```text
- Plugin: Docker Pipeline
- Plugin: GitHub Branch Source
- Plugin: Pipeline
- Plugin: Credentials
- Credentials: dockerhub-creds
- Credentials: github-creds
- Job type: Multibranch Pipeline
```

Evidence:

```text
- Screenshot Jenkins plugin/job
- Screenshot Jenkins có đủ docker/kubectl/helm/kustomize
```

---

## M2 — CI/CD + GitOps

## 2.1 Tạo config repo `yas-gitops`

Structure:

```text
yas-gitops/
├── argocd/
├── services.yaml
├── base/
│   ├── postgres/
│   ├── kafka/
│   ├── zookeeper/          # nếu Kafka deploy theo mode ZooKeeper
│   ├── redis/              # nếu service trong scope cần Redis
│   ├── elasticsearch/
│   ├── keycloak/
│   ├── tax/
│   ├── product/
│   ├── inventory/
│   ├── order/
│   ├── customer/
│   ├── cart/
│   ├── search/
│   ├── storefront-bff/
│   ├── storefront/
│   └── ingress-basic/
└── overlays/
    ├── dev/
    ├── staging/
    └── developer/
```

Checklist manifest:

```text
- Deployment
- Service ClusterIP
- ServiceAccount
- ConfigMap
- Secret template
- PVC cho stateful dependencies
- readinessProbe/livenessProbe
- resource requests/limits
- NetworkPolicy hoặc Istio AuthorizationPolicy ở phase mesh
```

Lưu ý:

```text
Không hardcode namespace trong base.
Namespace chỉ nằm trong overlays/*/kustomization.yaml.
```

---

## 2.2 Tạo overlay `dev`

`overlays/dev/kustomization.yaml`:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: dev

resources:
  - ../../base/postgres
  - ../../base/kafka
  - ../../base/zookeeper
  - ../../base/redis
  - ../../base/elasticsearch
  - ../../base/keycloak
  - ../../base/tax
  - ../../base/product
  - ../../base/inventory
  - ../../base/order
  - ../../base/customer
  - ../../base/cart
  - ../../base/search
  - ../../base/storefront-bff
  - ../../base/storefront
  - ../../base/ingress-basic

images:
  - name: YOUR_DOCKERHUB/yas-tax-service
    newTag: main
  - name: YOUR_DOCKERHUB/yas-product-service
    newTag: main
  - name: YOUR_DOCKERHUB/yas-inventory-service
    newTag: main
  - name: YOUR_DOCKERHUB/yas-order-service
    newTag: main
  - name: YOUR_DOCKERHUB/yas-customer-service
    newTag: main
  - name: YOUR_DOCKERHUB/yas-cart-service
    newTag: main
  - name: YOUR_DOCKERHUB/yas-search-service
    newTag: main
  - name: YOUR_DOCKERHUB/yas-storefront-bff
    newTag: main
  - name: YOUR_DOCKERHUB/yas-storefront
    newTag: main
```

Nếu service nào không nằm trong demo scope, không đưa vào overlay và ghi lý do ở báo cáo. Không để Jenkins build một service mà GitOps overlay không thể deploy/verify.

---

## 2.3 Tạo overlay `staging`

Khác `dev` ở:

```text
- namespace: staging
- host: yas.staging.local
- image tag: vX.Y.Z
- có thể dùng resource limit cao hơn
```

---

## 2.4 Tạo overlay `developer`

Dùng cho job `developer_build`.

```text
- namespace: developer hoặc dev-developer
- mỗi service có tag riêng theo parameter
- giữ dependency layer giống dev
```

Khuyến nghị dùng namespace riêng `developer`, không dùng namespace `dev`, để không phá môi trường dev auto.

---

## 2.5 Cài ArgoCD không trùng port

```bash
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -

kubectl apply -n argocd \
  -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

kubectl patch svc argocd-server -n argocd \
  -p '{"spec": {"type": "NodePort", "ports": [{"port": 443, "nodePort": 30444, "targetPort": 8080, "protocol": "TCP"}]}}'
```

Verify:

```bash
kubectl get svc -n argocd
kubectl get pods -n argocd
```

Evidence:

```text
- ArgoCD UI truy cập https://<MASTER_OR_WORKER_IP>:30444
- Screenshot app list
```

---

## 2.6 Tạo ArgoCD Applications

`argocd/app-dev.yaml`:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: yas-dev
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/<org>/yas-gitops
    targetRevision: HEAD
    path: overlays/dev
  destination:
    server: https://kubernetes.default.svc
    namespace: dev
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
      - ServerSideApply=true
```

Tạo tương tự:

```text
yas-staging → overlays/staging
yas-developer → overlays/developer
```

Apply:

```bash
kubectl apply -f argocd/app-dev.yaml
kubectl apply -f argocd/app-staging.yaml
kubectl apply -f argocd/app-developer.yaml
```

---

## 2.7 Jenkins CI

Acceptance criteria:

```text
- Push branch bất kỳ → DockerHub có image tag commit id
- Push main → DockerHub có tag main/latest, GitOps dev được bump
- Push tag vX.Y.Z → DockerHub có tag vX.Y.Z, GitOps staging được bump
```

Test:

```bash
git checkout -b dev_tax_service
git commit --allow-empty -m "test ci branch"
git push origin dev_tax_service

git checkout main
git commit --allow-empty -m "test ci main"
git push origin main

git tag v0.1.0
git push origin v0.1.0
```

Evidence:

```text
- Jenkins build log
- DockerHub image tags
- GitOps commit bump image
- ArgoCD app history/sync
```

---

## 2.8 `developer_build`

Acceptance criteria:

```text
- Form có parameter từng service
- Nhập dev_tax_service cho tax, các service còn lại main
- Jenkins resolve dev_tax_service → commit id
- Jenkins update overlays/developer/kustomization.yaml
- ArgoCD sync
- Truy cập app qua domain:port
```

Verify:

```bash
kubectl get pods -n developer
kubectl get deploy -n developer -o wide
kubectl describe deploy tax -n developer | grep Image
```

Evidence:

```text
- Screenshot Jenkins parameter form
- Screenshot resolved tags
- Screenshot GitOps commit
- Screenshot ArgoCD yas-developer Synced/Healthy
- Screenshot app truy cập được
- Screenshot DockerHub có đúng image tag commit id đã deploy
```

---

## 2.9 `teardown_dev`

GitOps mode:

```text
Option A:
- Jenkins set replicas = 0 trong overlay developer
- commit/push
- ArgoCD sync

Option B:
- Jenkins delete ArgoCD Application yas-developer
- delete namespace developer
```

Khuyến nghị Option A để giữ Git history.

Acceptance criteria:

```text
- Sau teardown, pod app trong namespace developer không còn hoặc replicas = 0
- PVC có thể giữ lại nếu muốn preserve data
```

Evidence:

```text
- Screenshot Jenkins teardown success
- Screenshot kubectl get pods -n developer
```

---

## M3 — Istio Service Mesh

## 3.1 Cài Istio

```bash
curl -L https://istio.io/downloadIstio | sh -
cd istio-*
export PATH=$PWD/bin:$PATH

istioctl x precheck
istioctl install --set profile=demo -y
kubectl apply -f samples/addons
```

Verify:

```bash
kubectl get pods -n istio-system
kubectl get svc -n istio-system
```

Expose Kiali:

```bash
kubectl patch svc kiali -n istio-system \
  -p '{"spec": {"type": "NodePort", "ports": [{"port": 20001, "nodePort": 30201, "targetPort": 20001, "protocol": "TCP"}]}}'
```

Evidence:

```text
- Kiali UI http://<NODE_IP>:30201
```

---

## 3.2 Chuyển entrypoint sang Istio Gateway

Không dùng Nginx để test mTLS STRICT.

Patch Istio IngressGateway NodePort:

```bash
kubectl patch svc istio-ingressgateway -n istio-system \
  -p '{"spec": {"type": "NodePort", "ports": [
    {"name":"http2","port":80,"nodePort":30090,"targetPort":8080,"protocol":"TCP"},
    {"name":"https","port":443,"nodePort":30490,"targetPort":8443,"protocol":"TCP"}
  ]}}'
```

Thêm hosts:

```bash
bash scripts/set-hosts.sh <WORKER_TAILSCALE_IP> yas.mesh.local
```

---

## 3.3 Bật sidecar injection

```bash
kubectl label namespace dev istio-injection=enabled --overwrite
kubectl rollout restart deployment -n dev
kubectl get pods -n dev
```

Verify:

```text
READY phải là 2/2 cho app pod.
```

---

## 3.4 Apply mTLS

```bash
kubectl apply -f istio/mtls.yaml
```

Verify:

```bash
istioctl authn tls-check <POD_NAME>.<NAMESPACE>
istioctl proxy-config secret <POD_NAME> -n dev | head
```

Evidence:

```text
- Screenshot tls-check
- Screenshot proxy-config secret có cert ACTIVE
```

---

## 3.5 Apply Gateway + VirtualService

```bash
kubectl apply -f istio/gateway.yaml
kubectl apply -f istio/virtualservices.yaml
```

Test:

```bash
curl -v -H "Host: yas.mesh.local" http://<WORKER_IP>:30090/
curl -v -H "Host: yas.mesh.local" http://<WORKER_IP>:30090/api/orders
```

Evidence:

```text
- Browser/curl truy cập qua Istio Gateway thành công
```

---

## 3.6 Apply AuthorizationPolicy

Policy cần có cả:

```text
- allow gateway → public services
- allow storefront → order/product/customer nếu thật sự cần
- allow order → tax/customer
- allow product → inventory/search
- block random pod → protected service
```

Apply:

```bash
kubectl apply -f istio/authz-policies.yaml
```

Test allowed:

```bash
kubectl exec -n dev deploy/order -c order -- \
  curl -sv http://tax:8080/api/taxes 2>&1 | grep -E "< HTTP|HTTP/"
```

Test denied:

```bash
kubectl run curl-test --image=curlimages/curl -n dev --rm -it -- \
  curl -sv http://tax:8080/api/taxes 2>&1 | grep -E "< HTTP|RBAC"
```

Expected:

```text
Allowed: 200 OK hoặc response hợp lệ của app
Denied: 403 Forbidden / RBAC access denied
```

Evidence:

```text
- Screenshot curl 200
- Screenshot curl 403
```

---

## 3.7 Test no-sidecar

```bash
kubectl run no-sidecar \
  --image=curlimages/curl \
  -n dev \
  --overrides='{
    "metadata": {
      "annotations": {
        "sidecar.istio.io/inject": "false"
      }
    },
    "spec": {
      "containers": [{
        "name": "c",
        "image": "curlimages/curl",
        "command": ["sleep", "999"]
      }]
    }
  }'

kubectl exec no-sidecar -n dev -- \
  curl -sv http://tax:8080/api/taxes
```

Expected:

```text
Connection fail, 403, hoặc mTLS-related failure.
Không nên kỳ vọng 200.
```

Cleanup:

```bash
kubectl delete pod no-sidecar -n dev
```

---

## 3.8 Test retry

Cách tốt hơn fault abort 100% là tạo endpoint test trả 500 thật hoặc canary lỗi. Nếu chưa sửa code được, vẫn có thể dùng Istio fault injection nhưng phải ghi rõ limitation.

Evidence nên gồm:

```text
- x-envoy-attempt-count
- istio-proxy access log
- upstream_rq_retry metric nếu lấy được
- Kiali traffic graph
```

Lệnh xem access log:

```bash
POD=$(kubectl get pod -n dev -l app=order -o jsonpath='{.items[0].metadata.name}')
kubectl logs -n dev $POD -c istio-proxy --tail=100
```

---

## 3.9 Kiali Topology

Generate traffic:

```bash
while true; do
  curl -s -H "Host: yas.mesh.local" http://<WORKER_IP>:30090/api/orders >/dev/null
  sleep 1
done
```

Trong Kiali:

```text
- Namespace: dev
- Graph type: Versioned app graph
- Display: Security, Traffic rate, Response time
```

Evidence:

```text
- Screenshot graph có lock icon/mTLS badge
- Screenshot edge traffic
```

---

## M4 — Báo Cáo Và Nộp Bài

## 4.1 Cấu trúc báo cáo đề xuất

```text
1. Giới thiệu bài toán
2. Kiến trúc tổng thể
3. Hạ tầng K8s + Tailscale
4. Persistent Storage bằng NFS
5. Jenkins CI
6. developer_build và teardown
7. ArgoCD GitOps
8. Service Mesh Istio
9. Test plan và evidence
10. Vấn đề gặp phải và cách sửa
11. Production reality check
12. Kết luận
```

Mục `Production reality check` phải nói rõ:

```text
- NodePort/hosts file là yêu cầu lab; production dùng DNS + Ingress/Gateway + TLS.
- Jenkins demo mount Docker socket/kubeconfig; production dùng agent/build isolation/RBAC tối thiểu.
- NFS lab không phải storage production cho Kafka/Postgres/Elasticsearch.
- Secrets demo không được commit thật; production dùng secret management.
```

## 4.2 Screenshot checklist cuối

| Nhóm | Screenshot |
|---|---|
| Tailscale | `tailscale status` |
| K8s | `kubectl get nodes -o wide` |
| NFS | `sudo exportfs -v`, `kubectl get storageclass`, PVC Bound |
| Jenkins | Dashboard jobs, build log, parameter form |
| DockerHub | image tags commit id/main/vX.Y.Z |
| GitOps | commit bump image tag |
| ArgoCD | App list, Synced/Healthy, resource tree |
| Basic Access | `yas.dev.local:30080` |
| Developer Build | selected branch → deployed image tag |
| Teardown | namespace/resource after teardown |
| Istio | pods 2/2, tls-check, Kiali graph |
| Authz | curl 200 + curl 403 |
| Retry | retry evidence |
| Production | mục production reality check trong báo cáo |

---

## 5. Timeline Gợi Ý

```text
Ngày 1:
- Audit repo YAS
- Chốt services.yaml
- Chốt port/dependency map

Ngày 2:
- Setup Tailscale + kubeadm cluster
- Verify nodes/pods

Ngày 3:
- Setup NFS + NFS provisioner
- Viết PVC/stateful manifests

Ngày 4:
- Setup Jenkins custom image
- CI build/push image commit id

Ngày 5:
- Tạo yas-gitops repo
- Viết base/overlays/dev/staging/developer

Ngày 6:
- Cài ArgoCD
- main → dev
- tag → staging

Ngày 7:
- developer_build GitOps
- teardown

Ngày 8:
- Cài Istio/Kiali
- Chuyển entrypoint sang Istio Gateway

Ngày 9:
- mTLS/Authz/Retry tests
- Chụp evidence

Ngày 10:
- Viết báo cáo docx
- Review checklist
```

---

## 6. Definition of Done

Project được xem là ổn khi chạy được các test sau:

```bash
# Cluster
kubectl get nodes -o wide

# CI image tag
docker pull YOUR_DOCKERHUB/yas-tax-service:<commit-id>

# ArgoCD
argocd app list

# Dev access
curl -H "Host: yas.dev.local" http://<WORKER_IP>:30080/

# Developer build image
kubectl describe deploy tax -n developer | grep Image

# Staging release
kubectl describe deploy tax -n staging | grep v0.1.0

# Istio mTLS
istioctl authn tls-check <pod>.dev

# Authz denied
kubectl run curl-test --image=curlimages/curl -n dev --rm -it -- \
  curl -sv http://tax:8080/api/taxes

# Kiali
# Graph hiển thị traffic + security badge
```
