#!/usr/bin/env bash
# ============================================================================
# check_dependency-graph_contracts.sh
#
# Verifies that every public interface defined in the dependency-graph contract
# freeze has a concrete implementation class. This ensures no interface is left
# unimplemented.
#
# Usage: bash check_dependency-graph_contracts.sh
# Exit: 0 if all interfaces have implementations, 1 otherwise
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

PASS=0
FAIL=0
ERRORS=()

log_pass() { echo "  ✓ PASS: $1"; ((PASS++)) || true; }
log_fail() { echo "  ✗ FAIL: $1"; ERRORS+=("$1"); ((FAIL++)) || true; }

echo ""
echo "═══════════════════════════════════════════════"
echo "  Contract Implementation Check"
echo "  Module: dependency-graph"
echo "═══════════════════════════════════════════════"
echo ""

SRC_DIR="${PROJECT_DIR}/src/main/java/com/keystone/graph"

if [[ ! -d "$SRC_DIR" ]]; then
    echo "  ✗ Source directory not found: $SRC_DIR"
    exit 1
fi

# Define expected interface → implementation mappings
# Format: "InterfaceName:ConcreteImplName"
INTERFACE_MAP=(
    "ImpactAnalyzer:ImpactAnalyzerImpl"
    "DependencyParser:DependencyParserImpl"
    "GraphRepository:GraphRepositoryImpl"
    "GraphEventPublisher:GraphEventPublisherImpl"
    "GraphService:GraphServiceImpl"
)

echo "  Checking interface implementations..."
echo ""

for entry in "${INTERFACE_MAP[@]}"; do
    IFACE="${entry%%:*}"
    IMPL="${entry##*:}"

    # Find the interface file
    IFACE_FILE=$(find "$SRC_DIR" -name "${IFACE}.java" -type f 2>/dev/null | head -1)
    if [[ -z "$IFACE_FILE" ]]; then
        log_fail "Interface ${IFACE}.java not found in ${SRC_DIR}"
        continue
    fi

    # Find the implementation file
    IMPL_FILE=$(find "$SRC_DIR" -name "${IMPL}.java" -type f 2>/dev/null | head -1)
    if [[ -z "$IMPL_FILE" ]]; then
        log_fail "Implementation ${IMPL}.java not found for interface ${IFACE}"
        continue
    fi

    # Verify the implementation actually implements the interface
    if grep -q "implements ${IFACE}" "$IMPL_FILE" 2>/dev/null; then
        log_pass "${IFACE} → ${IMPL} (valid)"
    elif grep -q "implements ${IFACE}" "$(dirname "$IMPL_FILE")/${IMPL}.java" 2>/dev/null; then
        log_pass "${IFACE} → ${IMPL} (valid)"
    else
        log_fail "${IMPL}.java does not declare 'implements ${IFACE}'"
        continue
    fi
done

echo ""
echo "═══════════════════════════════════════════════"
echo "  Results"
echo "═══════════════════════════════════════════════"
echo "  Passed: ${PASS}"
echo "  Failed: ${FAIL}"
echo ""

if [[ $FAIL -gt 0 ]]; then
    for err in "${ERRORS[@]}"; do
        echo "  - $err"
    done
    echo ""
    exit 1
fi

echo "  All interfaces have valid implementations."
exit 0
