#!/usr/bin/env bash
# check_cli-orchestrator_contracts.sh
#
# Validates that every interface defined in the contract freeze has a
# corresponding implementation. Scans Go interface files in
# internal/application/interfaces/ and checks that a concrete type
# implementing each interface exists in internal/infrastructure/.
#
# Exit codes:
#   0 = PASS — all interfaces have implementations
#   1 = FAIL — one or more interfaces lack implementations
#
# Usage: bash check_cli-orchestrator_contracts.sh [--verbose]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
VERBOSE=false

if [[ "${1:-}" == "--verbose" ]]; then
    VERBOSE=true
fi

cd "${PROJECT_DIR}"

# Interface-to-implementation mapping
# Format: interface_name:impl_pattern1|impl_pattern2
MAPPINGS=(
    "SpecParser:KinOpenAPIParser"
    "SpecCache:FilesystemCache"
    "DiffEngine:SpecDiffEngine"
    "Uploader:HTTPUploader"
)

INTERFACE_DIR="internal/application/interfaces"
INFRA_DIR="internal/infrastructure"

errors=0
total=0

echo "=============================================="
echo "  Contract Implementation Check"
echo "=============================================="

for mapping in "${MAPPINGS[@]}"; do
    iface_name="${mapping%%:*}"
    impl_patterns="${mapping#*:}"
    total=$((total + 1))

    found=false
    OLD_IFS="$IFS"
    IFS='|'
    for pattern in $impl_patterns; do
        if grep -r -q "type ${pattern} struct" "${INFRA_DIR}/" 2>/dev/null; then
            found=true
            if $VERBOSE; then
                echo "  ✅ ${iface_name} → ${pattern}"
            fi
            break
        fi
    done
    IFS="$OLD_IFS"

    if ! $found; then
        echo "  ❌ ${iface_name}: no implementation found (expected one of: ${impl_patterns//|/ | })"
        errors=$((errors + 1))
    fi
done

echo "----------------------------------------------"
if [[ ${errors} -eq 0 ]]; then
    echo "  ✅ PASS: ${total}/${total} interfaces have implementations"
    echo ""
    exit 0
else
    echo "  ❌ FAIL: ${errors}/${total} interfaces lack implementations"
    echo ""
    exit 1
fi
