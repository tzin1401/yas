# YAS Version Decision

- Source repo: https://github.com/tzin1401/yas
- Lab 2 branch: `lab2/cd-platform`
- Current fork stack verified from root `pom.xml`: Java 25, Spring Boot 4.0.2
- Assignment stack: Java 21, Spring Boot 3.2
- Decision: keep the current fork stack; do not downgrade for Lab 2.
- Reason: Lab 1 CI was already implemented on this fork with Jenkins tool `JDK-25`, Maven, Spring Boot 4.0.2, and Java 25 source/target configuration.
- Impact: Jenkins agents, Docker builds, local Maven commands, and cluster smoke documentation must use Java 25-compatible tooling.

## Deployment Platform Decision

- Current Lab 2 runtime target: one Google Cloud Compute Engine VM with 32 GB RAM.
- Kubernetes: `k3s` single-node on Ubuntu 24.04 LTS, control-plane node name `gcp-ci-cd-agent` (`v1.35.5+k3s1`). Originally planned as `kubeadm`; TX switched to `k3s` during provisioning for a lighter single-node install. See ADR-003 update in `architecture-fix-notes.md`.
- Network: no Tailscale; use the VM external IP, GCP firewall, hosts-file DNS for demo names, and SSH tunnels or admin-IP allowlisting for admin UIs.

## Verified Cluster State (2026-07-03)

- Confirmed via `kubectl get nodes -o wide` over SSH: single node `gcp-ci-cd-agent`, `Ready`, `control-plane` role, `containerd://2.2.3-k3s1`.
- Namespaces present: `argocd`, `dev`, `staging`, `developer`, `mesh-demo`, `ingress-nginx`, `istio-system`, plus shared platform namespaces (`postgres`, `redis`, `keycloak`, `kafka`, `elasticsearch`).
- `mesh-demo` namespace is not documented elsewhere in `docs/project02/`; confirm its purpose/scope with TX before including it in the final report.
- `dev` namespace has running application pods; `staging`/`developer` namespaces exist but had no application pods yet at verification time — check ArgoCD Application sync/health before relying on this.

## Production Reality Note

This lab setup may use NodePort, hosts file entries, local-path storage, demo credentials, and Jenkins Docker access because they keep the course deployment reproducible on one rented server. Production deployments should use proper DNS/TLS, least-privilege RBAC, external or encrypted secret management, isolated container builders, managed or CSI-backed storage, private admin access, and managed Kubernetes or a multi-node cluster where appropriate.
