#!/usr/bin/env bash
set -euo pipefail

catalog="services.yaml"
mode="validate"

usage() {
  cat <<'USAGE'
Usage:
  ci/validate-service-catalog.sh [--catalog services.yaml]
  ci/validate-service-catalog.sh --list-deployable-names [--catalog services.yaml]
  ci/validate-service-catalog.sh --list-deployable-paths [--catalog services.yaml]

Validates services.yaml as the CI/CD service catalog. Requires yq v4.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --catalog)
      catalog="${2:-}"
      shift 2
      ;;
    --list-deployable-names)
      mode="list-deployable-names"
      shift
      ;;
    --list-deployable-paths)
      mode="list-deployable-paths"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "ERROR: unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [[ -z "${catalog}" || ! -f "${catalog}" ]]; then
  echo "ERROR: catalog not found: ${catalog}" >&2
  exit 1
fi

if ! command -v yq >/dev/null 2>&1; then
  echo "ERROR: yq v4 is required to parse ${catalog}" >&2
  exit 1
fi

if ! yq --version 2>/dev/null | grep -Eq 'version v?4\.'; then
  echo "ERROR: yq v4 is required; got: $(yq --version 2>/dev/null || true)" >&2
  exit 1
fi

case "${mode}" in
  list-deployable-names)
    yq -r '.services[] | select(.deploy == true) | .name' "${catalog}"
    exit 0
    ;;
  list-deployable-paths)
    yq -r '.services[] | select(.deploy == true) | .path' "${catalog}"
    exit 0
    ;;
esac

errors=0

fail() {
  echo "ERROR: $*" >&2
  errors=$((errors + 1))
}

require_scalar() {
  local index="$1"
  local field="$2"
  local value
  value="$(yq -r ".services[${index}].${field} // \"\"" "${catalog}")"
  if [[ -z "${value}" || "${value}" == "null" ]]; then
    fail "services[${index}].${field} is required"
  fi
}

if [[ "$(yq -r '.services | tag' "${catalog}")" != "!!seq" ]]; then
  fail ".services must be a non-empty list"
elif [[ "$(yq -r '.services | length' "${catalog}")" -eq 0 ]]; then
  fail ".services must be a non-empty list"
fi

if [[ "$(yq -r '.dependencies.platform | tag' "${catalog}")" != "!!seq" ]]; then
  fail ".dependencies.platform must be a list"
fi

mapfile -t duplicate_names < <(yq -r '.services[].name // ""' "${catalog}" | sort | uniq -d)
for name in "${duplicate_names[@]}"; do
  [[ -n "${name}" ]] && fail "duplicate service name: ${name}"
done

mapfile -t duplicate_paths < <(yq -r '.services[].path // ""' "${catalog}" | sort | uniq -d)
for path in "${duplicate_paths[@]}"; do
  [[ -n "${path}" ]] && fail "duplicate service path: ${path}"
done

mapfile -t duplicate_images < <(yq -r '.services[] | select(.deploy == true) | .imageName // ""' "${catalog}" | sort | uniq -d)
for image in "${duplicate_images[@]}"; do
  [[ -n "${image}" ]] && fail "duplicate deployable imageName: ${image}"
done

declare -A allowed_dependencies=()
while IFS= read -r dependency; do
  [[ -n "${dependency}" && "${dependency}" != "null" ]] && allowed_dependencies["${dependency}"]=1
done < <(yq -r '.dependencies.platform[]?' "${catalog}")

while IFS= read -r service_name; do
  [[ -n "${service_name}" && "${service_name}" != "null" ]] && allowed_dependencies["${service_name}"]=1
done < <(yq -r '.services[].name // ""' "${catalog}")

service_count="$(yq -r '.services | length' "${catalog}")"
for ((i = 0; i < service_count; i++)); do
  name="$(yq -r ".services[${i}].name // \"\"" "${catalog}")"
  path="$(yq -r ".services[${i}].path // \"\"" "${catalog}")"
  deploy="$(yq -r ".services[${i}].deploy | tostring" "${catalog}")"
  dockerfile="$(yq -r ".services[${i}].dockerfile // \"\"" "${catalog}")"
  chart="$(yq -r ".services[${i}].chart // \"\"" "${catalog}")"
  image_name="$(yq -r ".services[${i}].imageName // \"\"" "${catalog}")"
  type="$(yq -r ".services[${i}].type // \"\"" "${catalog}")"
  exclusion_reason="$(yq -r ".services[${i}].exclusionReason // \"\"" "${catalog}")"

  require_scalar "${i}" "name"
  require_scalar "${i}" "path"
  require_scalar "${i}" "type"

  if [[ -n "${name}" && ! "${name}" =~ ^[a-z0-9][a-z0-9-]*[a-z0-9]$ ]]; then
    fail "${name}: service name must be lowercase kebab-case"
  fi

  case "${type}" in
    backend|bff|ui|library|config) ;;
    *) fail "${name:-services[${i}]}: unsupported type '${type}'" ;;
  esac

  if [[ "${deploy}" != "true" && "${deploy}" != "false" ]]; then
    fail "${name:-services[${i}]}: deploy must be true or false"
  fi

  if [[ "$(yq -r ".services[${i}].dependencies | tag" "${catalog}")" != "!!seq" ]]; then
    fail "${name:-services[${i}]}: dependencies must be a list"
  fi

  if [[ -n "${path}" && ! -d "${path}" ]]; then
    fail "${name}: path does not exist: ${path}"
  fi

  if [[ "${deploy}" == "true" ]]; then
    require_scalar "${i}" "dockerfile"
    require_scalar "${i}" "chart"
    require_scalar "${i}" "imageName"

    case "${type}" in
      backend|bff)
        if [[ -n "${path}" && ! -f "${path}/pom.xml" ]]; then
          fail "${name}: ${type} service path is missing pom.xml: ${path}"
        fi
        ;;
      ui)
        if [[ -n "${path}" && ! -f "${path}/package.json" ]]; then
          fail "${name}: ui service path is missing package.json: ${path}"
        fi
        ;;
    esac

    if [[ -n "${dockerfile}" && ! -f "${dockerfile}" ]]; then
      fail "${name}: dockerfile does not exist: ${dockerfile}"
    fi
    if [[ -n "${path}" && -n "${dockerfile}" && "${dockerfile}" != "${path}/"* ]]; then
      fail "${name}: dockerfile must be under service path '${path}': ${dockerfile}"
    fi
    if [[ -n "${chart}" && ! -d "${chart}" ]]; then
      fail "${name}: chart directory does not exist: ${chart}"
    elif [[ -n "${chart}" && ! -f "${chart}/Chart.yaml" ]]; then
      fail "${name}: chart is missing Chart.yaml: ${chart}"
    fi
    if [[ -n "${image_name}" && ! "${image_name}" =~ ^yas-[a-z0-9-]+$ ]]; then
      fail "${name}: imageName must match yas-<name>: ${image_name}"
    fi
  else
    if [[ -z "${exclusion_reason}" ]]; then
      fail "${name}: excluded service requires exclusionReason"
    fi
    if [[ "${type}" == "library" && -n "${path}" && ! -f "${path}/pom.xml" ]]; then
      fail "${name}: library exclusion path is missing pom.xml: ${path}"
    fi
    if [[ -n "${dockerfile}" && "${dockerfile}" != "null" ]]; then
      fail "${name}: excluded service must not define dockerfile"
    fi
    if [[ -n "${chart}" && "${chart}" != "null" ]]; then
      fail "${name}: excluded service must not define chart"
    fi
  fi

  while IFS= read -r dependency; do
    [[ -z "${dependency}" || "${dependency}" == "null" ]] && continue
    if [[ -z "${allowed_dependencies[${dependency}]:-}" ]]; then
      fail "${name}: dependency '${dependency}' is not a service or platform dependency"
    fi
  done < <(yq -r ".services[${i}].dependencies[]?" "${catalog}")
done

if [[ "${errors}" -gt 0 ]]; then
  echo "Catalog validation failed with ${errors} error(s)." >&2
  exit 1
fi

echo "Catalog validation OK: ${service_count} services checked."
