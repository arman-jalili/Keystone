-- =============================================================================
-- Keystone Backend — PostgreSQL Schema Initialization
-- =============================================================================
-- This script is mounted at /docker-entrypoint-initdb.d/ for the PostgreSQL
-- container. It creates all tables and indexes required by the Keystone
-- backend bounded contexts.
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- Ingestion Context
-- ─────────────────────────────────────────────────────────────────────────────

-- Tracked OpenAPI specifications
CREATE TABLE IF NOT EXISTS openapi_specs (
    id          UUID PRIMARY KEY,
    repository  VARCHAR(256) NOT NULL,
    spec_path   VARCHAR(512) NOT NULL,
    ingested_at TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_openapi_specs_repo_path ON openapi_specs (repository, spec_path);
CREATE INDEX IF NOT EXISTS idx_openapi_specs_ingested_at ON openapi_specs (ingested_at DESC);

-- Version history for tracked specs
CREATE TABLE IF NOT EXISTS spec_versions (
    id          UUID PRIMARY KEY,
    spec_id     UUID         NOT NULL REFERENCES openapi_specs(id) ON DELETE CASCADE,
    commit_sha  VARCHAR(64)  NOT NULL,
    checksum    VARCHAR(64)  NOT NULL,
    raw_content TEXT         NOT NULL,
    ingested_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_spec_versions_spec_id ON spec_versions (spec_id);
CREATE INDEX IF NOT EXISTS idx_spec_versions_ingested_at ON spec_versions (ingested_at DESC);

-- Idempotency keys for deduplication (7-day TTL per ADR-007)
CREATE TABLE IF NOT EXISTS idempotency_keys (
    event_id    UUID PRIMARY KEY,
    repository  VARCHAR(256) NOT NULL,
    commit_sha  VARCHAR(64)  NOT NULL,
    spec_path   VARCHAR(512) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    UNIQUE (repository, commit_sha, spec_path)
);

CREATE INDEX IF NOT EXISTS idx_idempotency_keys_created_at ON idempotency_keys (created_at);

-- ─────────────────────────────────────────────────────────────────────────────
-- Policy Context
-- ─────────────────────────────────────────────────────────────────────────────

-- Policy definitions (read-through cache synced from Git)
CREATE TABLE IF NOT EXISTS policies (
    id                    UUID PRIMARY KEY,
    name                  VARCHAR(128) NOT NULL UNIQUE,
    description           VARCHAR(1024),
    severity              VARCHAR(32)  NOT NULL,
    status                VARCHAR(32)  NOT NULL,
    dsl_expression        TEXT         NOT NULL,
    source_id             VARCHAR(256),
    version               INTEGER      NOT NULL DEFAULT 1,
    scope_path_patterns   TEXT,
    scope_operations      VARCHAR(256),
    scope_tags            VARCHAR(512),
    scope_exclude_paths   TEXT,
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_policies_status ON policies (status);
CREATE INDEX IF NOT EXISTS idx_policies_severity ON policies (severity);
CREATE INDEX IF NOT EXISTS idx_policies_source_id ON policies (source_id);

-- Policy set groupings
CREATE TABLE IF NOT EXISTS policy_sets (
    id          UUID PRIMARY KEY,
    name        VARCHAR(128) NOT NULL UNIQUE,
    description VARCHAR(1024),
    version     INTEGER      NOT NULL DEFAULT 1,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

-- Policy evaluation results
CREATE TABLE IF NOT EXISTS policy_evaluation_results (
    id                     UUID PRIMARY KEY,
    spec_id                UUID         NOT NULL,
    policy_set_id          UUID         NOT NULL,
    repository             VARCHAR(256) NOT NULL,
    spec_path              VARCHAR(512) NOT NULL,
    commit_sha             VARCHAR(40),
    verdict                VARCHAR(16)  NOT NULL,
    violations_json        TEXT,
    total_policies_checked INTEGER      NOT NULL DEFAULT 0,
    passed_count           INTEGER      NOT NULL DEFAULT 0,
    failed_count           INTEGER      NOT NULL DEFAULT 0,
    evaluated_at           TIMESTAMPTZ  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_eval_results_spec_id ON policy_evaluation_results (spec_id);
CREATE INDEX IF NOT EXISTS idx_eval_results_evaluated_at ON policy_evaluation_results (evaluated_at DESC);
CREATE INDEX IF NOT EXISTS idx_eval_results_verdict ON policy_evaluation_results (verdict);

-- ─────────────────────────────────────────────────────────────────────────────
-- Analysis Context
-- ─────────────────────────────────────────────────────────────────────────────

-- Breaking change analysis reports
CREATE TABLE IF NOT EXISTS breaking_change_reports (
    id              UUID PRIMARY KEY,
    base_spec_id    UUID,
    target_spec_id  UUID         NOT NULL,
    repository      VARCHAR(256) NOT NULL,
    spec_path       VARCHAR(512) NOT NULL,
    base_version    VARCHAR(256),
    target_version  VARCHAR(256) NOT NULL,
    verdict         VARCHAR(32)  NOT NULL,
    changes_json    TEXT,
    completed_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bcr_repository ON breaking_change_reports (repository);
CREATE INDEX IF NOT EXISTS idx_bcr_completed_at ON breaking_change_reports (completed_at DESC);
CREATE INDEX IF NOT EXISTS idx_bcr_verdict ON breaking_change_reports (verdict);

-- ─────────────────────────────────────────────────────────────────────────────
-- Dependency Graph Context
-- ─────────────────────────────────────────────────────────────────────────────

-- Registered services / APIs
CREATE TABLE IF NOT EXISTS graph_services (
    id         UUID PRIMARY KEY,
    name       VARCHAR(256) NOT NULL UNIQUE,
    team       VARCHAR(128),
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);

-- Service-to-service dependency edges
CREATE TABLE IF NOT EXISTS graph_api_dependencies (
    id            UUID PRIMARY KEY,
    producer_id   UUID         NOT NULL REFERENCES graph_services(id) ON DELETE CASCADE,
    consumer_id   UUID         REFERENCES graph_services(id) ON DELETE SET NULL,
    spec_path     VARCHAR(512) NOT NULL,
    discovered_at TIMESTAMPTZ  NOT NULL,
    UNIQUE (producer_id, consumer_id, spec_path)
);

CREATE INDEX IF NOT EXISTS idx_deps_producer ON graph_api_dependencies (producer_id);
CREATE INDEX IF NOT EXISTS idx_deps_consumer ON graph_api_dependencies (consumer_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- Dashboard Context
-- ─────────────────────────────────────────────────────────────────────────────

-- Health score snapshots (persisted, survives restarts)
CREATE TABLE IF NOT EXISTS health_scores (
    id            UUID PRIMARY KEY,
    entity_type   VARCHAR(64)  NOT NULL,
    entity_id     VARCHAR(256) NOT NULL,
    score         DOUBLE PRECISION NOT NULL,
    compliance_score DOUBLE PRECISION,
    stability_score  DOUBLE PRECISION,
    freshness_score  DOUBLE PRECISION,
    coverage_score   DOUBLE PRECISION,
    computed_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_health_scores_entity ON health_scores (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_health_scores_computed_at ON health_scores (computed_at DESC);

-- Append-only audit event store
CREATE TABLE IF NOT EXISTS audit_log_entries (
    id          VARCHAR(64)  PRIMARY KEY,
    action      VARCHAR(64)  NOT NULL,
    actor       VARCHAR(128) NOT NULL DEFAULT 'system',
    target      VARCHAR(512),
    details     VARCHAR(2048),
    timestamp   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_log_action ON audit_log_entries (action);
CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp ON audit_log_entries (timestamp DESC);
