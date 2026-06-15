# GitOps Base

Base manifests must stay namespace-neutral. Set namespaces only in environment overlays.

This base renders the deployable YAS application services from the existing Helm charts under `k8s/charts/**`.

- It intentionally does not set a namespace.
- Docker Hub image repositories are overridden to `docker.io/${DOCKERHUB_USERNAME}/yas-<service>` placeholders.
- Environment overlays own the deploy namespace and image tags.
- Jenkins release/deploy jobs should patch overlay image entries to the real `docker.io/$DOCKERHUB_USERNAME/yas-<service>:<tag>` targets before ArgoCD sync.

Render locally with Helm-enabled Kustomize:

```sh
kubectl kustomize deploy/gitops/base --enable-helm --load-restrictor=LoadRestrictionsNone
kubectl kustomize deploy/gitops/overlays/dev --enable-helm --load-restrictor=LoadRestrictionsNone
kubectl kustomize deploy/gitops/overlays/staging --enable-helm --load-restrictor=LoadRestrictionsNone
kubectl kustomize deploy/gitops/overlays/developer --enable-helm --load-restrictor=LoadRestrictionsNone
```

If `helm` is not installed, install it first because Kustomize invokes the Helm binary for chart inflation.
