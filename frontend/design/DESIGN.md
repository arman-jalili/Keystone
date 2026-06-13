# Keystone Dashboard — Design System

> Editorial-Monocle direction. Print-newspaper feel, enterprise API governance.
> Built for Next.js (App Router). Light + dark mode. Desktop-first, 1440px baseline.

---

## Visual Theme & Atmosphere

Paper newspaper, analogue rigour, chilled but enterprise. Generous whitespace, serif display headlines, restrained palette of off-white paper + ink + a single warm rust accent. Confident, quietly intelligent. No shadows, no rounded cards — borders and whitespace do the work.

---

## Color Palette

All colors defined in OKLch. Hex fallbacks provided for tooling that doesn't support OKLch.

### Light Theme

| Token     | OKLch                  | Hex       | Role |
|-----------|------------------------|-----------|------|
| `--bg`    | `oklch(97% 0.012 80)`  | `#F6F5F1` | Page background |
| `--surface`| `oklch(99% 0.005 80)` | `#FCFBFA` | Cards, nav, header |
| `--fg`    | `oklch(20% 0.02 60)`   | `#2C2A27` | Primary text |
| `--muted` | `oklch(48% 0.015 60)`  | `#6E6B64` | Secondary text, captions |
| `--border`| `oklch(89% 0.012 80)`  | `#DFDDD8` | Borders, dividers |
| `--accent`| `oklch(58% 0.16 35)`   | `#C45A3C` | Primary CTA, links, score ring |
| `--success`| `oklch(62% 0.15 145)`  | `#3D8C5C` | Healthy, passing |
| `--warn`  | `oklch(72% 0.15 85)`   | `#C4943A` | Warning, high |
| `--danger`| `oklch(56% 0.19 25)`   | `#C44A3C` | Critical, failing |

### Dark Theme

| Token     | OKLch                  | Hex       |
|-----------|------------------------|-----------|
| `--bg`    | `oklch(16% 0.012 70)`  | `#242320` |
| `--surface`| `oklch(20% 0.010 70)` | `#2E2D29` |
| `--fg`    | `oklch(92% 0.006 80)`  | `#EBE9E4` |
| `--muted` | `oklch(62% 0.012 70)`  | `#97948D` |
| `--border`| `oklch(30% 0.012 70)`  | `#46433D` |
| `--accent`| `oklch(64% 0.16 35)`   | `#D96B4A` |
| `--success`| `oklch(66% 0.14 145)`  | `#4DA86C` |
| `--warn`  | `oklch(74% 0.14 85)`   | `#D9A84A` |
| `--danger`| `oklch(60% 0.18 25)`   | `#D95A4A` |

### Accent Discipline

- One accent colour used at most **twice** per view.
- Allowed accent touchpoints: score ring, primary CTA, eyebrow/label, one rule accent line.
- Never use accent as a background fill. Never use accent on more than one element in the same visual region.

---

## Typography

### Font Stacks

| Role    | Stack |
|---------|-------|
| Display | `'Iowan Old Style', 'Charter', Georgia, serif` |
| Body    | `-apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif` |
| Mono   | `ui-monospace, 'SF Mono', 'JetBrains Mono', Menlo, monospace` |

### Type Scale

| Role       | Size   | Weight | Tracking      | Line-height | Font |
|------------|--------|--------|---------------|-------------|------|
| View title | 32px   | 400    | `-0.02em`     | 1.15        | Display |
| Card title | 18px   | 400    | `-0.01em`     | 1.3         | Display |
| Stat value | 36px   | 400    | `-0.02em`     | 1.0         | Display |
| Body       | 13-14px| 400    | `0`           | 1.5         | Body |
| Table cell | 13px   | 400    | `0`           | 1.5         | Body |
| Small      | 12px   | 400    | `0.01em`      | 1.5         | Body |
| Caption    | 11px   | 400    | `0.01em`      | 1.5         | Body |

### Mono Scale (labels, metadata, code)

| Role          | Size  | Weight | Tracking     | Font |
|---------------|-------|--------|--------------|------|
| Section label | 10px  | 500    | `0.08em`     | Mono |
| Nav items     | 13px  | 450    | `0.01em`     | Body |
| Nav badges    | 10px  | 400    | `0.04em`     | Mono |
| Table headers | 10px  | 500    | `0.08em`     | Mono |
| Table data    | 12px  | 400    | `0.02em`     | Mono |
| Breadcrumb    | 11px  | 400    | `0.06em`     | Mono |
| Pills         | 10px  | 400    | `0.06em`     | Mono |
| Card meta     | 10px  | 400    | `0.06em`     | Mono |

### Typography Rules

- ALL CAPS always has `letter-spacing >= 0.06em`. No exceptions.
- Display text (>=32px) always has negative tracking.
- Never set body and display to the same font family.
- `font-variant-numeric: tabular-nums` on all numeric table columns and stat values.
- No more than 3 type sizes visible above the fold in any view.

---

## Spacing

| Token  | Value | Usage |
|--------|-------|-------|
| `--space-xs` | 4px  | Internal label gaps |
| `--space-sm` | 8px  | Compact gaps |
| `--space-md` | 16px | Default gap, card internal padding |
| `--space-lg` | 24px | Section gaps, card padding |
| `--space-xl` | 32px | Content padding, section headers |
| `--space-2xl`| 48px | Major section separation |

Content area padding: `32px 40px 48px`.

---

## Borders & Surfaces

- Border width: `1px` everywhere (hairline).
- Border colour: `var(--border)`.
- No border-radius (0px). Sharp corners throughout.
- No box-shadows. Depth is signalled by borders and whitespace only.
- Cards: background `var(--surface)`, 1px border, 20px-24px internal padding.
- Header/footer borders: 2px (fg-colour) for table header separators.

### Two exceptions to the no-shadow rule

1. Theme toggle knob — uses a pseudo-element with `border-radius: 50%` and `transition: transform 0.2s`.
2. Live indicator dot — uses `border-radius: 50%` and a pulse animation.

These are functional UI chrome, not decorative depth.

---

## Components

### Navigation

- Fixed left rail, 232px wide, full height.
- Background: `var(--surface)`. Right border: 1px `var(--border)`.
- Wordmark at top: display font 22px, mono subtitle 10px.
- Nav items: 13px body font, 8px vertical padding, 12px horizontal.
- Active item: `color: var(--accent)`, `font-weight: 510`.
- Badge: mono 10px, background `var(--border)`, 2px radius.
- Theme toggle at bottom, separated by border-top rule.
- Nav items use 2-letter mono icons: `ov`, `in`, `br`, `po`, `dg`, `nt`.

### Top Bar

- 56px height, full width, bottom border.
- Left: breadcrumb (mono 11px, uppercase).
- Right: live indicator (7px dot with pulse animation + mono label).

### Cards

```
background: var(--surface)
border: 1px solid var(--border)
padding: 20px 24px
margin-bottom: 16px
```

Card variants:
- **Standard**: border on all sides.
- **Rule card**: 3px left border only. Green for passing, red for violated.
- **Diff block**: mono font, bg `var(--bg)`, pre-wrap, inside a standard card.

### Stat Grid

```
display: grid
grid-template-columns: repeat(auto-fit, minmax(180px, 1fr))
gap: 1px
background: var(--border)
border: 1px solid var(--border)
```

Each cell: background `var(--surface)`, padding `20px 24px`. Value in display font 36px, label in mono 10px uppercase.

### Tables

- Full width, collapsed borders.
- Header: 2px bottom border `var(--fg)`, mono 10px uppercase.
- Row: 1px bottom border `var(--border)`.
- Hover: background `var(--bg)`.
- Mono cells: 12px, `letter-spacing: 0.02em`.
- `font-variant-numeric: tabular-nums` on all numeric columns.

### Pills / Status Badges

```
display: inline-flex
padding: 2px 8px
border: 1px solid
font: 10px mono, uppercase, letter-spacing 0.06em
```

| Variant   | Text Color      | Border Color    | Background |
|-----------|-----------------|-----------------|------------|
| Critical  | `--danger`      | `--danger`      | danger @ 8% |
| High      | `--warn`        | `--warn`        | warn @ 8%   |
| Low       | `--success`     | `--success`     | success @ 8%|
| Info      | `--muted`       | `--border`      | `--bg`      |
| Pass      | `--success`     | `--success`     | success @ 5%|
| Fail      | `--danger`      | `--danger`      | danger @ 5% |
| Warn      | `--warn`        | `--warn`        | warn @ 5%   |

### Score Ring (Overview only)

- SVG donut chart, 160×160px.
- Background ring: `var(--border)`, 8px stroke.
- Progress arc: `var(--accent)`, 8px stroke, `stroke-linecap: round`.
- Rotated -90deg. Dasharray = circumference, dashoffset = (1 - score/100) × circumference.
- Centre: score number in display 44px `var(--accent)`, label in mono 10px uppercase.

### Dimension Bars (Overview)

- Label: mono 11px uppercase, right-aligned, 100px width.
- Bar: 100% width, 6px height, `var(--border)` background.
- Fill: absolute-positioned, `var(--fg)` (or success/danger/warn/accent).
- Value: mono 12px tabular-nums, right-aligned.

### Dependency Graph

- SVG container, `var(--surface)` background, 1px border.
- Nodes: rect with 2px radius, `var(--surface)` fill, `var(--border)` stroke.
- Node labels: display 13px + mono 10px subtitle.
- Edges: `var(--border)`, 1.5px stroke.
- Impacted edges: `var(--danger)`, 2px stroke.
- Impacted nodes: danger-tinted fill + danger stroke.
- Hover: accent stroke.
- Legend: mono 12px with swatch blocks.

### Notification Feed

- Feed items: flex row, 16px gap, 14px vertical padding.
- Unread dot: 8px circle, `var(--accent)`. Read: `var(--border)`.
- Title: 13px body, weight 510.
- Description: 12px body, `var(--muted)`.
- Meta: mono 10px, `var(--muted)`.
- Separator: 1px bottom border between items.

### Diff Block

```
font: 12px mono, 0.02em tracking, 1.7 line-height
background: var(--bg)
border: 1px solid var(--border)
padding: 16px 20px
white-space: pre-wrap
```

Removed lines: `color: var(--danger)`, `text-decoration: line-through`.
Added lines: `color: var(--success)`.

### Rule Card (Policy view)

- 3px left border, 14px 20px padding.
- Violated: left border `var(--danger)`.
- Passing: left border `var(--success)`.
- Name: 12px mono, weight 510.
- Description: 12px body, `var(--muted)`.
- Meta: 10px mono, `var(--muted)`, horizontal gap 16px.

### Two-Column Layout

```
display: grid
grid-template-columns: 1fr 1fr
gap: 24px
```

---

## Icon System

No icon library. Navigation uses 2-letter mono abbreviations. Inline elements use punctuation or mono text labels. This is deliberate — the newspaper aesthetic doesn't want decorative icons.

---

## Dark Mode

- Uses `data-theme` attribute on `<html>`.
- All colour tokens redefined under `[data-theme="dark"]`.
- Theme preference persisted to `localStorage` key `keystone-theme`.
- Toggle switch in nav footer: 28×16px track with 12px knob, CSS-only animation.

---

## View Persistence

- Active view persisted to `localStorage` key `keystone-view`.
- On load, restore last view. Fallback to `overview`.

---

## Do's and Don'ts

- ✅ Let whitespace do the work.
- ✅ One accent element per view — at most two.
- ✅ Sharp corners (0px radius) everywhere.
- ✅ Mono for all metadata, labels, badges, code.
- ✅ Serif display + sans body — never same family for both.
- ✅ Tabular nums on all numeric data.
- ✅ ALL CAPS always tracked >= 0.06em.
- ✅ Borders instead of shadows.
- ❌ No gradients.
- ❌ No box-shadows (except functional chrome: toggle knob, live dot).
- ❌ No rounded cards.
- ❌ No emoji icons.
- ❌ No coloured backgrounds (cards are always `var(--surface)`).
- ❌ No Inter, Roboto, or Arial as display face.
- ❌ No more than three type sizes above the fold.
- ❌ No icon library — mono text abbreviations instead.

---

## Responsive Notes

Desktop-first design. Minimum viable width: 1024px. The nav rail is fixed 232px. Tables should scroll horizontally on narrow viewports. Stat grids collapse to fewer columns via `auto-fit`.
