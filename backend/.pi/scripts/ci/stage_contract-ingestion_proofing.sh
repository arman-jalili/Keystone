#!/usr/bin/env bash
# ============================================================================
# stage_contract-ingestion_proofing.sh
#
# CI stage that runs contract and coverage checks for the contract-ingestion
# module. This stage is always run (not conditional).
#
# Usage: bash stage_contract-ingestion_proofing.sh
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
echo "  Stage 11: contract-ingestion Proofing"
echo "═══════════════════════════════════════════════"
echo ""

# 1. Contract Implementation Check
echo "─── Check 1: Contract Implementation ───"
if bash "${SCRIPT_DIR}/check_contract-ingestion_contracts.sh"; then
    log_pass "Contract implementation check"
else
    log_fail "Contract implementation check"
fi
echo ""

# 2. Coverage Threshold Check
echo "─── Check 2: Coverage Threshold ───"
if bash "${SCRIPT_DIR}/check_contract-ingestion_coverage.sh"; then
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
