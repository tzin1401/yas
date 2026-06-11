# YAS Lab 2 CD Constitution

## Core Principles

### I. GitOps Is The Deployment Source Of Truth

ArgoCD-managed environments (`dev`, `staging`, `developer`) must be changed through `deploy/gitops/**`. Jenkins may build images and commit desired state, but must not mutate these namespaces directly with `kubectl set image`, ad-hoc `kubectl apply`, or manual deletes.

### II. Service Catalog First

All CD automation must read from `services.yaml` or a generated view of that catalog. Service name, path, Dockerfile, Helm chart, image name, deployability, and dependencies must not drift across Jenkins, GitOps, and docs.

### III. Existing CI Gates Stay Enforced

Lab 2 extends the Lab 1 Jenkins pipeline. Gitleaks, unit tests, JaCoCo reports, coverage threshold, Maven build, SonarQube, and Snyk must remain active for code/service changes. Docs, Spec Kit, agent context, and GitOps-only commits may skip full Maven/image work through explicit path classification.

### IV. Immutable Images For CD

Feature/developer images use commit SHA tags. `main` may publish `main` and `latest` for lab dev. Staging uses release tags such as `vX.Y.Z` and must not deploy `latest`.

### V. Lab Versus Production Must Be Explicit

NodePort, hosts file, Tailscale, NFS on master, demo credentials, and Jenkins Docker access are acceptable for this course lab only. Production notes must call out DNS/TLS, least-privilege RBAC, external/encoded secrets, isolated builders, and managed or CSI-backed storage.

## Technical Constraints

- Repo: `https://github.com/tzin1401/yas.git`
- Branch: `lab2/cd-platform`
- Java/Spring decision: keep Java 25 and Spring Boot 4.0.2 from the current fork; document deviation from the assignment.
- Container registry: Docker Hub, format `docker.io/$DOCKERHUB_USERNAME/yas-<service>:<tag>`.
- Kubernetes access ports: Nginx `30080/30081`, Istio `30090/30490`, ArgoCD `30444`, Kiali `30201`.
- Required environments: `dev`, `staging`, `developer`.

## Development Workflow

1. Update SDD/spec/docs before implementing major CD behavior.
2. Validate service catalog before changing Jenkins or GitOps.
3. Render manifests before committing GitOps changes.
4. Keep credentials in Jenkins/Kubernetes secret stores, never in Git.
5. Capture command output/screenshots in the report evidence workflow.

## Governance

This constitution overrides local convenience. Any change that bypasses GitOps, weakens CI gates, changes the Java/Spring version decision, or introduces committed secrets must be rejected unless a new ADR is added under `docs/project02/` and approved by the team.

**Version**: 1.0.0 | **Ratified**: 2026-06-11 | **Last Amended**: 2026-06-11
