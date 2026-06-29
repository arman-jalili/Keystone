#!/usr/bin/env bash
# =============================================================================
# Keystone — Local CI Runner
# =============================================================================
# Runs all hardening stages from .pi/scripts/ci/ plus standard build/test.
# Mirrors the GitHub Actions pipeline.
#
# Usage:
#   bash local-ci.sh                    # Run all stages
#   bash local-ci.sh --verbose          # Show detailed output
#   bash local-ci.sh --stages 1,3,5     # Run specific stages only
#   bash local-ci.sh --help             # Show stage descriptions
# =============================================================================

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="${ROOT_DIR}/backend"
FRONTEND_DIR="${ROOT_DIR}/frontend"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

VERBOSE=false
ALL_STAGES=true
SELECTED_STAGES=()

while [[ $# -gt 0 ]]; do
    case $1 in
        --verbose) VERBOSE=true; shift ;;
        --stages)  ALL_STAGES=false; IFS=',' read -ra SELECTED_STAGES <<< "$2"; shift 2 ;;
        --help)
            echo "Stages:"
            echo "  1  — compile          Maven compile (Java 21)"
            echo "  2  — format           Spotless format check (Palantir)"
            echo "  3  — test             Unit tests (228+ tests)"
            echo "  4  — coverage         JaCoCo coverage report"
            echo "  5  — arch-conform     Architecture conformance (.pi/scripts/ci/)"
            echo "  6  — contracts        Contract impl checks per module (.pi/scripts/ci/)"
            echo "  7  — security         Secret scan, dependency audit"
            echo "  8  — docker           Docker Compose build"
            echo "  9  — frontend         Frontend build (TypeScript)"
            echo "  10 — verify           Full Maven verify"
            exit 0
            ;;
        *) shift ;;
    esac
done

should_run() {
    local stage="$1"
    [[ "$ALL_STAGES" == "true" ]] && return 0
    for s in "${SELECTED_STAGES[@]}"; do [[ "$s" == "$stage" ]] && return 0; done
    return 1
}

TOTAL=0; PASSED=0; FAILED=0; SKIPPED=0
declare -a RESULTS=()

run_cmd() {
    local num="$1" name="$2" dir="$3"; shift 3
    should_run "$num" || return 0
    ((TOTAL++))
    echo ""
    echo -e "${BLUE}═══ Stage ${num}: ${name} ═══${NC}"
    pushd "$dir" > /dev/null 2>&1 || { echo -e "  ${YELLOW}⊘ SKIPPED — cannot cd to ${dir}${NC}"; ((SKIPPED++)); RESULTS+=("SKIP:${name}"); return 0; }
    local output
    if output=$("$@" 2>&1); then
        echo -e "  ${GREEN}✓ PASSED${NC}"
        ((PASSED++)); RESULTS+=("PASS:${name}")
        $VERBOSE && echo "$output" | sed 's/^/    /'
    else
        echo -e "  ${RED}✗ FAILED${NC}"; ((FAILED++)); RESULTS+=("FAIL:${name}")
        echo "$output" | sed 's/^/    /'
    fi
    popd > /dev/null || true
}

run_script() {
    local num="$1" name="$2" dir="$3" script="$4"
    should_run "$num" || return 0
    ((TOTAL++))
    echo ""
    echo -e "${BLUE}═══ Stage ${num}: ${name} ═══${NC}"
    if [[ ! -f "$script" ]]; then
        echo -e "  ${YELLOW}⊘ SKIPPED — script not found: ${script}${NC}"; ((SKIPPED++)); RESULTS+=("SKIP:${name}")
        return 0
    fi
    pushd "$dir" > /dev/null 2>&1 || { echo -e "  ${YELLOW}⊘ SKIPPED — cannot cd to ${dir}${NC}"; ((SKIPPED++)); RESULTS+=("SKIP:${name}"); return 0; }
    local output
    if output=$(bash "$script" 2>&1); then
        echo -e "  ${GREEN}✓ PASSED${NC}"; ((PASSED++)); RESULTS+=("PASS:${name}")
        $VERBOSE && echo "$output" | sed 's/^/    /'
    else
        echo -e "  ${RED}✗ FAILED${NC}"; ((FAILED++)); RESULTS+=("FAIL:${name}")
        echo "$output" | sed 's/^/    /'
    fi
    popd > /dev/null || true
}

echo "╔══════════════════════════════════════════════════════╗"
echo "║         Keystone Local CI Runner                     ║"
echo "║   Stages from: backend/.pi/scripts/ci/              ║"
echo "╚══════════════════════════════════════════════════════╝"
echo ""

# ── Stage 1: Maven Compile ──
run_cmd "1" "compile" "$BACKEND_DIR" mvn clean compile -q -B

# ── Stage 2: Format Check (Spotless) ──
run_cmd "2" "format" "$BACKEND_DIR" mvn spotless:check -q -B

# ── Stage 3: Unit Tests ──
run_cmd "3" "test" "$BACKEND_DIR" mvn test -q -B

# ── Stage 4: Coverage (JaCoCo) ──
run_cmd "4" "coverage" "$BACKEND_DIR" mvn jacoco:report -q -B

# ── Stage 5: Architecture Conformance (.pi/scripts/ci/) ──
run_script "5" "arch-conform" "$BACKEND_DIR" \
    "$BACKEND_DIR/.pi/scripts/ci/check_architecture_conformance.sh"

# ── Stage 6: Contract Implementation Checks (.pi/scripts/ci/) ──
PI_CI="$BACKEND_DIR/.pi/scripts/ci"
for module in contract-ingestion policy-engine notification-engine dependency-graph dashboard; do
    contract_script="${PI_CI}/check_${module}_contracts.sh"
    run_script "6" "contracts:${module}" "$BACKEND_DIR" "$contract_script"
done

# ── Stage 7: Security Scan ──
if should_run "7"; then
    ((TOTAL++))
    echo ""
    echo -e "${BLUE}═══ Stage 7: security ═══${NC}"
    pushd "$ROOT_DIR" > /dev/null
    SECRETS=$(grep -rnE "(sk-[A-Za-z0-9]{32,}|ghp_[A-Za-z0-9]{36}|AKIA[0-9A-Z]{16}|-----BEGIN PRIVATE KEY-----)" \
        --include="*.java" --include="*.ts" --include="*.tsx" --include="*.go" --include="*.py" \
        --include="*.yml" --include="*.yaml" --include="*.json" \
        --exclude-dir=.git --exclude-dir=node_modules --exclude-dir=target --exclude-dir=.next \
        . 2>/dev/null || true)
    if [[ -z "$SECRETS" ]]; then
        echo -e "  ${GREEN}✓ PASSED — no secrets detected${NC}"; ((PASSED++)); RESULTS+=("PASS:security")
    else
        echo -e "  ${RED}✗ FAILED — potential secrets found${NC}"; echo "$SECRETS"; ((FAILED++)); RESULTS+=("FAIL:security")
    fi
    popd > /dev/null
fi

# ── Stage 8: Docker Build ──
run_cmd "8" "docker" "$ROOT_DIR" docker compose build --quiet 2>/dev/null

# ── Stage 9: Frontend Build ──
if should_run "9"; then
    ((TOTAL++))
    echo ""
    echo -e "${BLUE}═══ Stage 9: frontend ═══${NC}"
    if [[ -d "$FRONTEND_DIR" ]]; then
        pushd "$FRONTEND_DIR" > /dev/null
        if pnpm install --frozen-lockfile 2>/dev/null && pnpm build 2>/dev/null; then
            echo -e "  ${GREEN}✓ PASSED${NC}"; ((PASSED++)); RESULTS+=("PASS:frontend")
        else
            echo -e "  ${RED}✗ FAILED${NC}"; ((FAILED++)); RESULTS+=("FAIL:frontend")
        fi
        popd > /dev/null
    else
        echo -e "  ${YELLOW}⊘ SKIPPED — frontend directory not found${NC}"; ((SKIPPED++)); RESULTS+=("SKIP:frontend")
    fi
fi

# ── Stage 10: Full Verify ──
run_cmd "10" "verify" "$BACKEND_DIR" mvn verify -q -B

# ── Summary ──
echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "║               CI Stage Summary                       ║"
echo "╚══════════════════════════════════════════════════════╝"
for r in "${RESULTS[@]}"; do
    IFS=':' read -ra parts <<< "$r"
    case "${parts[0]}" in
        PASS) echo -e "  ${GREEN}✓${NC} ${parts[1]}" ;;
        FAIL) echo -e "  ${RED}✗${NC} ${parts[1]}" ;;
        SKIP) echo -e "  ${YELLOW}⊘${NC} ${parts[1]}" ;;
    esac
done
echo ""
echo -e "  ${GREEN}Passed:  ${PASSED}${NC}"
echo -e "  ${RED}Failed:  ${FAILED}${NC}"
echo -e "  ${YELLOW}Skipped: ${SKIPPED}${NC}"
echo "  Total:   ${TOTAL}"
echo ""
[[ $FAILED -eq 0 ]] && echo -e "${GREEN}✓ All stages passed.${NC}" || echo -e "${RED}✗ ${FAILED} stage(s) failed.${NC}"
echo ""
[[ $FAILED -eq 0 ]]
