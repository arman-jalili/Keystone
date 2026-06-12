#!/usr/bin/env bash
# ============================================================================
# check_policy-engine_contracts.sh
#
# Validates that every interface defined in the contract freeze phase has
# at least one concrete implementation class in the policy-engine module.
#
# Usage: bash .pi/scripts/ci/check_policy-engine_contracts.sh [--help]
# Exit: 0 = all contracts implemented, 1 = violations found
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="${SCRIPT_DIR}/../../.."
POLICY_DIR="${PROJECT_DIR}/src/main/java/com/keystone/policy"
FAILED=0

show_help() { sed -n '3,10p' "$0"; exit 0; }
[[ "${1:-}" == "--help" ]] && show_help

echo "============================================"
echo "  Policy Engine Contract Implementation Check"
echo "============================================"
echo ""

check_pair() {
    local interface_path="$1"
    local impl_path="$2"
    local interface_file="${POLICY_DIR}/${interface_path}"
    local impl_file="${POLICY_DIR}/${impl_path}"

    if [ ! -f "$interface_file" ]; then
        echo "❌ MISSING INTERFACE: ${interface_path}"
        FAILED=1
        return
    fi

    if [ -z "$impl_path" ]; then
        echo "⚠️  NO IMPL: ${interface_path} (no implementation assigned yet)"
        return
    fi

    if [ ! -f "$impl_file" ]; then
        echo "❌ MISSING IMPL: ${impl_path} (for ${interface_path})"
        FAILED=1
        return
    fi

    echo "✅ ${interface_path} → ${impl_path}"
}

check_pair "application/service/PolicySyncService.java" "sync/PolicySyncServiceImpl.java"
check_pair "application/service/PolicyEvaluationService.java" "evaluator/PolicyEvaluationServiceImpl.java"
check_pair "application/service/PolicyManagementService.java" ""
check_pair "domain/service/EvaluationEngine.java" "evaluator/EvaluationEngineImpl.java"
check_pair "domain/model/ExemptionManager.java" ""
check_pair "infrastructure/repository/PolicyRepository.java" "infrastructure/repository/PolicyRepositoryImpl.java"
check_pair "infrastructure/source/PolicySource.java" "source/GitPolicySourceImpl.java"
check_pair "infrastructure/source/GitPolicySource.java" "source/GitPolicySourceImpl.java"
check_pair "infrastructure/event/PolicyEventPublisher.java" "infrastructure/event/PolicyEventPublisherImpl.java"

echo ""
if [ $FAILED -eq 0 ]; then
    echo "✅ All policy-engine contracts have implementations."
else
    echo "❌ Some contracts are missing implementations."
fi
exit $FAILED
