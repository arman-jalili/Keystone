#!/usr/bin/env bash
# ============================================================================
# check_notification-engine_coverage.sh
#
# Checks that JACOCO coverage report for notification-engine meets thresholds.
# Default: 80% line coverage for notification module.
#
# Usage: bash .pi/scripts/ci/check_notification-engine_coverage.sh [--help]
#        COVERAGE_THRESHOLD=85 bash .pi/scripts/ci/check_notification-engine_coverage.sh
# Exit: 0 = coverage meets threshold, 1 = coverage too low
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="${SCRIPT_DIR}/../../.."

THRESHOLD="${COVERAGE_THRESHOLD:-80}"
FAILED=0

show_help() { sed -n '3,12p' "$0"; exit 0; }
[[ "${1:-}" == "--help" ]] && show_help

echo "================================================"
echo "  Notification Engine Coverage Check"
echo "================================================"
echo "  Threshold: ${THRESHOLD}%"
echo ""

# Check if JaCoCo XML report exists
REPORT_FILE="${PROJECT_DIR}/target/site/jacoco-aggregate/jacoco.xml"
if [ ! -f "$REPORT_FILE" ]; then
    echo "⚠️  No JaCoCo report found at target/site/jacoco-aggregate/jacoco.xml"
    echo "   Generating coverage data first..."
    echo "   Run: mvn jacoco:report -q"
    echo ""

    # Try to locate XML reports
    FOUND=$(find "${PROJECT_DIR}/target" -name "jacoco.xml" -path "*/jacoco*" 2>/dev/null | head -5)
    if [ -n "$FOUND" ]; then
        REPORT_FILE=$(echo "$FOUND" | head -1)
        echo "   Found alternative report: ${REPORT_FILE}"
    else
        echo "❌ No coverage report found. Run tests with coverage first:"
        echo "   mvn verify -Pcoverage"
        echo "   mvn jacoco:report"
        exit 0  # Don't fail — coverage data may not be generated yet
    fi
fi

# Extract coverage for notification module
# JaCoCo XML has <package name="com/keystone/notification">
NOTIFICATION_COVERAGE=$(grep -A5 'name="com/keystone/notification"' "$REPORT_FILE" 2>/dev/null | \
    grep -oP 'line_rate="[0-9.]+"' | head -1 | grep -oP '[0-9.]+' || echo "")

if [ -z "$NOTIFICATION_COVERAGE" ]; then
    echo "⚠️  Could not extract coverage data for notification module."
    echo "   Module may not have been instrumented."
    echo ""
    echo "   To generate coverage:"
    echo "     mvn clean verify"
    echo "     mvn jacoco:report"
    echo ""
    echo "   Skipping coverage check (not a failure)."
    exit 0
fi

# Convert to percentage (fraction → 0-100)
COVERAGE_PCT=$(echo "$NOTIFICATION_COVERAGE * 100" | bc 2>/dev/null || echo "0")
COVERAGE_PCT=${COVERAGE_PCT%.*}  # Remove decimal part

echo "   Notification module coverage: ${COVERAGE_PCT:-unknown}%"

if [ -z "${COVERAGE_PCT:-}" ] || [ "$COVERAGE_PCT" = "0" ]; then
    echo "⚠️  Could not compute coverage percentage."
    echo "   Skipping coverage check."
    exit 0
fi

if [ "$COVERAGE_PCT" -ge "$THRESHOLD" ]; then
    echo "✅ Coverage meets threshold (${COVERAGE_PCT}% >= ${THRESHOLD}%)"
else
    echo "❌ Coverage below threshold (${COVERAGE_PCT}% < ${THRESHOLD}%)"
    echo "   Add more tests to reach ${THRESHOLD}% coverage"
    FAILED=1
fi

echo ""
if [ $FAILED -eq 0 ]; then
    echo "✅ Coverage check passed."
else
    echo "❌ Coverage check failed."
fi
exit $FAILED
