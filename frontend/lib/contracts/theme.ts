/**
 * Contract Freeze: Theme Contract
 *
 * Canonical Reference: .pi/architecture/modules/frontend-app.md#theme-architecture
 * Source: design/DESIGN.md, design/tokens.css
 *
 * Theme management contract. Implementation handles:
 * - data-theme attribute on <html>
 * - localStorage persistence (key: "keystone-theme")
 * - FOUC prevention via inline <script>
 * - CSS-only toggle animation
 */

/**
 * Supported theme modes.
 */
export type Theme = 'light' | 'dark';

/**
 * localStorage key for persisting theme preference.
 */
export const THEME_STORAGE_KEY = 'keystone-theme';

/**
 * Theme service interface.
 *
 * Implementations must:
 * 1. Read initial theme from localStorage (fallback to 'light')
 * 2. Apply theme by setting data-theme attribute on <html>
 * 3. Persist theme changes to localStorage
 * 4. Provide FOUC-prevention inline script content
 */
export interface ThemeService {
  /** Get the current active theme */
  getCurrentTheme(): Theme;

  /** Set the theme and persist */
  setTheme(theme: Theme): void;

  /** Toggle between light and dark */
  toggleTheme(): Theme;

  /** Get the inline script to prevent FOUC (placed in <head>) */
  getFoucPreventionScript(): string;
}

/**
 * CSS custom property names (mapped from tokens.css).
 * Implementation sets these on :root and [data-theme="dark"].
 */
export interface ThemeTokens {
  '--bg': string;
  '--surface': string;
  '--fg': string;
  '--muted': string;
  '--border': string;
  '--accent': string;
  '--success': string;
  '--warn': string;
  '--danger': string;
}

/**
 * Light theme token values (OKLch).
 */
export const LIGHT_TOKENS: ThemeTokens = {
  '--bg': 'oklch(97% 0.012 80)',
  '--surface': 'oklch(99% 0.005 80)',
  '--fg': 'oklch(20% 0.02 60)',
  '--muted': 'oklch(48% 0.015 60)',
  '--border': 'oklch(89% 0.012 80)',
  '--accent': 'oklch(58% 0.16 35)',
  '--success': 'oklch(62% 0.15 145)',
  '--warn': 'oklch(72% 0.15 85)',
  '--danger': 'oklch(56% 0.19 25)',
} as const;

/**
 * Dark theme token values (OKLch).
 */
export const DARK_TOKENS: ThemeTokens = {
  '--bg': 'oklch(16% 0.012 70)',
  '--surface': 'oklch(20% 0.010 70)',
  '--fg': 'oklch(92% 0.006 80)',
  '--muted': 'oklch(62% 0.012 70)',
  '--border': 'oklch(30% 0.012 70)',
  '--accent': 'oklch(64% 0.16 35)',
  '--success': 'oklch(66% 0.14 145)',
  '--warn': 'oklch(74% 0.14 85)',
  '--danger': 'oklch(60% 0.18 25)',
} as const;
