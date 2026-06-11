# Service Mesh Runbook

Install Istio and Kiali only after the basic CD path is stable.

## Requirements

- Namespace selected for mesh demo has sidecar injection enabled.
- Pods must show READY `2/2`.
- mTLS mode must be STRICT for the demo namespace.

## Authorization Policy

Policies must show both allow and deny evidence:

- Allow ingress gateway to call BFF/API.
- Allow expected service-to-service flows such as `order -> tax`, `order -> customer`, and `order -> payment`.
- Deny curl from a pod that is not authorized.

## Retry Evidence

Target route: `tax`.

- Configure retry attempts: `3`.
- Configure per-try timeout.
- Use 50% fault abort for lab evidence.
- Do not use 100% fault abort as the only retry proof.

## Evidence

- `istioctl authn tls-check`
- curl allow logs
- curl deny logs
- retry curl logs
- Kiali topology screenshot
