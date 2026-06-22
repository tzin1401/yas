#!/usr/bin/env bash
set -euo pipefail
set -x

# Read configuration value from cluster-config.yaml or a local override.
CONFIG_FILE="${YAS_CLUSTER_CONFIG:-./cluster-config.yaml}"
if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Missing config file: $CONFIG_FILE" >&2
  exit 1
fi

DOMAIN="$(yq -r '.domain' "$CONFIG_FILE")"
POSTGRESQL_USERNAME="$(yq -r '.postgresql.username' "$CONFIG_FILE")"
POSTGRESQL_PASSWORD="$(yq -r '.postgresql.password' "$CONFIG_FILE")"
BOOTSTRAP_ADMIN_USERNAME="$(yq -r '.keycloak.bootstrapAdmin.username' "$CONFIG_FILE")"
BOOTSTRAP_ADMIN_PASSWORD="$(yq -r '.keycloak.bootstrapAdmin.password' "$CONFIG_FILE")"
KEYCLOAK_BACKOFFICE_REDIRECT_URL="$(yq -r '.keycloak.backofficeRedirectUrl' "$CONFIG_FILE")"
KEYCLOAK_STOREFRONT_REDIRECT_URL="$(yq -r '.keycloak.storefrontRedirectUrl' "$CONFIG_FILE")"

#Install CRD keycloak
kubectl create namespace keycloak
kubectl apply -f https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/26.0.2/kubernetes/keycloaks.k8s.keycloak.org-v1.yml
kubectl apply -f https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/26.0.2/kubernetes/keycloakrealmimports.k8s.keycloak.org-v1.yml
kubectl apply -f https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/26.0.2/kubernetes/kubernetes.yml -n keycloak

# Install keycloak
helm upgrade --install keycloak ./keycloak/keycloak \
--namespace keycloak \
--set hostname="identity.$DOMAIN" \
--set postgresql.username="$POSTGRESQL_USERNAME" \
--set postgresql.password="$POSTGRESQL_PASSWORD" \
--set bootstrapAdmin.username="$BOOTSTRAP_ADMIN_USERNAME" \
--set bootstrapAdmin.password="$BOOTSTRAP_ADMIN_PASSWORD" \
--set backofficeRedirectUrl="$KEYCLOAK_BACKOFFICE_REDIRECT_URL" \
--set storefrontRedirectUrl="$KEYCLOAK_STOREFRONT_REDIRECT_URL"
