# YAS K8S Deployment On GCP VM

This folder contains upstream YAS platform setup scripts and Helm values. For Lab 2 CD, run them on the GCP VM single-node cluster after the base cluster from `docs/project02/cluster-runbook.md` is ready.

## Target

- Google Cloud Compute Engine VM, 32 GB RAM.
- Ubuntu 24.04 LTS.
- `k3s` single-node Kubernetes (node `gcp-ci-cd-agent`); originally planned as `kubeadm`.
- Default StorageClass: `local-path`.
- No Tailscale.

## Configuration

The committed `cluster-config.yaml` contains placeholder values only. Do not put real secrets in Git.

Create a local config:

```bash
cp cluster-config.yaml cluster-config.local.yaml
$EDITOR cluster-config.local.yaml
```

Run scripts with:

```bash
export YAS_CLUSTER_CONFIG=./cluster-config.local.yaml
```

`cluster-config.local.yaml` is ignored by Git.

## Setup Order

From `k8s/deploy`:

```bash
./setup-keycloak.sh
./setup-redis.sh
./setup-cluster.sh
./deploy-yas-configuration.sh
./deploy-yas-applications.sh
```

Validate between each step:

```bash
kubectl get pods -A
kubectl get storageclass,pvc -A
kubectl get ingress,svc -A
```

## Hosts File

For demo access without real DNS, add entries on your client machine:

```text
<GCP_VM_EXTERNAL_IP> pgadmin.yas.local
<GCP_VM_EXTERNAL_IP> akhq.yas.local
<GCP_VM_EXTERNAL_IP> kibana.yas.local
<GCP_VM_EXTERNAL_IP> identity.yas.local
<GCP_VM_EXTERNAL_IP> backoffice.yas.local
<GCP_VM_EXTERNAL_IP> storefront.yas.local
<GCP_VM_EXTERNAL_IP> grafana.yas.local
<GCP_VM_EXTERNAL_IP> api.yas.local
```

Keep admin tools restricted by firewall or SSH tunnel. Do not expose database, Keycloak admin, Grafana admin, ArgoCD, Kiali, or Jenkins broadly on the public Internet.

## Resource References

- PostgreSQL operator: https://github.com/zalando/postgres-operator
- Elasticsearch: https://github.com/elastic/cloud-on-k8s
- Kafka: https://github.com/strimzi/strimzi-kafka-operator
- Debezium Connect: https://debezium.io/documentation/reference/stable/operations/kubernetes.html
- Keycloak: https://www.keycloak.org/operator/installation
- Redis: https://artifacthub.io/packages/helm/bitnami/redis
- Reloader: https://github.com/stakater/Reloader
- Prometheus: https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack
- Grafana: https://github.com/grafana-operator/grafana-operator
- Loki: https://github.com/grafana/loki/tree/main/production/helm/loki
- Tempo: https://github.com/grafana/helm-charts/tree/main/charts/tempo
- Promtail: https://github.com/grafana/helm-charts/tree/main/charts/promtail
- OpenTelemetry: https://github.com/open-telemetry/opentelemetry-operator

## Credentials

Bootstrap credentials are stored in Kubernetes Secrets by the charts. Use local placeholder values only for throwaway lab runs. Do not paste real passwords into screenshots, logs, reports, or Git.
