# DevOps CD Playbook

## Jenkins

- Keep Lab 1 CI gates intact.
- Use `services.yaml` for service scope.
- Use Docker Hub credential ID `dockerhub-creds`.
- Use GitOps SSH credential ID `github-gitops-ssh`.
- Use ArgoCD token credential ID `argocd-token`.

## GitOps

- Update `deploy/gitops/overlays/<env>` and let ArgoCD sync.
- Do not mutate `dev`, `staging`, or `developer` namespaces directly.
- Render manifests before commit.

## Cluster

- Use kubeadm over Tailscale for lab.
- Use NFS provisioner for dynamic PVCs in lab only.
- Keep NodePorts stable: Nginx `30080/30081`, Istio `30090/30490`, ArgoCD `30444`, Kiali `30201`.

## Evidence

Every demo step should produce a command output or screenshot suitable for the report.
