# Architecture Decision Record: ADR-004

<!--
Canonical Reference: .pi/architecture/decisions/ADR-004-theme-design-system.md
Blueprint Source: Guardian Framework v1.2
-->

## Title

Theme & Design System Implementation Strategy

## Status

- [x] Accepted

## Context

The design system specifies a print-newspaper aesthetic ("Editorial-Monocle" direction) with:
- Light and dark themes defined in OKLch color space
- No border-radius, no box-shadows, no icon library
- Mono text abbreviations instead of icons
- Serif display font + sans-serif body font
- Desktop-first (≥1024px min-width)
- Theme persisted to `localStorage` (`keystone-theme` key)

The frontend uses Next.js with Tailwind CSS (as specified in `design/tailwind.config.ts` and `design/tokens.css`).

**Tech Stack:** TypeScript (Bun/Node), Next.js (App Router), Tailwind CSS

## Decision

### 1. CSS Custom Properties for Theming

We will define all design tokens as **CSS custom properties** on `:root` (light) and `[data-theme="dark"]` (dark), mirroring the OKLch values from `design/tokens.css`. Tailwind CSS will reference these custom properties via its `theme.extend.colors` configuration.

This avoids CSS-in-JS runtime cost and keeps theming purely in CSS.

### 2. Theme Switching

- Toggle implemented in `NavRail.tsx` as a CSS-only switch
- On toggle: set `data-theme` attribute on `<html>` and write to `localStorage`
- An inline `<script>` in the `<head>` reads `localStorage` before hydration to prevent FOUC (flash of unstyled content)

### 3. No CSS-in-JS / No Runtime Theming Libraries

- No styled-components, emotion, or CSS modules
- Tailwind utility classes + CSS custom properties handle all theming
- Component-specific styles in `app/globals.css` or `components/**/*.module.css` only when Tailwind cannot express the style

### 4. Typography

- Google Fonts or `@fontsource` packages for Iowan Old Style / Charter (serif display)
- System font stack for body (no additional downloads)
- `font-variant-numeric: tabular-nums` on all numeric data via Tailwind `font-mono` variant with `tabular-nums` class

### 5. No Icon Library

- Navigation uses 2-letter mono abbreviations (no icons)
- Inline indicators use punctuation or mono text labels
- This reduces bundle size and aligns with the print-newspaper aesthetic

## Alternatives Considered

| Alternative | Pros | Cons | Reason Rejected |
|-------------|------|------|-----------------|
| CSS-in-JS (styled-components) | Scoped styles, dynamic theming | Runtime cost, larger bundle, hydration mismatch risk | Rejected — CSS custom properties are sufficient; no dynamic theming needed beyond light/dark |
| Tailwind dark: variant only | Simple, built into Tailwind | Cannot use data-theme attribute; limited to OS preference; no localStorage persistence | Rejected — need persistent user-controlled theme switching |
| Icon library (Lucide, Heroicons) | Rich icon set, accessible | Adds bundle weight; contradicts design system's "no decorative icons" rule | Rejected — design system explicitly forbids icon libraries |
| CSS Modules only | Zero runtime, scoped | No theme variable reuse, verbose | Rejected — Tailwind + CSS custom properties gives best DX for this design system |
| Shadcn/ui or Radix primitives | Accessible, well-tested | Styled components fight the design system; too much overhead for a dashboard | Rejected — dashboards need custom components, not rebuilt primitives |

## Consequences

### Positive
- Zero runtime cost for theming
- Tailwind utility classes keep components lean
- FOUC prevention via inline script
- Design tokens are centralized in CSS custom properties — changes propagate everywhere
- No icon library keeps bundle small

### Negative
- Must manually maintain OKLch / Hex fallback values
- Tailwind config must be kept in sync with CSS custom properties
- Component states (hover, active, focus) must be styled manually — no pre-built component library

### Neutral
- Accessible color contrast must be verified per theme (especially on muted text)
- Print-newspaper aesthetic may feel unfamiliar to some users

## Implementation

**Affected Modules:**
- `.pi/architecture/modules/frontend-app.md`

**Files to Update:**
- `app/globals.css` — CSS custom properties, Tailwind directives, base styles
- `tailwind.config.ts` — extend colors to reference CSS vars
- `app/layout.tsx` — inline FOUC-prevention script, data-theme attribute
- `components/layout/NavRail.tsx` — theme toggle switch
- `lib/theme.ts` — theme read/write helpers

## Validation

**Validators Required:**
- architecture-validator: Verify theme implementation matches design/DESIGN.md

## References

- Related ADRs: ADR-002 (Frontend Routing — theme persistence alongside view persistence)
- Design docs: `design/DESIGN.md`, `design/tokens.css`, `design/tailwind.config.ts`

---

*Decision date: 2026-06-13*
*Decision makers: System Architect*
