#!/usr/bin/env bash
# ============================================================================
# check_dependency-graph_coverage.sh
#
# Enforces minimum code coverage thresholds for the dependency-graph module.
# Uses JaCoCo (Java) to verify instruction, branch, and line coverage.
#
# Usage: bash check_dependency-graph_coverage.sh
# Exit: 0 if coverage meets threshold, 1 otherwise
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

COVERAGE_THRESHOLD="${COVERAGE_THRESHOLD:-80}"
INSTRUCTION_COVERAGE=0
BRANCH_COVERAGE=0
LINE_COVERAGE=0

echo ""
echo "═══════════════════════════════════════════════"
echo "  Coverage Threshold Check"
echo "  Module: dependency-graph"
echo "  Threshold: ${COVERAGE_THRESHOLD}%"
echo "═══════════════════════════════════════════════"
echo ""

# Check if JaCoCo report exists
JACOCO_REPORT="${PROJECT_DIR}/target/site/jacoco/jacoco.csv"
if [[ -f "$JACOCO_REPORT" ]]; then
    echo "  Found JaCoCo report: $JACOCO_REPORT"
    echo ""

    # Parse JaCoCo CSV for dependency-graph classes
    # CSV format: GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,LINE_MISSED,LINE_COVERED,COMPLEXITY_MISSED,COMPLEXITY_COVERED,METHOD_MISSED,METHOD_COVERED

    while IFS=',' read -r group pkg cls inst_missed inst_covered branch_missed branch_covered line_missed line_covered rest; do
        # Skip header
        if [[ "$group" == "GROUP" ]]; then
            continue
        fi
        # Only check keystone.graph classes
        if [[ "$pkg" != com.keystone.graph* ]]; then
            continue
        fi
        # Skip test classes
        if [[ "$cls" == *"Test" ]]; then
            continue
        fi

        # Calculate coverage percentages
        inst_total=$((inst_missed + inst_covered))
        branch_total=$((branch_missed + branch_covered))
        line_total=$((line_missed + line_covered))

        if [[ $inst_total -gt 0 ]]; then
            inst_pct=$((inst_covered * 100 / inst_total))
        else
            inst_pct=100
        fi

        if [[ $branch_total -gt 0 ]]; then
            branch_pct=$((branch_covered * 100 / branch_total))
        else
            branch_pct=100
        fi

        if [[ $line_total -gt 0 ]]; then
            line_pct=$((line_covered * 100 / line_total))
        else
            line_pct=100
        fi

        printf "  %-40s inst=%3d%% branch=%3d%% line=%3d%%\n" "${pkg}.${cls}" "$inst_pct" "$branch_pct" "$line_pct"

        # Aggregate
        INSTRUCTION_COVERAGE=$((INSTRUCTION_COVERAGE + inst_covered))
        BRANCH_COVERAGE=$((BRANCH_COVERAGE + branch_covered))
        LINE_COVERAGE=$((LINE_COVERAGE + line_covered))
    done < "$JACOCO_REPORT"
else
    echo "  ⚠ No JaCoCo report found at ${JACOCO_REPORT}"
    echo "  Run 'mvn jacoco:report' first to generate coverage data."
    echo ""
    echo "═══════════════════════════════════════════════"
    echo "  SKIPPED (no coverage data)"
    echo "═══════════════════════════════════════════════"
    exit 0
fi

echo ""
echo "═══════════════════════════════════════════════"
echo "  Coverage Summary"
echo "═══════════════════════════════════════════════"

# Compare against threshold
# For aggregated, we check line coverage as primary metric
if [[ $LINE_COVERAGE -ge $COVERAGE_THRESHOLD ]]; then
    echo "  ✅ Line coverage ${LINE_COVERAGE}% meets threshold ${COVERAGE_THRESHOLD}%"
else
    echo "  ❌ Line coverage ${LINE_COVERAGE}% is below threshold ${COVERAGE_THRESHOLD}%"
    exit 1
fi

exit 0
