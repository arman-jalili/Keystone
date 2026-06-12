#!/usr/bin/env bash
# check_cli-orchestrator_coverage.sh
#
# Enforces minimum test coverage thresholds per package for the
# cli-orchestrator module. Uses go test -cover to measure coverage.
#
# Exit codes:
#   0 = PASS — all packages meet or exceed coverage thresholds
#   1 = FAIL — one or more packages below threshold
#
# Usage: bash check_cli-orchestrator_coverage.sh [--verbose] [--threshold=N]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
VERBOSE=false
MIN_COVERAGE=80

while [[ $# -gt 0 ]]; do
    case $1 in
        --verbose) VERBOSE=true; shift ;;
        --threshold=*) MIN_COVERAGE="${1#*=}"; shift ;;
        --threshold) MIN_COVERAGE="$2"; shift 2 ;;
        *) shift ;;
    esac
done

cd "${PROJECT_DIR}"

# Packages to check with their minimum coverage thresholds
# Format: pkg_path:min_coverage
PACKAGES=(
    "github.com/arman-jalili/keystone-cli/internal/infrastructure/parser:80"
    "github.com/arman-jalili/keystone-cli/internal/infrastructure/cache:75"
    "github.com/arman-jalili/keystone-cli/internal/infrastructure/diff:80"
    "github.com/arman-jalili/keystone-cli/internal/infrastructure/uploader:75"
)

errors=0
total=0

echo "=============================================="
echo "  Coverage Threshold Check"
echo "=============================================="

for entry in "${PACKAGES[@]}"; do
    pkg="${entry%%:*}"
    threshold="${entry#*:}"
    total=$((total + 1))

    # Run tests and capture coverage output
    coverage_output=$(go test -cover -count=1 "${pkg}" 2>&1 || true)

    # Extract coverage percentage
    coverage=""
    if [[ "${coverage_output}" =~ coverage:\ ([0-9.]+)% ]]; then
        coverage="${BASH_REMATCH[1]}"
    else
        coverage="0"
    fi

    coverage_int="${coverage%.*}"
    if [[ -z "${coverage_int}" ]]; then
        coverage_int=0
    fi

    if [[ "${coverage_int}" -ge "${threshold}" ]]; then
        if $VERBOSE || true; then
            echo "  ✅ ${pkg}: ${coverage}% >= ${threshold}%"
        fi
    else
        echo "  ❌ ${pkg}: ${coverage}% < ${threshold}% (minimum ${threshold}%)"
        errors=$((errors + 1))
    fi
done

echo "----------------------------------------------"
if [[ ${errors} -eq 0 ]]; then
    echo "  ✅ PASS: ${total}/${total} packages meet coverage thresholds"
    echo ""
    exit 0
else
    echo "  ❌ FAIL: ${errors}/${total} packages below coverage threshold"
    echo ""
    exit 1
fi
