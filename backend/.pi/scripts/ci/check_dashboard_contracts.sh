#!/usr/bin/env bash
# ============================================================================
# check_dashboard_contracts.sh — Dashboard Contract Implementation Check
# ============================================================================
# Validates that every interface defined in the dashboard contract freeze
# has a corresponding concrete implementation class.
#
# Usage:
#   ./check_dashboard_contracts.sh          # Normal output
#   ./check_dashboard_contracts.sh --json   # JSON output for CI
#   ./check_dashboard_contracts.sh --help   # This help
#
# Exit codes:
#   0 - All interfaces have implementations
#   1 - One or more interfaces lack implementations
# ============================================================================

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
DASHBOARD_DIR="${PROJECT_ROOT}/src/main/java/com/keystone/dashboard"
PASS_COUNT=0
FAIL_COUNT=0
ERRORS=()

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Help
if [[ "${1:-}" == "--help" ]]; then
    sed -n '/^# ============/,/^# ============/p' "$0" | grep -v "^#!/"
    exit 0
fi

# JSON mode
JSON=false
if [[ "${1:-}" == "--json" ]]; then
    JSON=true
fi

echo "================================================"
echo "  Dashboard Contract Implementation Check"
echo "================================================"
echo ""

# Define interface-to-implementation mappings (using indexed arrays)
INTERFACES=(
    "DashboardQueryService:DashboardQueryServiceImpl"
    "HealthScoreService:HealthScoreServiceImpl"
    "PolicyUiService:PolicyUiServiceImpl"
    "HealthScoreCalculator:HealthScoreCalculatorImpl"
    "AuditLogService:AuditLogServiceImpl"
    "DashboardEventPublisher:"
    "HealthScoreRepository:"
    "DashboardMetricsRepository:"
)

RESULTS=()

for mapping in "${INTERFACES[@]}"; do
    INTERFACE="${mapping%%:*}"
    IMPL="${mapping##*:}"

    # Find the interface file
    IFACE_FILE=$(find "$DASHBOARD_DIR" -name "${INTERFACE}.java" -type f 2>/dev/null | head -1)

    if [[ -z "$IFACE_FILE" ]]; then
        if $JSON; then
            RESULTS+=("{\"interface\":\"$INTERFACE\",\"status\":\"skip\",\"message\":\"Interface file not found\"}")
        else
            echo -e "  ${YELLOW}[SKIP]${NC} $INTERFACE — file not found (may be external interface)"
        fi
        continue
    fi

    # Check if it's actually an interface
    if ! grep -q "^public interface ${INTERFACE}\b\|^interface ${INTERFACE}\b" "$IFACE_FILE" 2>/dev/null; then
        if $JSON; then
            RESULTS+=("{\"interface\":\"$INTERFACE\",\"status\":\"skip\",\"message\":\"Not an interface\"}")
        else
            echo -e "  ${YELLOW}[SKIP]${NC} $INTERFACE — not an interface"
        fi
        continue
    fi

    # If no implementation expected
    if [[ -z "$IMPL" ]]; then
        if $JSON; then
            RESULTS+=("{\"interface\":\"$INTERFACE\",\"status\":\"pass\",\"message\":\"Interface-only contract (no impl expected)\"}")
        else
            echo -e "  ${GREEN}[PASS]${NC} $INTERFACE — interface-only contract"
        fi
        PASS_COUNT=$((PASS_COUNT + 1))
        continue
    fi

    # Find the implementation file
    IMPL_FILE=$(find "$DASHBOARD_DIR" -name "${IMPL}.java" -type f 2>/dev/null | head -1)

    if [[ -z "$IMPL_FILE" ]]; then
        if $JSON; then
            RESULTS+=("{\"interface\":\"$INTERFACE\",\"status\":\"fail\",\"message\":\"Implementation $IMPL not found\"}")
        else
            echo -e "  ${RED}[FAIL]${NC} $INTERFACE → Implementation $IMPL not found"
        fi
        FAIL_COUNT=$((FAIL_COUNT + 1))
        ERRORS+=("$INTERFACE → $IMPL not found")
        continue
    fi

    # Check that the implementation actually implements the interface
    if grep -q "implements ${INTERFACE}" "$IMPL_FILE" 2>/dev/null; then
        if $JSON; then
            RESULTS+=("{\"interface\":\"$INTERFACE\",\"status\":\"pass\",\"message\":\"Implemented by $IMPL\"}")
        else
            echo -e "  ${GREEN}[PASS]${NC} $INTERFACE → $IMPL"
        fi
        PASS_COUNT=$((PASS_COUNT + 1))
    else
        if $JSON; then
            RESULTS+=("{\"interface\":\"$INTERFACE\",\"status\":\"fail\",\"message\":\"$IMPL does not declare 'implements $INTERFACE'\"}")
        else
            echo -e "  ${RED}[FAIL]${NC} $INTERFACE → $IMPL does not implement the interface"
        fi
        FAIL_COUNT=$((FAIL_COUNT + 1))
        ERRORS+=("$INTERFACE → $IMPL not implemented")
    fi
done

# Summary
echo ""
echo "================================================"
echo "  Summary"
echo "================================================"
if $JSON; then
    RESULTS_JSON=""
    for r in "${RESULTS[@]}"; do
        if [[ -n "$RESULTS_JSON" ]]; then
            RESULTS_JSON="${RESULTS_JSON},"
        fi
        RESULTS_JSON="${RESULTS_JSON}${r}"
    done
    cat << EOF
{
  "script": "check_dashboard_contracts.sh",
  "passed": $PASS_COUNT,
  "failed": $FAIL_COUNT,
  "results": [$RESULTS_JSON]
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
        echo -e "${RED}Contract check FAILED.${NC}"
        exit 1
    fi
    echo -e "${GREEN}All contracts verified.${NC}"
fi
exit 0
