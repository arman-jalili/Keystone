#!/usr/bin/env bash
# ============================================================================
# stage_dashboard_proofing.sh — Dashboard Proofing CI Stage
# ============================================================================
# Wrapper stage that runs all dashboard proofing checks.
#
# Usage:
#   ./stage_dashboard_proofing.sh          # Run all checks
#   ./stage_dashboard_proofing.sh --json   # JSON output
#   ./stage_dashboard_proofing.sh --help   # This help
#
# Exit codes:
#   0 - All checks pass
#   1 - One or more checks fail
# ============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PASS_COUNT=0
FAIL_COUNT=0
ERRORS=()

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

JSON=false
if [[ "${1:-}" == "--json" ]]; then
    JSON=true
elif [[ "${1:-}" == "--help" ]]; then
    sed -n '/^# ============/,/^# ============/p' "$0" | grep -v "^#!/"
    exit 0
fi

echo "================================================"
echo "  Stage: dashboard_proofing"
echo "================================================"
echo ""

# Run contract check
echo "--- Contract Implementation Check ---"
if bash "${SCRIPT_DIR}/check_dashboard_contracts.sh" 2>&1; then
    PASS_COUNT=$((PASS_COUNT + 1))
else
    FAIL_COUNT=$((FAIL_COUNT + 1))
    ERRORS+=("check_dashboard_contracts.sh failed")
fi

echo ""

# Run coverage check
echo "--- Coverage Threshold Check ---"
if bash "${SCRIPT_DIR}/check_dashboard_coverage.sh" 2>&1; then
    PASS_COUNT=$((PASS_COUNT + 1))
else
    FAIL_COUNT=$((FAIL_COUNT + 1))
    ERRORS+=("check_dashboard_coverage.sh failed")
fi

echo ""

# Summary
echo "================================================"
echo "  Stage Summary"
echo "================================================"
if $JSON; then
    cat << EOF
{
  "stage": "dashboard_proofing",
  "passed": $PASS_COUNT,
  "failed": $FAIL_COUNT,
  "status": "$([ $FAIL_COUNT -eq 0 ] && echo "pass" || echo "fail")"
}
EOF
else
    echo -e "  Passed: ${GREEN}$PASS_COUNT${NC}"
    echo -e "  Failed: ${RED}$FAIL_COUNT${NC}"
    echo ""
    if [[ $FAIL_COUNT -gt 0 ]]; then
        echo "FAILURES:"
        for err in "${ERRORS[@]}"; do
            echo "  - $err"
        done
        echo ""
        echo -e "${RED}Stage dashboard_proofing FAILED.${NC}"
        exit 1
    fi
    echo -e "${GREEN}Stage dashboard_proofing passed.${NC}"
fi
exit 0
