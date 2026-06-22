# Spec: YAS Lab 2 Continuous Delivery Platform

## Objective

Build the Lab 2 CD layer for the existing `tzin1401/yas` fork. The platform must preserve Lab 1 CI gates, build Docker Hub images for changed services, and deploy YAS to Kubernetes through GitOps-managed `dev`, `staging`, and `developer` environments. The current runtime target is a single Google Cloud Compute Engine VM with 32 GB RAM running `kubeadm` single-node Kubernetes.

## Users

- Developer: pushes branches and manually deploys a preview environment through `developer_build`.
- Release owner: tags `vX.Y.Z` and deploys staging.
- Operator: validates the GCP VM, Kubernetes, ArgoCD, rollback, teardown, and service mesh evidence.

## Functional Requirements

- FR-001: The repo must use branch `lab2/cd-platform` for Lab 2 work.
- FR-002: `services.yaml` must define every deployable service, Dockerfile, Helm chart, image name, and dependency.
- FR-003: Jenkins must keep Lab 1 gates for code/service changes.
- FR-004: Jenkins must build and push Docker Hub images tagged by commit SHA.
- FR-005: `main` builds must also publish `main` and `latest` tags for lab dev.
- FR-006: release tag builds must publish immutable `vX.Y.Z` images for staging.
- FR-007: Jenkins must update GitOps files rather than mutate ArgoCD-managed namespaces directly.
- FR-008: ArgoCD must manage `dev`, `staging`, and `developer`.
- FR-009: `developer_build` must deploy branch-specific images for selected services and default all others to `main`.
- FR-010: `teardown_developer` must remove developer resources through GitOps/ArgoCD prune.
- FR-011: GitOps/docs/spec/agent-only commits must skip full Maven/image pipeline.
- FR-012: Service mesh evidence must show mTLS, authorization allow/deny, retry, and Kiali topology.
- FR-013: The cluster runbook must provision a GCP VM based `kubeadm` single-node cluster without Tailscale.
- FR-014: Admin interfaces must be accessed through SSH tunnels or firewall allowlisting, not broad public exposure.

## Non-Functional Requirements

- NFR-001: No real secrets are committed.
- NFR-002: Staging never deploys `latest`.
- NFR-003: Base GitOps manifests must not hardcode environment namespaces.
- NFR-004: NodePort, hosts-file DNS, local-path storage, and demo credentials must be documented as lab-only.
- NFR-005: The plan must not rely on Tailscale.

## Success Criteria

- Jenkins CI still passes for a changed service.
- Docker Hub contains commit SHA, `main/latest`, and `vX.Y.Z` image tags.
- The GCP VM shows one Ready Kubernetes node with 32 GB-class capacity.
- ArgoCD apps for `dev`, `staging`, and `developer` are `Synced/Healthy`.
- `developer_build` deploys one branch-specific service with dependencies ready.
- Teardown and rollback produce auditable Jenkins and ArgoCD logs.
- App URLs are reachable through the VM external IP plus hosts file or Host header.
- Mesh demo has curl and Kiali evidence.

## Boundaries

- Always: follow `AGENTS.md`, use `services.yaml`, render manifests before GitOps commits, restrict admin access, and keep secrets out of Git.
- Ask first: changing Java/Spring version decision, removing CI gates, adding real external services, exposing admin UIs publicly, or replacing kubeadm with another Kubernetes distribution.
- Never: commit secrets, deploy direct into ArgoCD-managed namespaces, use GHCR upstream images for final CD, or reintroduce Tailscale as the Lab 2 network path.
