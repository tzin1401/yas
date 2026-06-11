# Developer Overlay

The `developer` environment is updated by the Jenkins `developer_build` job.

Required platform dependencies before app success:

- PostgreSQL
- Redis
- Keycloak
- Kafka + Zookeeper
- Elasticsearch
- `yas-configuration`
- Ingress or Gateway

Jenkins must update this overlay through GitOps and then sync ArgoCD. It must not mutate the namespace directly.
