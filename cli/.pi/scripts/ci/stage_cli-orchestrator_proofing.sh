#!/usr/bin/env bash
# stage_cli-orchestrator_proofing.sh
#
# CI stage wrapper for cli-orchestrator proofing checks.
# Runs contract implementation validation and coverage checks.
#
# Usage: bash stage_cli-orchestrator_proofing.sh
#
# Exit codes:
#   0 = ALL PASS
#   1 = ONE OR MORE CHECKS FAILED

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STAGE_NAME="cli-orchestrator_proofing"
PASSED=0
FAILED=0

echo "=============================================="
echo "  Stage: ${STAGE_NAME}"
echo "=============================================="

run_check() {
    local name="$1"
    local script="$2"
    shift 2

    echo ""
    echo "--- [${name}] ---"
    if bash "${script}" "$@"; then
        echo "  ✅ ${name}: PASS"
        PASSED=$((PASSED + 1))
    else
        echo "  ❌ ${name}: FAIL"
        FAILED=$((FAILED + 1))
    fi
}

# Run contract implementation check
run_check "Contract Implementation Check" \
    "${SCRIPT_DIR}/check_cli-orchestrator_contracts.sh" \
    "--verbose"

# Run coverage threshold check
run_check "Coverage Threshold Check" \
    "${SCRIPT_DIR}/check_cli-orchestrator_coverage.sh" \
    "--verbose" "--threshold=80"

echo ""
echo "=============================================="
echo "  Stage Summary: ${STAGE_NAME}"
echo "    Passed: ${PASSED}"
echo "    Failed: ${FAILED}"
echo "=============================================="

if [[ ${FAILED} -gt 0 ]]; then
    exit 1
fi
exit 0
