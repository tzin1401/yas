# Cluster Runbook - Trí Xuân Owner

## Target Topology

- Master/control-plane: kubeadm, kubectl, Helm, ArgoCD, NFS server.
- Worker: YAS workloads, ingress/gateway, app pods.
- Laptop dev: browser, hosts file, optional kubectl.

## Minimum Resources

- Master: Ubuntu 22.04/24.04, 2-4 CPU, 8 GB RAM, 50 GB disk.
- Worker: Ubuntu 22.04/24.04, 4 CPU, 8-12 GB RAM, 40 GB disk.

## Required Tools

Install `containerd`, `kubeadm`, `kubelet`, `kubectl`, `helm`, `yq`, `git`, and `tailscale`.

## Tailscale

```bash
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up
tailscale status
tailscale ip -4
```

Record:

```text
MASTER_TAILSCALE_IP=<master-ip>
WORKER_TAILSCALE_IP=<worker-ip>
```

## Kubernetes Bootstrap

On master:

```bash
sudo kubeadm init \
  --apiserver-advertise-address=${MASTER_TAILSCALE_IP} \
  --apiserver-cert-extra-sans=${MASTER_TAILSCALE_IP} \
  --pod-network-cidr=10.244.0.0/16 \
  --node-name=master

mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config

kubectl apply -f https://github.com/flannel-io/flannel/releases/latest/download/kube-flannel.yml
kubeadm token create --print-join-command
```

On worker, run the join command printed by master.

Verify:

```bash
kubectl get nodes -o wide
kubectl get pods -n kube-system
```

## NFS Storage

On master:

```bash
sudo apt-get update
sudo apt-get install -y nfs-kernel-server
sudo mkdir -p /srv/nfs/yas
sudo chown nobody:nogroup /srv/nfs/yas
sudo chmod 0777 /srv/nfs/yas
printf '/srv/nfs/yas %s(rw,sync,no_subtree_check,no_root_squash)\n' "${WORKER_TAILSCALE_IP}" | sudo tee /etc/exports
sudo exportfs -rav
sudo systemctl enable --now nfs-kernel-server
sudo exportfs -v
```

Install provisioner:

```bash
helm repo add nfs-subdir-external-provisioner https://kubernetes-sigs.github.io/nfs-subdir-external-provisioner/
helm repo update
helm upgrade --install nfs-subdir-external-provisioner nfs-subdir-external-provisioner/nfs-subdir-external-provisioner \
  --namespace nfs-provisioner --create-namespace \
  --set nfs.server=${MASTER_TAILSCALE_IP} \
  --set nfs.path=/srv/nfs/yas \
  --set storageClass.name=nfs-client
```

Verify PVC before app dependencies:

```bash
kubectl get storageclass
kubectl get pvc -A
```

## Access Ports

- Nginx Ingress HTTP: `30080`
- Nginx Ingress HTTPS: `30081`
- Istio IngressGateway HTTP: `30090`
- Istio IngressGateway HTTPS: `30490`
- ArgoCD UI: `30444`
- Kiali UI: `30201`

No two services may share a NodePort.

## Evidence

Capture:

- `tailscale status`
- `kubectl get nodes -o wide`
- `kubectl get pods -A`
- `kubectl get storageclass,pvc -A`
- ArgoCD apps `Synced/Healthy`
- App URL through hosts file and NodePort
