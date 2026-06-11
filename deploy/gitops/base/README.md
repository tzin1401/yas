# GitOps Base

Base manifests must stay namespace-neutral. Set namespaces only in environment overlays.

This folder is intentionally minimal in the first Lab 2 scaffold. Use existing YAS Helm charts under `k8s/charts/**` as the source for application manifests and add rendered or Kustomize-managed resources here only when the team chooses the final chart rendering strategy.
