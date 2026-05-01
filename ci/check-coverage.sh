#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "Usage: $0 <module> <minimum_percent>" >&2
  exit 1
fi

module="$1"
minimum="$2"
report_path="${module}/target/site/jacoco/jacoco.xml"

if [[ ! -f "${report_path}" ]]; then
  echo "[WARN] Coverage report missing: ${report_path} — skipping coverage gate for ${module}"
  exit 0
fi

line_counter="$(awk -F'"' '/<counter type="LINE"/ {print $0; exit}' "${report_path}")"
covered="$(awk -F'"' '/<counter type="LINE"/ {for(i=1;i<=NF;i++){if($i=="covered"){print $(i+1); exit}}}' "${report_path}")"
missed="$(awk -F'"' '/<counter type="LINE"/ {for(i=1;i<=NF;i++){if($i=="missed"){print $(i+1); exit}}}' "${report_path}")"

if [[ -z "${line_counter}" || -z "${covered}" || -z "${missed}" ]]; then
  echo "[WARN] No LINE coverage data in ${report_path} — skipping coverage gate for ${module}"
  exit 0
fi

total=$((covered + missed))
if [[ "${total}" -eq 0 ]]; then
  echo "[WARN] No executable lines found for ${module} — skipping coverage gate"
  exit 0
fi

coverage_percent="$(awk -v c="${covered}" -v t="${total}" 'BEGIN { printf "%.2f", (c / t) * 100 }')"

echo "Module ${module} line coverage: ${coverage_percent}% (required >= ${minimum}%)"
if awk -v actual="${coverage_percent}" -v threshold="${minimum}" 'BEGIN { exit !(actual + 0 >= threshold + 0) }'; then
  exit 0
fi

echo "Coverage gate failed for ${module}: ${coverage_percent}% < ${minimum}%"
exit 1
