# Architecture Change Log

<!--
Canonical Reference: .pi/architecture/CHANGELOG.md
Blueprint Source: Guardian Framework v1.2
DO NOT EDIT GENERATED FILES - Modify this source only
-->

This document tracks all architecture changes requiring implementation updates.

---

## Change Log Format

Each entry follows this structure:

```markdown
## [YYYY-MM-DD] - [Change Title]

### Changed
- Module: [module-name]
  - [Component]: [what changed]
  - [Component]: [what changed]

### Impact Analysis
- Files affected:
  - src/[path1]
  - src/[path2]
- Canonical refs to update:
  - .pi/architecture/modules/[module].md#[section]
- Validators required:
  - [validator-name]

### Migration Steps
1. [Step 1]
2. [Step 2]
3. [Step 3]

### Status
- [ ] Architecture doc updated
- [ ] CHANGELOG entry added
- [ ] Implementation updated
- [ ] Canonical refs updated
- [ ] Validators run
```

---

## Entries

## [2026-06-12] - cli-orchestrator Full Implementation

### Added
- Module: cli-orchestrator
  - SpecParser: `internal/infrastructure/parser/kinopenapi_parser.go`
  - LocalCache: `internal/infrastructure/cache/filesystem_cache.go`
  - LocalDiffEngine: `internal/infrastructure/diff/spec_diff.go`
  - Uploader: `internal/infrastructure/uploader/http_uploader.go`
- docs/runbook.md — operations runbook
- docs/dr-plan.md — disaster recovery plan

### Changed
- Architecture module doc updated with correct implementation file paths

### Status
- [x] All 7 cli-orchestrator issues implemented and merged
- [x] Architecture docs updated
- [x] CHANGELOG updated

---

<!-- Add new entries above this line -->

---

## Template Usage

When making architecture changes:

1. **Before change**: Review existing architecture docs
2. **During change**: Update `.pi/architecture/modules/[module].md`
3. **After change**: Add entry to this CHANGELOG
4. **Implementation**: Follow migration steps, update canonical refs
5. **Validation**: Run `validate-canonical.sh` to verify sync

---

## Architecture Sync Status

Track which changes have been synced to implementation:

| Date | Change | Module | Sync Status | Validator Status |
|------|--------|--------|-------------|------------------|
| [date] | [title] | [module] | [pending/complete] | [pass/fail] |

---

*Last updated: [date]*
*Framework version: 1.2.0*