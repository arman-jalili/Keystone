#!/usr/bin/env bash
# ============================================================================
# stage_notification-engine_proofing.sh
#
# CI stage that runs all notification-engine proofing checks:
#   1. Contract implementation validation
#   2. Coverage threshold enforcement
#
# Usage: bash .pi/scripts/ci/stage_notification-engine_proofing.sh [--verbose]
# Exit: 0 = all checks pass, 1 = any check fails
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
VERBOSE=false
[[ "${1:-}" == "--verbose" ]] && VERBOSE=true

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

PASS_COUNT=0
FAIL_COUNT=0
ERRORS=()

run_check() {
    local name="$1"
    local script="$2"
    echo -e "${CYAN}[CHECK]${NC} ${name}..."

    local output
    if output=$(bash "$script" 2>&1); then
        echo -e "  ${GREEN}✓ PASS${NC}"
        PASS_COUNT=$((PASS_COUNT + 1))
        if $VERBOSE; then echo "$output" | sed 's/^/    /'; fi
    else
        echo -e "  ${RED}✗ FAIL${NC}"
        FAIL_COUNT=$((FAIL_COUNT + 1))
        ERRORS+=("${name}: $output")
        echo "$output" | sed 's/^/    /'
    fi
    echo ""
}

echo "═══════════════════════════════════════════════"
echo "  Notification Engine Proofing Stage"
echo "═══════════════════════════════════════════════"
echo ""

run_check "Contract Implementation Check" \
    "${SCRIPT_DIR}/check_notification-engine_contracts.sh"

run_check "Coverage Threshold Check" \
    "${SCRIPT_DIR}/check_notification-engine_coverage.sh"

echo "═══════════════════════════════════════════════"
echo "  Results: ${PASS_COUNT} passed, ${FAIL_COUNT} failed"
echo "═══════════════════════════════════════════════"

if [ ${#ERRORS[@]} -gt 0 ]; then
    echo ""
    echo "Failures:"
    for err in "${ERRORS[@]}"; do
        echo "  - $err"
    done
    exit 1
fi

echo -e "${GREEN}✅ All notification-engine proofing checks passed.${NC}"
exit 0
