# Final Plan Lab 2 CD Cho `tzin1401/yas`

## Summary

- Repo: `https://github.com/tzin1401/yas.git`; branch triển khai: `lab2/cd-platform`; không tạo fork mới.
- Kế thừa Lab 1 Jenkins CI: changed-module detection, Gitleaks, JUnit/JaCoCo, coverage gate 70%, Maven build, SonarQube, Snyk.
- Lab 2 bổ sung: Docker Hub image pipeline, K8s cluster, ArgoCD GitOps `dev/staging/developer`, `developer_build`, teardown, rollback, Istio/Kiali.
- Version decision: giữ repo hiện tại Java 25 + Spring Boot 4.0.2, không downgrade về Java 21/Spring Boot 3.2.

## Implementation Scope

- Setup Spec Kit, SDD artifacts, `AGENTS.md`, `.agents/**`, and project docs.
- Create `services.yaml` as the shared service catalog for Jenkins, GitOps, and docs.
- Create GitOps/ArgoCD skeleton under `deploy/gitops/**`.
- Add Jenkins skip-CI path classification for docs/GitOps/spec/agent-only commits.
- Leave real cluster creation and Jenkins credential provisioning as runbook-driven steps because they require external machines and Jenkins administrator access.

## Acceptance Checklist

- `AGENTS.md` has hard rules.
- `docs/project02/project-version.md` locks Java 25/Spring Boot 4.0.2.
- `services.yaml` matches repo artifacts.
- GitOps app templates point to `lab2/cd-platform`.
- Docs/GitOps-only commits do not trigger full Maven/image pipeline.
- ArgoCD-managed namespaces are never mutated directly by Jenkins.
- No real secrets are committed.
