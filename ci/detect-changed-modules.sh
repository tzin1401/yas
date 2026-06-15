#!/usr/bin/env bash
set -euo pipefail

catalog_file="${CATALOG_FILE:-services.yaml}"

read_maven_modules() {
  sed -n '/<modules>/,/<\/modules>/p' pom.xml \
    | sed -n 's:.*<module>\([^<]*\)</module>.*:\1:p'
}

read_catalog_paths() {
  if [[ -f "${catalog_file}" ]] && command -v yq >/dev/null 2>&1; then
    yq -r '.services[] | .path' "${catalog_file}"
  else
    read_maven_modules
  fi
}

mapfile -t maven_modules < <(read_maven_modules)
declare -A is_maven_module=()
for module in "${maven_modules[@]}"; do
  is_maven_module["${module}"]=1
done

modules=()
while IFS= read -r service_path; do
  if [[ -n "${service_path}" && -n "${is_maven_module[${service_path}]:-}" ]]; then
    modules+=("${service_path}")
  fi
done < <(read_catalog_paths)

if [[ ${#modules[@]} -eq 0 ]]; then
  echo "ERROR: no Maven modules resolved from ${catalog_file} and pom.xml" >&2
  exit 1
fi

base_ref="${1:-}"
if [[ -z "${base_ref}" ]]; then
  if [[ -n "${CHANGE_TARGET:-}" ]]; then
    base_ref="origin/${CHANGE_TARGET}"
  elif git rev-parse --verify HEAD~1 >/dev/null 2>&1; then
    base_ref="HEAD~1"
  else
    # First commit case: build all modules.
    printf '%s\n' "${modules[@]}" | paste -sd ','
    exit 0
  fi
fi

if ! git rev-parse --verify "${base_ref}" >/dev/null 2>&1; then
  # Missing target branch in local refs (common in fresh Jenkins checkout).
  if [[ "${base_ref}" == origin/* ]]; then
    target_branch="${base_ref#origin/}"
    git fetch origin "${target_branch}:${target_branch}" >/dev/null 2>&1 || true
  fi
fi

changed_files="$(git diff --name-only "${base_ref}"...HEAD || true)"

# Lab 2 CD: GitOps/docs/spec/agent-only commits should not trigger full Maven
# test/build/image work. Jenkins will run lightweight validation only.
if [[ -n "${changed_files}" ]] \
  && ! grep -Ev '^(deploy/gitops/|docs/|\.agents/|\.specify/)' <<< "${changed_files}" | grep -q .; then
  printf '%s\n' "__skip_full_ci__"
  exit 0
fi

# Only Jenkinsfile and/or ci/* changed → CI-only; avoid rebuilding every module (Coverage Gate on all services).
if [[ -n "${changed_files}" ]] \
  && ! grep -Ev '^(Jenkinsfile|ci/)' <<< "${changed_files}" | grep -q .; then
  printf '%s\n' "common-library" | paste -sd ','
  exit 0
fi

# If shared build or root files change, rebuild all modules.
if [[ -z "${changed_files}" ]] \
  || grep -Eq '^(pom\.xml|\.github/|checkstyle/|common-library/|docker/|k8s/|scripts/)' <<< "${changed_files}"; then
  printf '%s\n' "${modules[@]}" | paste -sd ','
  exit 0
fi

selected_modules=()
for module in "${modules[@]}"; do
  if grep -Eq "^${module}/" <<<"${changed_files}"; then
    selected_modules+=("${module}")
  fi
done

if [[ ${#selected_modules[@]} -eq 0 ]]; then
  # Non-module change (e.g. docs): keep CI green but cheap by running one core module.
  selected_modules=("common-library")
fi

printf '%s\n' "${selected_modules[@]}" | paste -sd ','
