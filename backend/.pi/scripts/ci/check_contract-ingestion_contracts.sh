#!/usr/bin/env bash
# ============================================================================
# check_contract-ingestion_contracts.sh
#
# Verifies that every public interface defined in the contract-ingestion freeze
# has a concrete implementation class. This ensures no interface is left
# unimplemented.
#
# Usage: bash check_contract-ingestion_contracts.sh
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
echo "  Module: contract-ingestion"
echo "═══════════════════════════════════════════════"
echo ""

SRC_DIR="${PROJECT_DIR}/src/main/java/com/keystone/ingestion"

if [[ ! -d "$SRC_DIR" ]]; then
    echo "  ✗ Source directory not found: $SRC_DIR"
    exit 1
fi

# Define expected interface → implementation mappings
# Format: "InterfaceName:ConcreteImplName"
INTERFACE_MAP=(
    "DeduplicationFilter:DeduplicationFilterImpl"
    "SpecValidator:SpecValidatorImpl"
    "IngestionService:IngestionServiceImpl"
    "SpecRepository:SpecRepositoryImpl"
    "IngestionEventPublisher:IngestionEventPublisherImpl"
    "IdempotencyStore:IdempotencyStoreImpl"
)

echo "  Checking interface implementations..."
echo ""

for mapping in "${INTERFACE_MAP[@]}"; do
    IFS=':' read -r iface impl <<< "$mapping" || true

    # Find interface file
    iface_file=$(find "$SRC_DIR" -name "${iface}.java" -type f 2>/dev/null | head -1 || true)
    if [[ -z "$iface_file" ]]; then
        log_fail "${iface} — interface file not found"
        continue
    fi
    log_pass "${iface} — interface found"

    # Find implementation file
    impl_file=$(find "$SRC_DIR" -name "${impl}.java" -type f 2>/dev/null | head -1 || true)
    if [[ -z "$impl_file" ]]; then
        log_fail "${impl} — implementation class not found for interface ${iface}"
        continue
    fi
    log_pass "${impl} — implementation found"

    # Verify the implementation actually implements the interface
    if grep -q "implements.*${iface}" "$impl_file" 2>/dev/null; then
        log_pass "${impl} implements ${iface}"
    elif grep -q "${iface}" "$impl_file" 2>/dev/null; then
        log_pass "${impl} references ${iface}"
    else
        log_fail "${impl} does not implement or extend ${iface}"
    fi
done

# Check for any interface that has no implementation at all
echo ""
echo "  Checking for orphaned interfaces..."
echo ""

while IFS= read -r -d '' iface_file; do
    ifname=$(basename "$iface_file" .java)

    # Skip files that are not interfaces
    if ! grep -q "^public interface\|^interface" "$iface_file" 2>/dev/null; then
        continue
    fi

    # Skip the DomainEvent interface (it's a base contract, not a service)
    if [[ "$ifname" == "DomainEvent" ]]; then
        continue
    fi

    # Check if this interface extends another interface (e.g. JpaRepository)
    if grep -q "extends.*<" "$iface_file" 2>/dev/null; then
        continue
    fi

    # Check if any class implements this interface within the module
    impl_count=$(grep -rl "implements.*${ifname}" "$SRC_DIR" 2>/dev/null | wc -l | tr -d ' ' || true)
    if [[ "$impl_count" -eq 0 ]]; then
        log_fail "${ifname} — no implementation found"
    fi
done < <(find "$SRC_DIR" -name "*.java" -type f -print0 2>/dev/null || true)

echo ""
echo "═══════════════════════════════════════════════"
echo "  Results"
echo "═══════════════════════════════════════════════"
echo "  Passed: ${PASS}"
echo "  Failed: ${#ERRORS[@]}"
echo ""

if [[ ${#ERRORS[@]} -gt 0 ]]; then
    echo "  FAILURES:"
    for err in "${ERRORS[@]}"; do
        echo "    - $err"
    done
    echo ""
    exit 1
fi

echo "  All contracts verified."
exit 0
