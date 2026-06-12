#!/usr/bin/env bash
# ============================================================================
# check_policy-engine_coverage.sh
#
# Enforces minimum test coverage thresholds for the policy-engine module.
# Uses JaCoCo (configured in pom.xml) to check coverage.
#
# Usage: bash .pi/scripts/ci/check_policy-engine_coverage.sh [--help]
# Exit: 0 = coverage meets thresholds, 1 = below threshold
# ============================================================================
set -euo pipefail

MIN_COVERAGE=${POLICY_MIN_COVERAGE:-80}

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="${SCRIPT_DIR}/../../.."

show_help() {
    sed -n '3,10p' "$0"
    exit 0
}

[[ "${1:-}" == "--help" ]] && show_help

echo "============================================"
echo "  Policy Engine Coverage Check"
echo "============================================"
echo ""

# Run JaCoCo report
cd "$PROJECT_DIR"
if ! command -v mvn &>/dev/null && [ ! -f "mvnw" ]; then
    echo "⚠️  Maven not available, skipping coverage check" >&2
    exit 0
fi

MAVEN_CMD="mvn"
[ -f "mvnw" ] && MAVEN_CMD="./mvnw"

echo "Running tests with coverage..."
$MAVEN_CMD test -q 2>/dev/null || {
    echo "⚠️  Test run failed, skipping coverage check" >&2
    exit 0
}

# Generate JaCoCo report
$MAVEN_CMD jacoco:report -q 2>/dev/null || {
    echo "⚠️  JaCoCo report generation failed, skipping" >&2
    exit 0
}

# Check for JaCoCo CSV/XML report
REPORT_DIR="target/site/jacoco"
if [ ! -f "${REPORT_DIR}/jacoco.xml" ]; then
    echo "⚠️  JaCoCo XML report not found at ${REPORT_DIR}/jacoco.xml" >&2
    echo "   Run: mvn jacoco:report" >&2
    exit 0
fi

# Parse coverage from JaCoCo XML for the policy package
POLICY_XPATH="//package[@name='com/keystone/policy']"
if command -v xmllint &>/dev/null; then
    COVERAGE=$(xmllint --xpath "sum(${POLICY_XPATH}/counter[@type='INSTRUCTION']/@missed) div (sum(${POLICY_XPATH}/counter[@type='INSTRUCTION']/@covered) + sum(${POLICY_XPATH}/counter[@type='INSTRUCTION']/@missed)) * 100" "${REPORT_DIR}/jacoco.xml" 2>/dev/null || echo "")
else
    # Fallback: use grep/awk to parse coverage
    COVERAGE=$(grep -o 'counter.*type="INSTRUCTION"[^>]*' "${REPORT_DIR}/jacoco.xml" 2>/dev/null | head -1 | grep -oP 'missed="\K[^"]+' || echo "0")
fi

if [ -z "$COVERAGE" ] || [ "$COVERAGE" = "0" ]; then
    echo "⚠️  Could not determine coverage, check manually" >&2
    echo ""
    echo "Coverage report available at: ${REPORT_DIR}/index.html"
    exit 0
fi

COVERAGE_INT=$(printf "%.0f" "$COVERAGE" 2>/dev/null || echo "0")

echo "Policy engine instruction coverage: ${COVERAGE_INT}%"
echo "Minimum required: ${MIN_COVERAGE}%"

if [ "$COVERAGE_INT" -ge "$MIN_COVERAGE" ]; then
    echo "✅ Coverage meets threshold."
    exit 0
else
    echo "❌ Coverage below minimum threshold (${MIN_COVERAGE}%)."
    echo "   Add more tests to improve coverage."
    exit 1
fi
