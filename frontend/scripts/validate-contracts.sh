#!/usr/bin/env bash
# ============================================================================
# validate-contracts.sh — Verify all frozen contracts have implementations
#
# Reads each interface from lib/contracts/ and verifies a concrete
# implementation exists. Uses TypeScript compiler for type-checking.
#
# Canonical Reference: .pi/architecture/modules/frontend-app.md
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PASS=0
FAIL=0

echo "============================================"
echo "  Contract Implementation Check"
echo "============================================"
echo ""

# ── 1. Check TypeScript compiles ──
echo "--- TypeScript Compilation ---"
if npx tsc --noEmit 2>/dev/null; then
  echo "  ✅ TypeScript compiles without errors"
  PASS=$((PASS + 1))
else
  echo "  ❌ TypeScript compilation failed"
  FAIL=$((FAIL + 1))
fi

# ── 2. Verify contract files exist ──
echo ""
echo "--- Contract File Check ---"
CONTRACT_FILES=(
  "lib/contracts/types.ts"
  "lib/contracts/api-client.ts"
  "lib/contracts/endpoints.ts"
  "lib/contracts/errors.ts"
  "lib/contracts/transform.ts"
  "lib/contracts/view-config.ts"
  "lib/contracts/theme.ts"
  "lib/domain/events.ts"
)

for file in "${CONTRACT_FILES[@]}"; do
  if [ -f "$PROJECT_ROOT/$file" ]; then
    echo "  ✅ $file"
    PASS=$((PASS + 1))
  else
    echo "  ❌ MISSING: $file"
    FAIL=$((FAIL + 1))
  fi
done

# ── 3. Verify component contracts exist ──
echo ""
echo "--- Component Contract Check ---"
COMPONENT_CONTRACTS=(
  "components/layout/contracts.ts"
  "components/shared/contracts.ts"
  "components/overview/contracts.ts"
  "components/inventory/contracts.ts"
  "components/breaking/contracts.ts"
  "components/policy/contracts.ts"
  "components/graph/contracts.ts"
  "components/notifications/contracts.ts"
)

for file in "${COMPONENT_CONTRACTS[@]}"; do
  if [ -f "$PROJECT_ROOT/$file" ]; then
    echo "  ✅ $file"
    PASS=$((PASS + 1))
  else
    echo "  ❌ MISSING: $file"
    FAIL=$((FAIL + 1))
  fi
done

# ── 4. Verify implementations exist for all components ──
echo ""
echo "--- Implementation Check ---"
declare -A IMPLEMENTATIONS=(
  ["AppLayout"]="components/layout/AppLayout.tsx"
  ["NavRail"]="components/layout/NavRail.tsx"
  ["TopBar"]="components/layout/TopBar.tsx"
  ["ThemeToggle"]="components/layout/ThemeToggle.tsx"
  ["ViewShell"]="components/shared/ViewShell.tsx"
  ["StatGrid"]="components/shared/StatGrid.tsx"
  ["DataTable"]="components/shared/DataTable.tsx"
  ["OverviewView"]="components/overview/OverviewView.tsx"
  ["BreakingView"]="components/breaking/BreakingView.tsx"
  ["PolicyView"]="components/policy/PolicyView.tsx"
  ["InventoryView"]="components/inventory/InventoryView.tsx"
  ["GraphView"]="components/graph/GraphView.tsx"
  ["NotificationsView"]="components/notifications/NotificationsView.tsx"
)

for name in "${!IMPLEMENTATIONS[@]}"; do
  file="${IMPLEMENTATIONS[$name]}"
  if [ -f "$PROJECT_ROOT/$file" ]; then
    # Check the component is exported
    if grep -q "export.*function $name\|export const $name\|export { $name }" "$PROJECT_ROOT/$file" 2>/dev/null; then
      echo "  ✅ $name → $file"
    else
      echo "  ⚠️  $name found but may not be exported: $file"
    fi
    PASS=$((PASS + 1))
  else
    echo "  ❌ MISSING: $name → $file"
    FAIL=$((FAIL + 1))
  fi
done

# ── Summary ──
echo ""
echo "============================================"
echo "  Summary"
echo "============================================"
echo "  Passed: $PASS"
echo "  Failed: $FAIL"
echo ""

if [ "$FAIL" -gt 0 ]; then
  echo "❌ Some checks failed."
  exit 1
fi

echo "✅ All contract checks passed."
exit 0
