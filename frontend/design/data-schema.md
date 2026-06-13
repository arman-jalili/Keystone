# Keystone Dashboard — Data Schema

All data shapes aligned to the backend domain model. These are the shapes the Next.js frontend should expect from the API layer.

---

## API Endpoints

| Endpoint | Method | Returns | View |
|----------|--------|---------|------|
| `/api/v1/dashboard/health` | GET | `GovernanceHealth` | Overview |
| `/api/v1/ingestion/apis` | GET | `ApiInventoryItem[]` | API Inventory |
| `/api/v1/ingestion/apis/stale` | GET | `StaleApiItem[]` | API Inventory |
| `/api/v1/analysis/breaking-changes` | GET | `BreakingChange[]` | Breaking Changes |
| `/api/v1/policies` | GET | `Policy[]` | Policy Compliance |
| `/api/v1/policies/summary` | GET | `PolicySummary` | Policy Compliance |
| `/api/v1/dependencies/graph` | GET | `DependencyGraph` | Dependency Graph |
| `/api/v1/dependencies/impact` | GET | `ImpactCascade[]` | Dependency Graph |
| `/api/v1/notifications` | GET | `Notification[]` | Notifications |
| `/api/v1/notifications/channels` | GET | `NotificationChannel[]` | Notifications |

---

## Type Definitions

### GovernanceHealth

```typescript
interface GovernanceHealth {
  overallScore: number;           // 0-100
  dimensions: {
    compliance: HealthDimension;  // policy pass rate
    breaking: HealthDimension;    // open breakages
    coverage: HealthDimension;    // % APIs with active policies
    staleness: HealthDimension;   // APIs past ingestion threshold
    impact: HealthDimension;      // % consumers covered by dependency mapping
  };
  summary: {
    totalApis: number;
    activePolicies: number;
    breakingChanges30d: number;
    servicesAtRisk: number;
    dependencyEdges: number;
  };
  recentBreakages: RecentBreakage[];
  topViolations: TopViolation[];
}

interface HealthDimension {
  value: number;    // raw count or percentage value
  pct: number;      // 0-100 for bar display
  label: string;    // human-readable interpretation
  trend?: 'up' | 'down' | 'stable';
  tone: 'success' | 'warn' | 'danger' | 'accent' | 'neutral';
}

interface RecentBreakage {
  serviceName: string;
  changeType: 'field-removal' | 'type-change' | 'path-removal' | 'enum-change';
  severity: 'critical' | 'high';
  relativeTime: string;  // e.g. "2h ago"
}

interface TopViolation {
  serviceName: string;
  policyName: string;
  violationCount: number;
  trend: number;  // +N or -N
}
```

### API Inventory

```typescript
interface ApiInventoryItem {
  id: string;
  serviceName: string;
  version: string;             // semver
  specFormat: 'OpenAPI 3.0' | 'OpenAPI 3.1';
  health: 'healthy' | 'low-risk' | 'warning' | 'at-risk';
  lastAnalyzed: string;        // ISO 8601
  owner: string;               // team name
  policyPassRate?: number;     // 0-100
  openBreakages?: number;
}

interface StaleApiItem {
  id: string;
  serviceName: string;
  lastIngested: string;        // ISO 8601
  daysStale: number;
  version: string;
}
```

### Breaking Changes

```typescript
interface BreakingChange {
  id: string;
  serviceName: string;
  changeType: 'field-removal' | 'type-change' | 'path-removal' | 'enum-change';
  severity: 'critical' | 'high';
  detectedAt: string;          // ISO 8601
  versionFrom: string;
  versionTo: string;
  diffText: string;            // raw diff with -/+ markers
  impactedConsumers: string[]; // service names
}

interface BreakingChangeSummary {
  total30d: number;
  critical: number;
  high: number;
  nonBreaking: number;
  items: BreakingChange[];
}
```

### Policies

```typescript
interface Policy {
  id: string;
  name: string;                // kebab-case identifier
  description: string;
  scope: string;               // e.g. "all APIs", "user-facing APIs"
  status: 'passing' | 'violated';
  violationCount: number;
  violatingServices: string[];
}

interface PolicySummary {
  activePolicies: number;
  passRate: number;            // 0-100
  openViolations: number;
  coveredApis: number;
  policies: Policy[];
}
```

### Dependency Graph

```typescript
interface DependencyGraph {
  nodes: GraphNode[];
  edges: GraphEdge[];
}

interface GraphNode {
  id: string;
  label: string;               // service name
  subtitle: string;            // e.g. "API · v3.2.1"
  kind: 'api' | 'svc' | 'ui';
  x: number;                   // layout position
  y: number;
  impacted: boolean;           // affected by active breakage
}

interface GraphEdge {
  from: string;                // node id
  to: string;                  // node id
  impacted: boolean;           // this edge is on an impact path
}

interface ImpactCascade {
  id: string;
  sourceService: string;
  sourceVersion: string;
  changeDescription: string;
  downstreamServices: string[];
  totalConsumers: number;
  severity: 'critical' | 'high';
}
```

### Notifications

```typescript
interface Notification {
  id: string;
  title: string;
  description: string;
  severity: 'critical' | 'high' | 'warning';
  channel: 'slack' | 'email' | 'webhook';
  channelDetail: string;       // e.g. "#api-gov", "gov-team@company.com"
  read: boolean;
  timestamp: string;           // ISO 8601
  relativeTime: string;        // e.g. "2 hours ago"
}

interface NotificationChannel {
  id: string;
  type: 'slack' | 'email' | 'webhook';
  status: 'active' | 'inactive';
  config: {
    target: string;            // channel name, email, or webhook URL
    rules: string[];           // e.g. ["on-breaking-change", "on-policy-violation"]
    lastDelivered: string;     // relative time
  };
}

interface NotificationSummary {
  total7d: number;
  unread: number;
  activeChannels: number;
  deliveryRate: number;        // 0-100
}
```

---

## Data Fetching Pattern

Use React Server Components where possible. Fetch data in `page.tsx` with `fetch()` to the Keystone backend API. For client-side interactivity (view switching, theme toggle):

```
app/
├── layout.tsx          ← AppLayout (Server), fetches nothing
├── page.tsx            ← redirects to /overview
├── overview/
│   └── page.tsx        ← fetches GovernanceHealth
├── inventory/
│   └── page.tsx        ← fetches ApiInventoryItem[] + StaleApiItem[]
├── breaking/
│   └── page.tsx        ← fetches BreakingChangeSummary
├── policy/
│   └── page.tsx        ← fetches PolicySummary
├── graph/
│   └── page.tsx        ← fetches DependencyGraph + ImpactCascade[]
├── notifications/
│   └── page.tsx        ← fetches Notification[] + NotificationChannel[]
```

Or — single-page with client-side view switching using searchParams:
```
app/
├── layout.tsx          ← AppLayout
├── page.tsx            ← reads ?view= param, fetches appropriate data
```

**Recommended:** Single-page with searchParams. This avoids full-page navigation on view switch and preserves the SPA feel while keeping RSC benefits.

### API Base URL

```
NEXT_PUBLIC_KEYSTONE_API_URL=http://localhost:8080/api/v1
```

All fetches use this base. In production, this points to the Keystone backend service.
