# ArgoCD Applications

Apply these manifests after ArgoCD is installed:

```bash
kubectl apply -f deploy/gitops/argocd/apps/
```

The applications track branch `lab2/cd-platform` and sync overlays from this same repository.
