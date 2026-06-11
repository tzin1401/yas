# YAS Lab 2 CD Agent Rules

## Source Of Truth

- Branch: `lab2/cd-platform`
- Service catalog: `services.yaml`
- SDD: `.specify/memory/constitution.md` and `specs/001-yas-lab2-cd/`
- GitOps desired state: `deploy/gitops/**`
- Project docs: `docs/project02/**`

## Hard Rules

- Never commit real secrets, kubeconfig files, tokens, Docker Hub passwords, Snyk tokens, or SonarQube tokens.
- Never run `kubectl set image` or direct `kubectl apply` into ArgoCD-managed namespaces: `dev`, `staging`, `developer`.
- Jenkins updates GitOps files only; ArgoCD owns cluster sync.
- Final CD images use Docker Hub: `docker.io/$DOCKERHUB_USERNAME/yas-<service>:<tag>`.
- Do not use mutable `latest` in staging.
- Do not weaken existing Lab 1 CI gates: Gitleaks, tests, JaCoCo coverage, build, SonarQube, and Snyk.
- Do not hardcode service lists in Jenkins or GitOps logic when `services.yaml` can be used.

## Required Checks Before Commit

- Run `git status --short`.
- Validate `services.yaml` parses and matches repo artifacts.
- Render or dry-run changed Helm/Kustomize manifests.
- Confirm no real secret appears in staged diff.
- Update `docs/project02/**` or `.agents/evidence/README.md` when deployment behavior changes.

## Commit Style

- `docs(lab2): ...`
- `ci(lab2): ...`
- `cd(lab2): ...`
- `gitops(lab2): ...`
- `mesh(lab2): ...`

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->
