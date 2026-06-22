# Cluster Runbook - GCP VM Single-Node

## Target Topology

- One Google Cloud Compute Engine VM runs Kubernetes, Jenkins tooling, ArgoCD, ingress, mesh, and YAS workloads.
- Kubernetes distribution: `kubeadm` single-node.
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

Install `kubeadm`, `kubelet`, and `kubectl` from the official Kubernetes apt repository, then install Helm and any CI/CD CLIs required by Jenkins.

## Kubernetes Bootstrap

On the VM:

```bash
sudo kubeadm init \
  --apiserver-advertise-address=${GCP_VM_INTERNAL_IP} \
  --apiserver-cert-extra-sans=${GCP_VM_INTERNAL_IP},${GCP_VM_EXTERNAL_IP} \
  --pod-network-cidr=10.244.0.0/16 \
  --node-name=yas-gcp-single-node

mkdir -p "$HOME/.kube"
sudo cp -i /etc/kubernetes/admin.conf "$HOME/.kube/config"
sudo chown "$(id -u):$(id -g)" "$HOME/.kube/config"

kubectl apply -f https://github.com/flannel-io/flannel/releases/latest/download/kube-flannel.yml
kubectl taint nodes --all node-role.kubernetes.io/control-plane- || true
```

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
- `kubectl get nodes -o wide`
- `kubectl describe node yas-gcp-single-node`
- `kubectl get pods -A`
- `kubectl get storageclass,pvc -A`
- ArgoCD apps `Synced/Healthy`
- App URL through hosts file/Host header and NodePort.
