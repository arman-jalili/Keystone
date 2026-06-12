#!/usr/bin/env bash
# ============================================================================
# check_contract-ingestion_coverage.sh
#
# Enforces minimum code coverage thresholds for the contract-ingestion module.
# Uses JaCoCo (Java) or generic coverage tool to verify coverage.
#
# Usage: bash check_contract-ingestion_coverage.sh
# Exit: 0 if coverage meets threshold, 1 otherwise
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# Go up from .pi/scripts/ci/ to the backend directory
PROJECT_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

COVERAGE_THRESHOLD="${COVERAGE_THRESHOLD:-80}"
INSTRUCTION_COVERAGE=0
BRANCH_COVERAGE=0
LINE_COVERAGE=0

echo ""
echo "═══════════════════════════════════════════════"
echo "  Coverage Threshold Check"
echo "  Module: contract-ingestion"
echo "  Threshold: ${COVERAGE_THRESHOLD}%"
echo "═══════════════════════════════════════════════"
echo ""

# Check if JaCoCo report exists
JACOCO_REPORT="${PROJECT_DIR}/target/site/jacoco/jacoco.csv"
if [[ -f "$JACOCO_REPORT" ]]; then
    echo "  Found JaCoCo report: $JACOCO_REPORT"
    echo ""

    # Parse JaCoCo CSV for contract-ingestion classes
    # CSV format: GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,LINE_MISSED,LINE_COVERED,COMPLEXITY_MISSED,COMPLEXITY_COVERED,METHOD_MISSED,METHOD_COVERED

    while IFS=',' read -r group pkg cls inst_missed inst_covered branch_missed branch_covered line_missed line_covered rest; do
        # Skip header
        if [[ "$group" == "GROUP" ]]; then
            continue
        fi
        # Only check keystone.ingestion classes
        if [[ "$pkg" != com.keystone.ingestion* ]]; then
            continue
        fi
        # Skip test classes
        if [[ "$cls" == *"Test" ]]; then
            continue
        fi

        total_inst=$((inst_missed + inst_covered))
        total_branch=$((branch_missed + branch_covered))
        total_line=$((line_missed + line_covered))

        if [[ $total_inst -gt 0 ]]; then
            pct=$((inst_covered * 100 / total_inst))
            if [[ $pct -lt $COVERAGE_THRESHOLD ]]; then
                echo "  ✗ FAIL: ${pkg}.${cls} — ${pct}% instruction coverage (threshold: ${COVERAGE_THRESHOLD}%)"
                INSTRUCTION_COVERAGE=$((INSTRUCTION_COVERAGE + 1))
            fi
        fi
    done < "$JACOCO_REPORT"

    # Overall module coverage
    echo ""
    echo "  Checking overall module coverage..."

    MODULE_INST_COVERED=$(awk -F',' '$2 ~ /^com\.keystone\.ingestion/ && NR>1 {i+=$5} END {print i}' "$JACOCO_REPORT")
    MODULE_INST_MISSED=$(awk -F',' '$2 ~ /^com\.keystone\.ingestion/ && NR>1 {i+=$4} END {print i}' "$JACOCO_REPORT")
    MODULE_LINE_COVERED=$(awk -F',' '$2 ~ /^com\.keystone\.ingestion/ && NR>1 {i+=$9} END {print i}' "$JACOCO_REPORT")
    MODULE_LINE_MISSED=$(awk -F',' '$2 ~ /^com\.keystone\.ingestion/ && NR>1 {i+=$8} END {print i}' "$JACOCO_REPORT")

    MODULE_TOTAL_INST=$((MODULE_INST_COVERED + MODULE_INST_MISSED))
    MODULE_TOTAL_LINE=$((MODULE_LINE_COVERED + MODULE_LINE_MISSED))

    if [[ $MODULE_TOTAL_INST -gt 0 ]]; then
        MODULE_INST_PCT=$((MODULE_INST_COVERED * 100 / MODULE_TOTAL_INST))
    else
        MODULE_INST_PCT=0
    fi

    if [[ $MODULE_TOTAL_LINE -gt 0 ]]; then
        MODULE_LINE_PCT=$((MODULE_LINE_COVERED * 100 / MODULE_TOTAL_LINE))
    else
        MODULE_LINE_PCT=0
    fi

    echo "  Module instruction coverage: ${MODULE_INST_PCT}%"
    echo "  Module line coverage: ${MODULE_LINE_PCT}%"

    if [[ $MODULE_INST_PCT -ge $COVERAGE_THRESHOLD ]]; then
        echo "  ✓ PASS: Instruction coverage meets threshold"
    else
        echo "  ✗ FAIL: Instruction coverage (${MODULE_INST_PCT}%) below threshold (${COVERAGE_THRESHOLD}%)"
        INSTRUCTION_COVERAGE=$((INSTRUCTION_COVERAGE + 1))
    fi

    if [[ $MODULE_LINE_PCT -ge $COVERAGE_THRESHOLD ]]; then
        echo "  ✓ PASS: Line coverage meets threshold"
    else
        echo "  ✗ FAIL: Line coverage (${MODULE_LINE_PCT}%) below threshold (${COVERAGE_THRESHOLD}%)"
        INSTRUCTION_COVERAGE=$((INSTRUCTION_COVERAGE + 1))
    fi
elif command -v mvn &>/dev/null && [[ -f "${PROJECT_DIR}/pom.xml" ]]; then
    echo "  JaCoCo report not found. Running Maven tests with coverage..."
    echo ""

    cd "$PROJECT_DIR"
    if mvn verify -q 2>/dev/null; then
        echo "  ✓ PASS: Tests passed with coverage"
        log_pass=true
    else
        echo "  ✗ FAIL: Tests or coverage check failed"
        INSTRUCTION_COVERAGE=$((INSTRUCTION_COVERAGE + 1))
    fi
else
    echo "  JaCoCo report not found and Maven not available."
    echo "  Run 'mvn clean verify' first to generate coverage data."
    echo "  Skipping coverage check."
fi

echo ""
echo "═══════════════════════════════════════════════"
echo "  Results"
echo "═══════════════════════════════════════════════"
echo "  Coverage failures: ${INSTRUCTION_COVERAGE}"
echo ""

if [[ $INSTRUCTION_COVERAGE -gt 0 ]]; then
    exit 1
fi

echo "  Coverage thresholds met."
exit 0
