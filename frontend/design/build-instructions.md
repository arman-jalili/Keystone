# Keystone Dashboard — Build Instructions for Agents

> Read this first. Contains the step-by-step build order, file tree, and integration rules.

---

## Build Order

1. **Scaffold Next.js App Router project** — `npx create-next-app@latest frontend --typescript --tailwind --eslint --app --src-dir=false --import-alias="@/*" --no-turbopack`
2. **Copy tokens** — Drop `design/tokens.css` content into `app/globals.css` (replacing the default).
3. **Copy Tailwind config** — Replace `tailwind.config.ts` with `design/tailwind.config.ts`.
4. **Build the layout shell** — AppLayout, NavRail, TopBar (see below).
5. **Build shared components** — StatGrid, DataTable, Pill, SectionLabel, TwoCol, ViewShell.
6. **Build view-specific components** — ScoreRing, DimensionBar, RuleCard, DiffBlock, BreakageCard, DependencyGraph, NotificationFeed, ImpactCascade.
7. **Build the page** — Single `app/page.tsx` reading `?view=` searchParams to switch views.
8. **Wire theme toggle** — Client component, localStorage persistence.

---

## File Tree

```
frontend/
├── app/
│   ├── globals.css          ← tokens.css content + Tailwind directives
│   ├── layout.tsx            ← AppLayout (Server Component)
│   ├── page.tsx              ← reads ?view=, fetches data, renders view
│   └── not-found.tsx         ← optional
├── components/
│   ├── layout/
│   │   ├── AppLayout.tsx     ← flex row: NavRail + main
│   │   ├── NavRail.tsx       ← Client Component (interactive)
│   │   └── TopBar.tsx        ← Server or Client (if live indicator polls)
│   ├── shared/
│   │   ├── ViewShell.tsx
│   │   ├── StatGrid.tsx
│   │   ├── DataTable.tsx
│   │   ├── Pill.tsx
│   │   ├── SectionLabel.tsx
│   │   └── TwoCol.tsx
│   ├── overview/
│   │   ├── ScoreRing.tsx
│   │   └── DimensionBar.tsx
│   ├── breaking/
│   │   ├── DiffBlock.tsx
│   │   └── BreakageCard.tsx
│   ├── policy/
│   │   └── RuleCard.tsx
│   ├── graph/
│   │   ├── DependencyGraph.tsx
│   │   └── ImpactCascade.tsx
│   └── notifications/
│       ├── NotificationFeed.tsx
│       └── ChannelCard.tsx
├── lib/
│   ├── api.ts               ← fetch wrappers for Keystone backend
│   ├── types.ts              ← all TypeScript interfaces from data-schema.md
│   └── theme.ts              ← theme toggle hook
├── design/                   ← this directory (design source of truth)
│   ├── DESIGN.md
│   ├── tokens.css
│   ├── components.md
│   ├── data-schema.md
│   ├── routes.md
│   ├── tailwind.config.ts
│   └── build-instructions.md
├── tailwind.config.ts
├── tsconfig.json
├── next.config.ts
└── package.json
```

---

## Key Integration Rules

### 1. Color Tokens
Use Tailwind arbitrary values for colors since they're in OKLch:
```tsx
<div className="bg-[oklch(97%_0.012_80)] text-[oklch(20%_0.02_60)]">
```

Or bind them as CSS variables and reference via Tailwind:
```css
/* globals.css */
@theme {
  --color-bg: oklch(97% 0.012 80);
  --color-surface: oklch(99% 0.005 80);
  /* ... etc */
}
```

Then use `bg-bg`, `text-fg`, `border-border`, etc.

### 2. Fonts
No external font loading needed. The stacks use system fonts with serif fallbacks. The display font (`Iowan Old Style`, `Charter`, `Georgia`) is available on macOS and falls back gracefully.

### 3. Theme Toggle
```tsx
'use client';
// ThemeToggle.tsx
// Reads data-theme from <html>, toggles between light/dark
// Persists to localStorage key "keystone-theme"
// Uses a 28x16px track with ::after knob — CSS-only animation
```

### 4. View Switching
```tsx
// app/page.tsx
// Reads searchParams.view, defaults to "overview"
// Fetches the appropriate data for that view
// Renders the corresponding view component
// NavRail uses <Link> with ?view= params — no full page reloads
```

### 5. API Layer
```typescript
// lib/api.ts
const BASE = process.env.NEXT_PUBLIC_KEYSTONE_API_URL || 'http://localhost:8080/api/v1';

export async function fetchHealth(): Promise<GovernanceHealth> {
  const res = await fetch(`${BASE}/dashboard/health`, { next: { revalidate: 60 } });
  return res.json();
}
// ... one function per endpoint
```

### 6. Data Fetching
Use Server Components with `fetch()` where possible. Revalidate every 60 seconds for dashboard data. For components that need client interactivity (theme toggle, nav active state), use `'use client'` directive.

### 7. No External Dependencies
- No icon libraries (lucide-react, heroicons, etc.) — use 2-letter mono abbreviations.
- No UI component libraries (shadcn, Radix, MUI) — all components are custom.
- No animation libraries — CSS transitions and keyframes only.
- Keep it zero-dependency beyond React + Next.js + Tailwind.

### 8. Layout Contract
- NavRail: 232px wide, fixed. Do not change this width.
- TopBar: 56px tall, fixed.
- Content: fills remaining space, scrollable.
- No responsive breakpoints needed below 1024px (desktop-first).

---

## Component Props Reference

For full prop signatures, see `design/components.md`. Quick reference:

| Component | File | Key Props |
|-----------|------|-----------|
| ViewShell | `shared/ViewShell.tsx` | `title`, `subtitle`, `children` |
| StatGrid | `shared/StatGrid.tsx` | `stats: Array<{value, label, tone?}>` |
| DataTable | `shared/DataTable.tsx` | `columns`, `rows` |
| Pill | `shared/Pill.tsx` | `tone`, `children` |
| SectionLabel | `shared/SectionLabel.tsx` | `children` |
| TwoCol | `shared/TwoCol.tsx` | `left`, `right` |
| ScoreRing | `overview/ScoreRing.tsx` | `score`, `size?` |
| DimensionBar | `overview/DimensionBar.tsx` | `items` |
| RuleCard | `policy/RuleCard.tsx` | `name`, `description`, `scope`, `violationCount`, `violatingServices`, `status` |
| DiffBlock | `breaking/DiffBlock.tsx` | `diffText` |
| BreakageCard | `breaking/BreakageCard.tsx` | `serviceName`, `changeType`, `severity`, ... |
| DependencyGraph | `graph/DependencyGraph.tsx` | `nodes`, `edges` |
| ImpactCascade | `graph/ImpactCascade.tsx` | `sourceService`, `sourceVersion`, ... |
| NotificationFeed | `notifications/NotificationFeed.tsx` | `items` |
| ChannelCard | `notifications/ChannelCard.tsx` | `type`, `status`, `config` |

---

## Visual QA Checklist (before marking complete)

- [ ] Light mode renders with off-white paper background, sharp corners, no shadows
- [ ] Dark mode renders with deep warm-dark background, all tokens flip correctly
- [ ] Theme toggle animates knob slide, persists across refresh
- [ ] View switching updates nav active state, breadcrumb, and URL without full reload
- [ ] Display font (Iowan Old Style / Charter / Georgia serif) renders on titles and stat values
- [ ] Mono font renders on all labels, badges, table headers, breadcrumbs, code
- [ ] ALL CAPS text has visible letter-spacing (>= 0.06em) — not cramped
- [ ] Accent colour appears at most twice per view
- [ ] No box shadows, no rounded corners, no gradients
- [ ] Tables use tabular-nums for alignment
- [ ] Nav rail is exactly 232px, top bar exactly 56px
- [ ] Stat grid has 1px gap showing border colour between cells
- [ ] Score ring renders as SVG donut with correct dashoffset
- [ ] Diff block shows removed lines in red with line-through, added lines in green
- [ ] Rule cards show left border accent (green for passing, red for violated)
- [ ] Dependency graph nodes are positioned correctly, edges connect to rect centres
- [ ] Scrollbar is minimal (6px, border colour)
