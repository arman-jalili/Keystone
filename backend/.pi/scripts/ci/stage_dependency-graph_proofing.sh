#!/usr/bin/env bash
# ============================================================================
# stage_dependency-graph_proofing.sh
#
# CI stage that runs contract and coverage checks for the dependency-graph
# module. This stage is always run (not conditional).
#
# Usage: bash stage_dependency-graph_proofing.sh
# Exit: 0 if all checks pass, 1 on any failure
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PASS=0
FAIL=0

log_pass() { echo "  ✓ PASS: $1"; ((PASS++)); }
log_fail() { echo "  ✗ FAIL: $1"; ((FAIL++)); }

echo ""
echo "═══════════════════════════════════════════════"
echo "  Stage 11: dependency-graph Proofing"
echo "═══════════════════════════════════════════════"
echo ""

# 1. Contract Implementation Check
echo "─── Check 1: Contract Implementation ───"
if bash "${SCRIPT_DIR}/check_dependency-graph_contracts.sh"; then
    log_pass "Contract implementation check"
else
    log_fail "Contract implementation check"
fi
echo ""

# 2. Coverage Threshold Check
echo "─── Check 2: Coverage Threshold ───"
if bash "${SCRIPT_DIR}/check_dependency-graph_coverage.sh"; then
    log_pass "Coverage threshold check"
else
    log_fail "Coverage threshold check"
fi
echo ""

echo "═══════════════════════════════════════════════"
echo "  Stage Results"
echo "═══════════════════════════════════════════════"
echo "  Passed: ${PASS}"
echo "  Failed: ${FAIL}"
echo ""

if [[ $FAIL -gt 0 ]]; then
    echo "  Stage FAILED."
    exit 1
fi

echo "  Stage PASSED."
exit 0
