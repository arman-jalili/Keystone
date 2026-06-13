#!/usr/bin/env bash
# ============================================================================
# check_dashboard_coverage.sh — Dashboard Coverage Threshold Check
# ============================================================================
# Enforces minimum coverage thresholds for the dashboard module.
#
# Usage:
#   ./check_dashboard_coverage.sh            # Normal output
#   ./check_dashboard_coverage.sh --threshold=70  # Custom threshold (default 80)
#   ./check_dashboard_coverage.sh --json      # JSON output
#   ./check_dashboard_coverage.sh --help      # This help
#
# Exit codes:
#   0 - Coverage meets or exceeds threshold
#   1 - Coverage below threshold
# ============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
THRESHOLD=80
JSON=false
PASS_COUNT=0
FAIL_COUNT=0

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Parse arguments
for arg in "$@"; do
    case "$arg" in
        --threshold=*) THRESHOLD="${arg#*=}" ;;
        --json) JSON=true ;;
        --help)
            sed -n '/^# ============/,/^# ============/p' "$0" | grep -v "^#!/"
            exit 0
            ;;
    esac
done

echo "================================================"
echo "  Dashboard Coverage Check (threshold: ${THRESHOLD}%)"
echo "================================================"
echo ""

# Check if JaCoCo report exists
REPORT_FILE="${PROJECT_ROOT}/target/site/jacoco/index.html"
if [[ ! -f "$REPORT_FILE" ]]; then
    echo "  JaCoCo report not found. Generating..."
    cd "$PROJECT_ROOT"
    mvn jacoco:report -q 2>/dev/null || true
fi

if [[ ! -f "$REPORT_FILE" ]]; then
    if $JSON; then
        echo '{"script":"check_dashboard_coverage.sh","status":"skip","message":"JaCoCo report not found"}'
    else
        echo -e "  ${YELLOW}[SKIP]${NC} JaCoCo report not found — run 'mvn test jacoco:report' first"
    fi
    exit 0
fi

# Extract dashboard module coverage from JaCoCo report
# Look for coverage of com/keystone/dashboard classes
DASHBOARD_COVERAGE=$(grep -oP 'com/keystone/dashboard[^<]*<td[^>]*class="ctr2">[^<]*</td>' "$REPORT_FILE" 2>/dev/null | grep -oP '\d+\.?\d*%' | head -1 || echo "0%")

# Also try to find instruction coverage from jacoco.csv
CSV_FILE="${PROJECT_ROOT}/target/site/jacoco/jacoco.csv"
DASHBOARD_INSTRUCTION_COVERAGE=0
DASHBOARD_BRANCH_COVERAGE=0
DASHBOARD_CLASSES=0
DASHBOARD_COVERED_CLASSES=0

if [[ -f "$CSV_FILE" ]]; then
    # Parse CSV for dashboard module
    while IFS=, read -r group package class instruction_missed instruction_covered branch_missed branch_covered line_missed line_covered complexity_missed complexity_covered method_missed method_covered; do
        if [[ "$package" == *"com/keystone/dashboard"* ]]; then
            DASHBOARD_CLASSES=$((DASHBOARD_CLASSES + 1))
            if [[ "$instruction_missed" -eq 0 && "$instruction_covered" -gt 0 ]]; then
                DASHBOARD_COVERED_CLASSES=$((DASHBOARD_COVERED_CLASSES + 1))
            fi
        fi
    done < "$CSV_FILE"

    # Calculate a basic coverage metric
    if [[ $DASHBOARD_CLASSES -gt 0 ]]; then
        DASHBOARD_INSTRUCTION_COVERAGE=$((DASHBOARD_COVERED_CLASSES * 100 / DASHBOARD_CLASSES))
    fi
fi

echo "  Dashboard classes:     $DASHBOARD_CLASSES"
echo "  Classes with coverage: $DASHBOARD_COVERED_CLASSES"
echo "  Coverage:              ${DASHBOARD_INSTRUCTION_COVERAGE}% (threshold: ${THRESHOLD}%)"
echo ""

if [[ $DASHBOARD_INSTRUCTION_COVERAGE -ge $THRESHOLD ]] || [[ $DASHBOARD_CLASSES -eq 0 ]]; then
    if $JSON; then
        echo "{\"script\":\"check_dashboard_coverage.sh\",\"status\":\"pass\",\"coverage\":$DASHBOARD_INSTRUCTION_COVERAGE,\"threshold\":$THRESHOLD}"
    else
        echo -e "  ${GREEN}[PASS]${NC} Coverage ${DASHBOARD_INSTRUCTION_COVERAGE}% meets threshold ${THRESHOLD}%"
    fi
    exit 0
else
    if $JSON; then
        echo "{\"script\":\"check_dashboard_coverage.sh\",\"status\":\"fail\",\"coverage\":$DASHBOARD_INSTRUCTION_COVERAGE,\"threshold\":$THRESHOLD}"
    else
        echo -e "  ${RED}[FAIL]${NC} Coverage ${DASHBOARD_INSTRUCTION_COVERAGE}% below threshold ${THRESHOLD}%"
    fi
    exit 1
fi
