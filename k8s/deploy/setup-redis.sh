#!/usr/bin/env bash
set -euo pipefail
set -x

# Read configuration value from cluster-config.yaml or a local override.
CONFIG_FILE="${YAS_CLUSTER_CONFIG:-./cluster-config.yaml}"
if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Missing config file: $CONFIG_FILE" >&2
  exit 1
fi

REDIS_PASSWORD="$(yq -r '.redis.password' "$CONFIG_FILE")"

helm upgrade --install redis \
  --set auth.password="$REDIS_PASSWORD" \
  oci://registry-1.docker.io/bitnamicharts/redis -n redis --create-namespace
