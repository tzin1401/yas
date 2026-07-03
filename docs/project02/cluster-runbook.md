# Cluster Runbook - GCP VM Single-Node

## Target Topology

- One Google Cloud Compute Engine VM runs Kubernetes, Jenkins tooling, ArgoCD, ingress, mesh, and YAS workloads.
- Kubernetes distribution: `k3s` single-node (control-plane node name `gcp-ci-cd-agent`, `v1.35.5+k3s1`). Originally planned as `kubeadm` — TX switched to `k3s` during provisioning; see ADR-003 update in `architecture-fix-notes.md`. The `kubeadm`-specific steps below (kubeadm init, apt repo, taint removal) do not apply; k3s installs via its own single-command installer and does not taint the control-plane node.
- OS: Ubuntu 24.04 LTS.
- VM size: 32 GB RAM, recommended 4-8 vCPU, 150 GB+ persistent disk.
- Tailscale is not used.

## Access Model

- Public/demo access:
  - Nginx Ingress HTTP/HTTPS: `30080/30081`
  - Istio IngressGateway HTTP/HTTPS: `30090/30490`
- Admin access:
  - Jenkins UI, ArgoCD UI, and Kiali must use SSH tunnel or GCP firewall allowlisting for the admin IP.
  - Do not open Jenkins, ArgoCD, Kiali, Kubernetes API, databases, or admin consoles to `0.0.0.0/0`.

Example SSH tunnels:

```bash
ssh -L 8080:127.0.0.1:8080 \
    -L 30444:127.0.0.1:30444 \
    -L 30201:127.0.0.1:30201 \
    <user>@${GCP_VM_EXTERNAL_IP}
```

## GCP VM And Firewall

Record these values in evidence:

```text
GCP_VM_EXTERNAL_IP=<external-ip>
GCP_VM_INTERNAL_IP=<internal-ip>
ADMIN_SOURCE_CIDR=<your-public-ip>/32
```

Minimum firewall intent:

- Allow SSH `22/tcp` from `ADMIN_SOURCE_CIDR`.
- Allow app/demo NodePorts `30080`, `30081`, `30090`, `30490` from the demo audience only.
- Allow admin NodePorts `30444`, `30201`, and Jenkins `8080` only from `ADMIN_SOURCE_CIDR`, or do not expose them and use SSH tunnel.
- Keep Kubernetes API `6443` restricted to the VM/admin path.

## Required Tools

Install host and cluster tooling on the VM:

Before running cluster, GitOps, or mesh commands, verify the operator shell has the expected CLIs:

```bash
yq --version
helm version --short
kustomize version
kubectl version --client
argocd version --client
istioctl version --remote=false
```

Gate: do not continue until all commands return a version. Install missing tools first, then capture the command output as evidence.

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg lsb-release apt-transport-https git yq
```

Install container runtime and Kubernetes tooling:

```bash
sudo apt-get install -y containerd
sudo mkdir -p /etc/containerd
containerd config default | sudo tee /etc/containerd/config.toml >/dev/null
sudo sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml
sudo systemctl enable --now containerd

sudo swapoff -a
sudo sed -i.bak '/ swap / s/^/#/' /etc/fstab

sudo modprobe overlay
sudo modprobe br_netfilter
cat <<'EOF' | sudo tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF
cat <<'EOF' | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables  = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward                 = 1
EOF
sudo sysctl --system
```

Actual bootstrap used `k3s` instead of `kubeadm`/`kubelet` apt packages. Install Helm and any CI/CD CLIs required by Jenkins in addition to the k3s install below.

## Kubernetes Bootstrap

On the VM (as actually run — k3s, not kubeadm):

```bash
curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="server \
  --node-name=gcp-ci-cd-agent \
  --tls-san=${GCP_VM_INTERNAL_IP} --tls-san=${GCP_VM_EXTERNAL_IP}" sh -

mkdir -p "$HOME/.kube"
sudo cp -i /etc/rancher/k3s/k3s.yaml "$HOME/.kube/config"
sudo chown "$(id -u):$(id -g)" "$HOME/.kube/config"
```

k3s ships its own bundled CNI (flannel VXLAN by default) and does **not** taint the control-plane node, so the `kubectl taint nodes --all node-role.kubernetes.io/control-plane-` step used by kubeadm is not needed here — workloads schedule on the single node immediately.

Note: k3s bundles Traefik as its default ingress controller. This deployment removed it (`helm-delete-traefik` job visible in `kube-system`) in favor of the Nginx Ingress installed later in this runbook, to match the documented `30080/30081` NodePort design.

Verify:

```bash
kubectl get nodes -o wide
kubectl get pods -n kube-system
kubectl cluster-info
```

## Storage

Use local-path dynamic storage for the single-node lab:

```bash
kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/master/deploy/local-path-storage.yaml
kubectl patch storageclass local-path -p '{"metadata":{"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'
kubectl get storageclass
```

Local-path storage is lab-only. It is tied to this VM and does not provide managed replication or cross-node migration.

## GitOps Readiness

ArgoCD owns the `dev`, `staging`, and `developer` namespaces. Do not run `kubectl set image` or direct `kubectl apply` against workloads in those namespaces. Jenkins updates GitOps files, and ArgoCD reconciles the cluster.

Verify desired state before syncing:

```bash
yq '.services[] | select(.deploy == true) | .name' services.yaml
kustomize build --enable-helm --load-restrictor=LoadRestrictionsNone deploy/gitops/overlays/dev >/tmp/yas-dev-render.yaml
kustomize build --enable-helm --load-restrictor=LoadRestrictionsNone deploy/gitops/overlays/staging >/tmp/yas-staging-render.yaml
kustomize build --enable-helm --load-restrictor=LoadRestrictionsNone deploy/gitops/overlays/developer >/tmp/yas-developer-render.yaml
kubectl create namespace dev --dry-run=client -o yaml
kubectl create namespace staging --dry-run=client -o yaml
kubectl create namespace developer --dry-run=client -o yaml
```

Verify ArgoCD applications after Jenkins commits desired state:

```bash
argocd app list
argocd app get yas-dev
argocd app get yas-staging
argocd app get yas-developer
argocd app wait yas-dev --health --sync --timeout 600
argocd app wait yas-staging --health --sync --timeout 600
argocd app wait yas-developer --health --sync --timeout 600
kubectl get pods,svc,ingress -n dev
kubectl get pods,svc,ingress -n staging
kubectl get pods,svc,ingress -n developer
```

Gate: every required ArgoCD app must be `Synced` and `Healthy` before recording application URL evidence.

## Staging Immutability

Staging must use immutable release tags such as `vX.Y.Z`. It must not deploy `latest`, `main`, or branch names.

Before promoting staging, verify the GitOps diff and rendered manifests:

```bash
git diff -- deploy/gitops/overlays/staging
kustomize build --enable-helm --load-restrictor=LoadRestrictionsNone deploy/gitops/overlays/staging | yq '.. | select(type == "!!map" and has("image")) | .image' -
kustomize build --enable-helm --load-restrictor=LoadRestrictionsNone deploy/gitops/overlays/staging | yq '.. | select(type == "!!map" and has("image") and (.image | test(":(latest|main)$"))) | .image' -
```

Gate: the first render command must show only Docker Hub images in the form `docker.io/$DOCKERHUB_USERNAME/yas-<service>:vX.Y.Z`; the mutable-tag check must produce no output.

## Ingress And Platform Controllers

Install Nginx Ingress with stable NodePorts:

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

Install ArgoCD and expose it only through SSH tunnel or admin-restricted firewall:

```bash
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl patch svc argocd-server -n argocd \
  -p '{"spec":{"type":"NodePort","ports":[{"port":443,"nodePort":30444,"targetPort":8080,"protocol":"TCP","name":"https"}]}}'
kubectl apply -f deploy/gitops/argocd/apps/
```

## Hosts File And App Access

For demo access without real DNS, add entries on the client machine:

```text
<GCP_VM_EXTERNAL_IP> yas.dev.local
<GCP_VM_EXTERNAL_IP> yas.staging.local
<GCP_VM_EXTERNAL_IP> yas.developer.local
<GCP_VM_EXTERNAL_IP> yas.mesh.local
```

Example checks:

```bash
curl -H "Host: yas.dev.local" "http://${GCP_VM_EXTERNAL_IP}:30080/"
curl -H "Host: yas.mesh.local" "http://${GCP_VM_EXTERNAL_IP}:30090/"
```

## Evidence

Capture:

- GCP VM machine type, memory, disk, and OS version.
- GCP firewall rules or screenshots proving admin access is restricted.
- SSH tunnel command or screenshot for admin UI access.
- Tool version gate output for `yq`, `helm`, `kustomize`, `kubectl`, `argocd`, and `istioctl`
- `kubectl get nodes -o wide`
- `kubectl describe node gcp-ci-cd-agent`
- `kubectl get pods -A`
- `kubectl get storageclass,pvc -A`
- ArgoCD apps `Synced/Healthy`
- GitOps render output for changed overlays
- Staging immutable image tag evidence
- App URL through hosts file/Host header and NodePort.

## Production Reality Notes

- NodePort, hosts file routing, local-path storage, demo credentials, and Jenkins Docker access are lab conveniences only.
- Production should use DNS and TLS, least-privilege RBAC, external or sealed secrets, isolated image builders, managed storage or CSI-backed storage, and auditable GitOps promotion controls.
