#!/usr/bin/env bash
# ============================================================================
# stage_policy-engine_proofing.sh
#
# CI stage wrapper that runs all policy-engine proofing checks in sequence.
# Called by the hardening pipeline for automated PR validation.
#
# Usage: bash .pi/scripts/ci/stage_policy-engine_proofing.sh
# Exit: 0 = all checks pass, 1 = any check fails
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FAILED=0

echo "============================================"
echo "  Policy Engine Proofing Stage"
echo "============================================"
echo ""

run_check() {
    local name="$1"
    local script="$2"

    echo "--- ${name} ---"
    if bash "$script"; then
        echo ""
        return 0
    else
        echo ""
        FAILED=1
        return 1
    fi
}

run_check "Contract Implementation Check" "${SCRIPT_DIR}/check_policy-engine_contracts.sh"
run_check "Coverage Threshold Check" "${SCRIPT_DIR}/check_policy-engine_coverage.sh"

echo "============================================"
if [ $FAILED -eq 0 ]; then
    echo "✅ All policy-engine proofing checks passed."
else
    echo "❌ Some policy-engine proofing checks failed."
fi
exit $FAILED
