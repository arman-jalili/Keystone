# Keystone Dashboard — Routes & Navigation

---

## URL Structure

Single-page app with `?view=` search param:

```
/                           → redirects to /?view=overview
/?view=overview             → Overview / Health Score
/?view=inventory            → API Inventory / Catalog
/?view=breaking             → Breaking Change Analysis
/?view=policy               → Policy Compliance
/?view=graph                → Dependency Graph
/?view=notifications        → Notification Center
```

Theme persisted separately in localStorage (`keystone-theme`).

---

## Navigation Tree

```
Keystone
├── Overview              (ov)   badge: health score
├── API Inventory         (in)   badge: total APIs count
├── Breaking Changes      (br)   badge: open breakages
├── Policy Compliance     (po)   badge: open violations
├── Dependency Graph      (dg)   no badge
└── Notifications         (nt)   badge: unread count
```

---

## View Content Map

### Overview (`?view=overview`)
1. View title + subtitle
2. Score ring (160×160) + 5 dimension bars
3. Stat grid (5 cells)
4. Two-col: Recent Breakages table + Top Policy Violations table

### API Inventory (`?view=inventory`)
1. View title + subtitle
2. Full API table (8+ rows: service, version, spec, health pill, last analyzed, owner)
3. Section label: "Stale Specifications"
4. Stale APIs table (4 rows)

### Breaking Changes (`?view=breaking`)
1. View title + subtitle
2. Stat grid (4 cells: total, critical, high, non-breaking)
3. Section label: "Critical Breakages"
4. Breakage cards (2-3), each with: header, meta, diff block, impacted consumers list

### Policy Compliance (`?view=policy`)
1. View title + subtitle
2. Stat grid (4 cells: policies, pass rate, violations, APIs covered)
3. Section label: "Policy Rules"
4. Rule cards (7), alternating violated/passing

### Dependency Graph (`?view=graph`)
1. View title + subtitle
2. SVG graph container (900×420 viewBox, 15 nodes, 12 edges)
3. Legend row
4. Section label: "Impact Cascade"
5. One impact cascade card

### Notifications (`?view=notifications`)
1. View title + subtitle
2. Stat grid (4 cells: total 7d, unread, channels, delivery rate)
3. Section label: "Unread"
4. Notification feed (4 items)
5. Section label: "Channels"
6. Two-col: Slack channel card + Email channel card
