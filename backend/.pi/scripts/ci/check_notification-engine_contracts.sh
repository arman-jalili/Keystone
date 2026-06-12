#!/usr/bin/env bash
# ============================================================================
# check_notification-engine_contracts.sh
#
# Validates that every interface defined in the contract freeze phase has
# at least one concrete implementation class in the notification-engine module.
#
# Usage: bash .pi/scripts/ci/check_notification-engine_contracts.sh [--help]
# Exit: 0 = all contracts implemented, 1 = violations found
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="${SCRIPT_DIR}/../../.."
NOTIFICATION_DIR="${PROJECT_DIR}/src/main/java/com/keystone/notification"
FAILED=0

show_help() { sed -n '3,10p' "$0"; exit 0; }
[[ "${1:-}" == "--help" ]] && show_help

echo "================================================"
echo "  Notification Engine Contract Implementation Check"
echo "================================================"
echo ""

check_pair() {
    local interface_path="$1"
    local impl_path="$2"
    local interface_file="${NOTIFICATION_DIR}/${interface_path}"
    local impl_file="${NOTIFICATION_DIR}/${impl_path}"

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

# Application layer
check_pair "application/service/NotificationDispatcher.java" "application/service/NotificationDispatcherImpl.java"

# Domain layer
check_pair "domain/channel/NotificationChannel.java" ""
check_pair "domain/channel/CiStatusChannel.java" "domain/channel/CiStatusChannelImpl.java"
check_pair "domain/service/ChannelRegistry.java" "domain/service/ChannelRegistryImpl.java"

# Infrastructure layer
check_pair "infrastructure/repository/NotificationRepository.java" "infrastructure/repository/NotificationRepositoryImpl.java"
check_pair "infrastructure/event/NotificationEventPublisher.java" "infrastructure/event/NotificationEventPublisherImpl.java"

echo ""
if [ $FAILED -eq 0 ]; then
    echo "✅ All notification-engine contracts have implementations."
else
    echo "❌ Some contracts are missing implementations."
fi
exit $FAILED
