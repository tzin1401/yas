# Staging Overlay

The `staging` environment tracks immutable release tags such as `v1.2.3`.

Never deploy `latest` in staging.

Intent:

- Namespace: `staging`
- Default image tag: `v0.0.0` placeholder until the first release promotion.
- Jenkins `release_staging` must patch every promoted service to an immutable `vX.Y.Z` tag.

Validate staging tags before committing GitOps changes:

```sh
deploy/gitops/validate-staging-immutable.sh
kubectl kustomize deploy/gitops/overlays/staging --enable-helm --load-restrictor=LoadRestrictionsNone
```
