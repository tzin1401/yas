# Đồ Án 02 — Hướng Giải Quyết Đã Sửa Lỗi Kiến Trúc

> File này thay thế cho `solution-approach.md`.  
> Mục tiêu: giữ đúng yêu cầu 6 điểm cơ bản, đồng thời không tạo xung đột nếu nhóm làm thêm ArgoCD và Service Mesh.

---

## 0. Nguyên Tắc Chốt Kiến Trúc

### 0.0 Scope đã chốt

Plan này chốt theo **Mode B — Full 10 điểm**:

```text
Jenkins CI build/push image
Jenkins CD job update GitOps repo
ArgoCD quản lý dev/staging/developer
Istio xử lý mTLS, AuthorizationPolicy, retry, Kiali topology
```

Nếu nhóm chỉ làm 6 điểm cơ bản thì bỏ toàn bộ ArgoCD/Istio khỏi acceptance path và dùng Jenkins deploy trực tiếp vào namespace do Jenkins quản lý. Không trộn hai mode trong cùng một namespace.

### 0.0.1 Pin phiên bản YAS trước khi triển khai

Đề bài ghi Java 21 và Spring Boot 3.2. Repo `nashtech-garage/yas` trên `main` có thể thay đổi theo thời gian, nên nhóm phải chọn một trong hai cách:

```text
Option khuyến nghị:
- Fork/clone YAS tại commit/tag khớp Java 21 + Spring Boot 3.2.
- Ghi commit SHA vào báo cáo và Jenkinsfile checkout cố định commit/tag đó khi demo.

Option thay thế:
- Dùng repo main mới nhất.
- Cập nhật toàn bộ toolchain Jenkins agent, Dockerfile, JDK, Maven/Node theo version hiện tại.
- Ghi rõ deviation so với đề bài trong báo cáo.
```

### 0.1 Chọn một trong hai mode CD

| Mode | Dùng khi nào | Ai deploy? | Ghi chú |
|---|---|---|---|
| **Mode A — Basic-only** | Chỉ làm 6 điểm cơ bản | Jenkins dùng `kubectl/helm` deploy trực tiếp | Không bật ArgoCD quản lý namespace `dev/staging` |
| **Mode B — Full 10 điểm** | Làm ArgoCD + Istio | Jenkins chỉ build image + commit config repo, ArgoCD sync | **Khuyến nghị dùng mode này** |

**Quy tắc quan trọng:** nếu ArgoCD đã quản lý namespace `dev` hoặc `staging`, Jenkins **không được** `kubectl set image`, `kubectl apply` trực tiếp vào namespace đó. Nếu làm vậy, ArgoCD `selfHeal` có thể revert lại cấu hình trong Git, làm `developer_build` deploy xong nhưng bị kéo về tag cũ.

---

## 1. Kiến Trúc Tổng Quan Đã Sửa

```text
Developer
   │
   │ git push branch / tag
   ▼
GitHub App Repo: <org>/yas
   │
   │ webhook
   ▼
Jenkins Multibranch Pipeline
   ├── CI:
   │     ├── build từng service
   │     ├── tag image = commit id
   │     ├── nếu branch main: push thêm tag main/latest
   │     └── nếu Git tag vX.Y.Z: push thêm tag release vX.Y.Z
   │
   ├── developer_build:
   │     ├── nhận branch parameter từng service
   │     ├── resolve branch → commit id
   │     ├── update overlay developer/dev trong GitOps repo
   │     └── ArgoCD sync
   │
   └── teardown_dev:
         ├── Basic mode: Jenkins delete namespace/resource trực tiếp
         └── GitOps mode: Jenkins sửa GitOps repo để disable app hoặc xóa overlay env

GitHub Config Repo: <org>/yas-gitops
   ├── base/
   ├── overlays/dev/
   ├── overlays/staging/
   └── overlays/developer/

ArgoCD
   ├── watches config repo
   ├── sync dev
   ├── sync staging
   └── sync developer env

K8s Cluster qua Tailscale
   ├── master: kubeadm control-plane, Jenkins, ArgoCD, NFS server
   └── worker: app pods, ingress/gateway, NFS client
```

---

## 2. Port Map Không Bị Trùng

| Thành phần | Port NodePort | Ghi chú |
|---|---:|---|
| Jenkins UI | `8080` trên master | Truy cập `http://100.x.x.1:8080` |
| Nginx Ingress HTTP, basic mode | `30080` | Dùng khi chưa bật Istio STRICT |
| Nginx Ingress HTTPS, basic mode | `30081` | Tránh trùng `30443` |
| ArgoCD UI | `30444` | Tránh trùng với Ingress/Istio |
| Kiali UI | `30201` | Dùng cho Service Mesh evidence |
| Istio IngressGateway HTTP, mesh mode | `30090` hoặc thay thế `30080` | Không chạy cùng Nginx trên cùng port |
| Istio IngressGateway HTTPS, mesh mode | `30490` | Optional |

**Khuyến nghị demo:**

```text
Giai đoạn 1 — Basic CI/CD:
Browser → Nginx Ingress NodePort 30080 → services

Giai đoạn 2 — Service Mesh:
Browser → Istio IngressGateway NodePort 30090 → VirtualService → services
```

Không nên để Nginx Ingress gọi vào namespace `dev` khi đã bật `mTLS STRICT`, trừ khi Nginx cũng được inject sidecar và có AuthorizationPolicy phù hợp.

### 2.1 Ghi chú production

NodePort phù hợp đồ án vì đề bài yêu cầu cung cấp `domain name:port` để developer tự trỏ hosts file. Trong production thực tế nên dùng:

```text
- Cloud LoadBalancer hoặc Ingress/Gateway controller
- DNS thật thay vì hosts file thủ công
- TLS certificate thật, ví dụ cert-manager + ACME hoặc certificate nội bộ
- Không expose Jenkins/ArgoCD/Kiali bằng NodePort public nếu không có VPN/auth/RBAC phù hợp
```

---

## 3. Phân Bổ Máy

| Máy | Vai trò | Chạy gì | Tailscale IP ví dụ |
|---|---|---|---|
| Máy 1 | Master / Control Plane | kubeadm, kubectl, Jenkins, ArgoCD, NFS Server | `100.x.x.1` |
| Máy 2 | Worker | Workloads, NFS Client, Nginx/Istio Gateway | `100.x.x.2` |
| Laptop dev | Client | Browser, kubectl optional, set-hosts script | `100.x.x.N` |

| Máy | CPU | RAM | Disk |
|---|---:|---:|---:|
| Master | 2-4 CPU | 6-8 GB | **50 GB+ vì NFS data nằm ở Master** |
| Worker | 4 CPU | 8-12 GB | 30 GB+ cho images/container runtime |

---

## 4. K8s Cluster Với kubeadm Qua Tailscale

Các bước kubeadm trong plan cũ vẫn dùng được, nhưng cần giữ các điểm sau:

```bash
MASTER_TAILSCALE_IP=<IP_TAILSCALE_MASTER>

sudo kubeadm init \
  --apiserver-advertise-address=${MASTER_TAILSCALE_IP} \
  --apiserver-cert-extra-sans=${MASTER_TAILSCALE_IP} \
  --pod-network-cidr=10.244.0.0/16 \
  --node-name=master
```

Verify bắt buộc:

```bash
kubectl get nodes -o wide
kubectl get pods -n kube-system
kubectl cluster-info
```

Evidence cần chụp:

```text
- tailscale status có master + worker
- kubectl get nodes -o wide, INTERNAL-IP là Tailscale IP
- kube-system pods Running
```

---

## 5. Persistent Storage — NFS Đã Sửa

### 5.1 Lỗi cũ cần tránh

Plan cũ export từng thư mục con:

```text
/srv/nfs/yas/kafka
/srv/nfs/yas/elasticsearch
...
```

nhưng NFS provisioner lại mount path cha:

```text
/srv/nfs/yas
```

Nếu path cha không được export, `nfs-subdir-external-provisioner` có thể mount fail.

### 5.2 Cách sửa đúng

Trên Master:

```bash
sudo apt-get update
sudo apt-get install -y nfs-kernel-server

sudo mkdir -p /srv/nfs/yas
sudo chown nobody:nogroup /srv/nfs/yas
sudo chmod 0777 /srv/nfs/yas

# Thay 100.x.x.2 bằng Tailscale IP của Worker.
cat <<EOF | sudo tee /etc/exports
/srv/nfs/yas 100.x.x.2(rw,sync,no_subtree_check,no_root_squash)
EOF

sudo exportfs -rav
sudo systemctl enable --now nfs-kernel-server
sudo exportfs -v
```

Trên Worker:

```bash
sudo apt-get update
sudo apt-get install -y nfs-common

sudo mkdir -p /mnt/yas-nfs-test
sudo mount -t nfs 100.x.x.1:/srv/nfs/yas /mnt/yas-nfs-test
df -h | grep yas
sudo umount /mnt/yas-nfs-test
```

Cài provisioner:

```bash
helm repo add nfs-subdir-external-provisioner \
  https://kubernetes-sigs.github.io/nfs-subdir-external-provisioner/
helm repo update

helm upgrade --install nfs-provisioner \
  nfs-subdir-external-provisioner/nfs-subdir-external-provisioner \
  --namespace kube-system \
  --set nfs.server=100.x.x.1 \
  --set nfs.path=/srv/nfs/yas \
  --set storageClass.name=nfs-client \
  --set storageClass.defaultClass=true \
  --set storageClass.reclaimPolicy=Retain
```

Verify:

```bash
kubectl get storageclass
kubectl get pods -n kube-system | grep nfs
```

### 5.3 Ghi chú production

NFS server tự dựng trên master chỉ nên dùng cho lab/demo. Với production:

```text
- Không dùng chmod 0777 và no_root_squash cho dữ liệu nhạy cảm.
- Không đặt dữ liệu Kafka/Postgres/Elasticsearch production trên NFS lab.
- Ưu tiên managed database/Kafka/Elasticsearch, hoặc operator chính thức với StorageClass block storage.
- Phải có backup/restore, retention, capacity planning và test khôi phục.
```

---

## 6. Repo Structure Đề Xuất

### 6.1 App repo: `<org>/yas`

```text
yas/
├── Jenkinsfile
├── Jenkinsfile.developer_build
├── Jenkinsfile.teardown
├── scripts/
│   ├── resolve-branch-tag.sh
│   └── set-hosts.sh
├── docker/
│   └── jenkins-agent/
│       └── Dockerfile
└── <service-directories>
```

### 6.2 Config repo: `<org>/yas-gitops`

```text
yas-gitops/
├── argocd/
│   ├── app-dev.yaml
│   ├── app-staging.yaml
│   └── app-developer.yaml
├── base/
│   ├── namespaces/
│   ├── configmaps/
│   ├── secrets-template/
│   ├── postgres/
│   ├── kafka/
│   ├── zookeeper/              # nếu Kafka mode đang dùng ZooKeeper
│   ├── redis/                  # nếu module YAS đang dùng Redis/session/cache
│   ├── elasticsearch/
│   ├── keycloak/
│   ├── backoffice-bff/
│   ├── cart/
│   ├── customer/
│   ├── delivery/
│   ├── identity/
│   ├── inventory/
│   ├── location/
│   ├── media/
│   ├── order/
│   ├── payment/
│   ├── payment-paypal/
│   ├── product/
│   ├── promotion/
│   ├── rating/
│   ├── recommendation/
│   ├── search/
│   ├── storefront-bff/
│   ├── storefront/
│   ├── tax/
│   ├── webhook/
│   └── ingress-basic/
├── overlays/
│   ├── dev/
│   ├── staging/
│   └── developer/
└── istio/
    ├── gateway.yaml
    ├── virtualservices.yaml
    ├── mtls.yaml
    ├── destination-rules.yaml
    ├── authz-policies.yaml
    └── retry-policy.yaml
```

**Quy tắc Kustomize:** file trong `base/` không hardcode namespace. Namespace chỉ set trong `overlays/*/kustomization.yaml`.

Ví dụ đúng:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: tax
spec:
  selector:
    matchLabels:
      app: tax
  template:
    metadata:
      labels:
        app: tax
    spec:
      serviceAccountName: tax
      containers:
        - name: tax
          image: YOUR_DOCKERHUB/yas-tax-service:main
          ports:
            - containerPort: 8080
```

Ví dụ sai cần tránh:

```yaml
metadata:
  name: tax
  namespace: dev
```

---

## 7. Dependency Layer Phải Deploy Trước App Services

`developer_build` không chỉ apply PVC. PVC chỉ tạo volume, không tạo Kafka/PostgreSQL/Keycloak/Elasticsearch pod. Thứ tự deploy chuẩn:

```text
1. Namespace
2. ServiceAccounts
3. Secrets + ConfigMaps
4. PVC
5. PostgreSQL
6. Kafka
7. Elasticsearch
8. Keycloak
9. Backend services
10. Frontend/storefront
11. Ingress hoặc Istio Gateway
```

Ví dụ verify rollout:

```bash
kubectl rollout status deploy/postgres -n dev --timeout=180s
kubectl rollout status deploy/kafka -n dev --timeout=180s
kubectl rollout status deploy/elasticsearch -n dev --timeout=240s
kubectl rollout status deploy/keycloak -n dev --timeout=240s
```

---

## 8. Jenkins Đã Sửa

### 8.1 Jenkins cần đủ tool

Image `jenkins/jenkins:lts-jdk17` mặc định có thể thiếu Docker CLI, kubectl, helm, kustomize. Nên tạo custom image:

```Dockerfile
FROM jenkins/jenkins:lts-jdk17

USER root

RUN apt-get update && apt-get install -y \
    docker.io git curl unzip ca-certificates gnupg lsb-release \
    && rm -rf /var/lib/apt/lists/*

RUN curl -LO "https://dl.k8s.io/release/v1.29.0/bin/linux/amd64/kubectl" \
    && install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl \
    && rm kubectl

RUN curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

RUN curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh" | bash \
    && mv kustomize /usr/local/bin/kustomize

USER jenkins
```

Docker Compose:

```yaml
services:
  jenkins:
    build: ./docker/jenkins-agent
    user: root
    container_name: jenkins
    restart: unless-stopped
    ports:
      - "8080:8080"
      - "50000:50000"
    volumes:
      - jenkins_home:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock
      - /root/.kube/config:/root/.kube/config:ro
    environment:
      - KUBECONFIG=/root/.kube/config

volumes:
  jenkins_home:
```

**Ghi chú production:** compose trên dùng `user: root`, mount `/var/run/docker.sock` và mount kubeconfig root để demo nhanh. Production nên đổi sang Jenkins agent ephemeral, service account/RBAC tối thiểu, kubeconfig riêng cho CI, và build image bằng Kaniko/BuildKit/buildx remote builder thay vì mount Docker socket host.

### 8.2 Jenkins job type

Dùng **Multibranch Pipeline** để có biến:

```text
env.BRANCH_NAME
env.TAG_NAME
env.GIT_COMMIT
```

Nếu dùng Pipeline thường, `BRANCH_NAME` có thể null.

---

## 8.3 Service catalog là input duy nhất cho CI/CD

Không hardcode danh sách service trong Jenkinsfile. Sau bước audit, tạo `services.yaml` và để Jenkinsfile/scripts đọc file này.

Danh sách module YAS thực tế cần audit tối thiểu:

```yaml
services:
  - name: backoffice-bff
    path: backoffice-bff
    type: spring
  - name: cart
    path: cart
    type: spring
  - name: customer
    path: customer
    type: spring
  - name: delivery
    path: delivery
    type: spring
  - name: identity
    path: identity
    type: spring
  - name: inventory
    path: inventory
    type: spring
  - name: location
    path: location
    type: spring
  - name: media
    path: media
    type: spring
  - name: order
    path: order
    type: spring
  - name: payment
    path: payment
    type: spring
  - name: payment-paypal
    path: payment-paypal
    type: spring
  - name: product
    path: product
    type: spring
  - name: promotion
    path: promotion
    type: spring
  - name: rating
    path: rating
    type: spring
  - name: recommendation
    path: recommendation
    type: spring
  - name: search
    path: search
    type: spring
  - name: storefront-bff
    path: storefront-bff
    type: spring
  - name: storefront
    path: storefront
    type: nextjs
  - name: tax
    path: tax
    type: spring
  - name: webhook
    path: webhook
    type: spring
```

Nếu nhóm chỉ deploy subset để kịp demo, phải ghi rõ trong báo cáo:

```text
- Core services deployed
- Services intentionally excluded
- Lý do exclude
- Luồng user nào vẫn chứng minh được yêu cầu đề bài
```

---

## 9. CI Pipeline Đã Sửa

| Trigger | Build image tag | Deploy? |
|---|---|---|
| Push feature branch | `<commit-id>` | Không auto deploy |
| Push main | `<commit-id>`, `main`, `latest` | Update GitOps `overlays/dev` |
| Git tag `vX.Y.Z` | `vX.Y.Z` | Update GitOps `overlays/staging` |
| Manual `developer_build` | resolve branch → `<commit-id>` | Update GitOps `overlays/developer` hoặc `overlays/dev` |

Pseudocode:

```groovy
pipeline {
  agent any

  environment {
    DOCKERHUB_USER = 'your-dockerhub-username'
    REGISTRY = 'https://index.docker.io/v1/'
    GITOPS_REPO = 'github.com/<org>/yas-gitops.git'
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Resolve Commit') {
      steps {
        script {
          env.COMMIT_ID = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
        }
      }
    }

    stage('Build & Push Images') {
      steps {
        script {
          def services = readYaml(file: 'services.yaml').services

          docker.withRegistry(REGISTRY, 'dockerhub-creds') {
            services.each { svc ->
              def imageName = "${DOCKERHUB_USER}/yas-${svc.name}"
              def img = docker.build("${imageName}:${COMMIT_ID}", "./${svc.path}")
              img.push("${COMMIT_ID}")

              if (env.BRANCH_NAME == 'main') {
                img.push('main')
                img.push('latest')
              }

              if (env.TAG_NAME ==~ /v\d+\.\d+\.\d+/) {
                img.push("${env.TAG_NAME}")
              }
            }
          }
        }
      }
    }

    stage('Update GitOps dev') {
      when { branch 'main' }
      steps {
        sh "./scripts/update-gitops-images.sh overlays/dev main"
      }
    }

    stage('Update GitOps staging') {
      when { expression { env.TAG_NAME ==~ /v\d+\.\d+\.\d+/ } }
      steps {
        sh "./scripts/update-gitops-images.sh overlays/staging ${TAG_NAME}"
      }
    }
  }
}
```

---

## 10. `developer_build` Đã Sửa Theo GitOps

### 10.1 Flow đúng

```text
Developer chọn branch cho từng service
   ↓
Jenkins resolve branch → commit id
   ↓
Jenkins update overlays/developer/kustomization.yaml
   ↓
Jenkins git commit + push config repo
   ↓
ArgoCD app-developer sync
   ↓
Developer truy cập domain:port
```

### 10.2 Vì sao không deploy trực tiếp?

Vì ArgoCD đang giữ desired state. Nếu Jenkins tự `kubectl set image`, state trong cluster sẽ lệch với Git. ArgoCD có thể tự restore về state trong Git.

### 10.3 Pseudocode `Jenkinsfile.developer_build`

Parameter nên được sinh từ `services.yaml`. Ví dụ dưới đây rút gọn để minh họa cách resolve branch sang image tag; khi implement thật phải bao phủ toàn bộ service trong scope đã chốt.

```groovy
pipeline {
  agent any

  parameters {
    string(name: 'tax_branch', defaultValue: 'main')
    string(name: 'product_branch', defaultValue: 'main')
    string(name: 'inventory_branch', defaultValue: 'main')
    string(name: 'order_branch', defaultValue: 'main')
    string(name: 'customer_branch', defaultValue: 'main')
    string(name: 'storefront_branch', defaultValue: 'main')
  }

  environment {
    DOCKERHUB_USER = 'your-dockerhub-username'
    GITOPS_REPO = 'github.com/<org>/yas-gitops.git'
    OVERLAY = 'overlays/developer'
    ACCESS_URL = 'http://yas-dev.local:30080'
  }

  stages {
    stage('Resolve Branch Tags') {
      steps {
        script {
          def mapping = [
            'tax': params.tax_branch,
            'product': params.product_branch,
            'inventory': params.inventory_branch,
            'order': params.order_branch,
            'customer': params.customer_branch,
            'storefront': params.storefront_branch,
          ]

          env.RESOLVED = mapping.collect { svc, branch ->
            def tag = branch == 'main'
              ? 'main'
              : sh(script: "git ls-remote https://github.com/<org>/yas.git refs/heads/${branch} | awk '{print \$1}' | head -c 7", returnStdout: true).trim()

            if (!tag) {
              error("Không tìm thấy branch ${branch} cho service ${svc}")
            }

            return "${svc}=${tag}"
          }.join(',')
        }
      }
    }

    stage('Update GitOps Overlay') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'github-creds',
          usernameVariable: 'GIT_USER',
          passwordVariable: 'GIT_TOKEN')]) {
          sh """
            rm -rf /tmp/yas-gitops
            git clone https://${GIT_USER}:${GIT_TOKEN}@${GITOPS_REPO} /tmp/yas-gitops
            cd /tmp/yas-gitops/${OVERLAY}

            IFS=',' read -ra PAIRS <<< "${RESOLVED}"
            for pair in "${PAIRS[@]}"; do
              svc="${pair%%=*}"
              tag="${pair##*=}"
              kustomize edit set image ${DOCKERHUB_USER}/yas-${svc}:${tag}
            done

            git config user.email "jenkins@ci.local"
            git config user.name "Jenkins CI"

            git add kustomization.yaml
            git commit -m "developer_build: ${RESOLVED} [skip ci]" || echo "No changes"
            git push
          """
        }
      }
    }

    stage('Print Access Info') {
      steps {
        echo """
Deploy request đã được commit vào GitOps repo.
ArgoCD sẽ sync overlay developer.

Truy cập:
${ACCESS_URL}

Nếu chưa có DNS, chạy:
bash scripts/set-hosts.sh <WORKER_TAILSCALE_IP>
"""
      }
    }
  }
}
```

---

## 11. `teardown_dev` Đã Sửa

### 11.1 Basic mode

Nếu không dùng ArgoCD:

```bash
kubectl delete namespace dev --ignore-not-found=true
```

hoặc xóa workloads nhưng giữ PVC:

```bash
kubectl delete deploy,svc,ingress,configmap,secret --all -n dev --ignore-not-found=true
```

### 11.2 GitOps mode

Không nên xóa trực tiếp resource do ArgoCD quản lý. Có 2 cách sạch hơn:

**Cách 1 — disable developer app bằng Git commit**

```text
Jenkins sửa overlays/developer/kustomization.yaml
→ bỏ resources app services hoặc set replicas = 0
→ commit/push
→ ArgoCD sync
```

**Cách 2 — xóa ArgoCD Application developer**

```bash
kubectl delete application yas-developer -n argocd
kubectl delete namespace developer
```

Cách 1 tốt hơn nếu muốn giữ lịch sử GitOps.

---

## 12. Ingress Basic Đã Sửa

### 12.1 Cài Nginx không trùng port

```bash
helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace \
  --set controller.service.type=NodePort \
  --set controller.service.nodePorts.http=30080 \
  --set controller.service.nodePorts.https=30081
```

### 12.2 Ingress base không hardcode namespace

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: yas-ingress
  annotations:
    nginx.ingress.kubernetes.io/proxy-read-timeout: "120"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "120"
spec:
  ingressClassName: nginx
  rules:
    - host: yas.dev.local
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: storefront
                port:
                  number: 3000
```

Trong `overlays/dev/kustomization.yaml`:

```yaml
namespace: dev
resources:
  - ../../base/ingress-basic
```

Trong `overlays/staging`, patch host thành `yas.staging.local`.

---

## 13. Service Mesh Đã Sửa

### 13.1 Không dùng Nginx làm entrypoint cho mTLS STRICT

Khi bật `PeerAuthentication STRICT` trong namespace app, traffic từ Nginx Ingress không có sidecar có thể bị chặn. Vì vậy trong phần nâng cao Service Mesh, dùng **Istio IngressGateway**.

Flow đúng:

```text
Browser
  → Istio IngressGateway
  → Gateway + VirtualService
  → service trong namespace dev
  → mTLS STRICT giữa các service
```

### 13.2 Istio Gateway

```yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: yas-gateway
  namespace: dev
spec:
  selector:
    istio: ingressgateway
  servers:
    - port:
        number: 80
        name: http
        protocol: HTTP
      hosts:
        - yas.mesh.local
```

### 13.3 VirtualService entrypoint

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: yas-entry
  namespace: dev
spec:
  hosts:
    - yas.mesh.local
  gateways:
    - yas-gateway
  http:
    - match:
        - uri:
            prefix: /api/orders
      route:
        - destination:
            host: order.dev.svc.cluster.local
            port:
              number: 8080
      retries:
        attempts: 3
        perTryTimeout: 2s
        retryOn: "5xx,reset,connect-failure"
    - match:
        - uri:
            prefix: /
      route:
        - destination:
            host: storefront.dev.svc.cluster.local
            port:
              number: 3000
```

### 13.4 mTLS STRICT

```yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: dev
spec:
  mtls:
    mode: STRICT
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: default-mtls
  namespace: dev
spec:
  host: "*.dev.svc.cluster.local"
  trafficPolicy:
    tls:
      mode: ISTIO_MUTUAL
```

### 13.5 AuthorizationPolicy cần allow gateway

Nếu `order` nhận request từ browser thông qua Istio Gateway, phải allow principal của gateway.

```yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-order
  namespace: dev
spec:
  selector:
    matchLabels:
      app: order
  action: ALLOW
  rules:
    - from:
        - source:
            principals:
              - "cluster.local/ns/istio-system/sa/istio-ingressgateway-service-account"
              - "cluster.local/ns/dev/sa/storefront"
      to:
        - operation:
            methods: ["GET", "POST", "PUT", "DELETE"]
```

### 13.6 Test no-sidecar đúng

Vì namespace `dev` đã bật auto-injection, muốn tạo pod không sidecar phải thêm annotation:

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
```

---

## 14. Service List Cần Audit Từ Repo YAS

Không hardcode chỉ 6 service nếu repo thật có nhiều service hơn. Trước khi viết Jenkinsfile cuối cùng, chạy:

```bash
find . -maxdepth 3 -iname "Dockerfile" -print
find . -maxdepth 3 -iname "pom.xml" -print
find . -maxdepth 3 -iname "package.json" -print
```

Tạo file `services.yaml`:

```yaml
services:
  - name: tax
    path: tax
    port: 8080
    type: spring
  - name: product
    path: product
    port: 8080
    type: spring
  - name: inventory
    path: inventory
    port: 8080
    type: spring
  - name: order
    path: order
    port: 8080
    type: spring
  - name: customer
    path: customer
    port: 8080
    type: spring
  - name: cart
    path: cart
    port: 8080
    type: spring
  - name: search
    path: search
    port: 8080
    type: spring
  - name: storefront-bff
    path: storefront-bff
    port: 8080
    type: spring
  - name: storefront
    path: storefront
    port: 3000
    type: nextjs
```

Sau audit, bổ sung các module còn lại như `backoffice-bff`, `delivery`, `identity`, `location`, `media`, `payment`, `payment-paypal`, `promotion`, `rating`, `recommendation`, `webhook` nếu chúng có Dockerfile/build path và nằm trong scope demo.

---

## 15. Checklist Đã Sửa

| Nhóm | Checklist |
|---|---|
| Cluster | `kubectl get nodes -o wide` thấy Master/Worker qua Tailscale |
| NFS | export `/srv/nfs/yas`, provisioner mount đúng path cha |
| Jenkins | custom image có docker, kubectl, helm, kustomize |
| CI | branch nào cũng build tag commit id |
| main | push tag `main/latest` và update GitOps dev |
| release | tag `vX.Y.Z` build image `vX.Y.Z` rồi update staging |
| developer_build | không direct mutate namespace do ArgoCD quản lý |
| ArgoCD | NodePort `30444`, không trùng Ingress |
| Kustomize | base không hardcode namespace |
| Basic Ingress | Nginx dùng `30080/30081` |
| Mesh | dùng Istio Gateway, không dùng Nginx vào mTLS STRICT |
| Authz | allow principal của gateway và service-to-service |
| Test | có curl 200, curl 403, Kiali graph, mTLS evidence, retry evidence |
