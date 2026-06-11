# Project Map

## Root

- `Jenkinsfile`: Lab 1 CI pipeline. Lab 2 extends it with image/CD behavior.
- `ci/`: helper scripts for changed module detection, coverage gate, and tool verification.
- `services.yaml`: Lab 2 service catalog and dependency source of truth.
- `docs/project02/`: assignment, fixed architecture notes, final plan, and version decision.
- `.specify/` and `specs/001-yas-lab2-cd/`: Spec Kit SDD workflow.

## Application Source

- Backend/BFF Maven modules live at root-level service folders such as `cart/`, `order/`, `tax/`, `storefront-bff/`.
- Frontend Next.js apps live in `backoffice/` and `storefront/`.
- `common-library/` is a Maven dependency, not a deployable runtime image.

## Deployment Source

- Existing upstream Helm charts live in `k8s/charts/**`.
- Lab 2 GitOps state lives in `deploy/gitops/**`.
- ArgoCD app manifests live in `deploy/gitops/argocd/apps/`.

## Agent Entry Points

1. Read `AGENTS.md`.
2. Read `docs/project02/final-plan-lab2-cd.md`.
3. Read `services.yaml`.
4. Read the relevant `specs/001-yas-lab2-cd/` artifact.
5. Read the target source/config files before editing.
