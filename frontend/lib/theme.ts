/**
 * Theme service implementation.
 *
 * Canonical Reference: .pi/architecture/modules/frontend-app.md#theme-architecture
 * Contract: lib/contracts/theme.ts
 */
import { THEME_STORAGE_KEY, LIGHT_TOKENS, DARK_TOKENS } from '@/lib/contracts/theme';
import type { Theme } from '@/lib/contracts/theme';

/**
 * Get the current theme from localStorage.
 * Falls back to 'light' if not set.
 */
export function getStoredTheme(): Theme {
  if (typeof window === 'undefined') return 'light';
  const stored = localStorage.getItem(THEME_STORAGE_KEY);
  if (stored === 'dark' || stored === 'light') return stored;
  return 'light';
}

/**
 * Apply theme by setting data-theme attribute on <html>.
 */
export function applyTheme(theme: Theme): void {
  if (typeof document === 'undefined') return;
  document.documentElement.setAttribute('data-theme', theme);
}

/**
 * Persist theme to localStorage.
 */
export function persistTheme(theme: Theme): void {
  if (typeof window === 'undefined') return;
  localStorage.setItem(THEME_STORAGE_KEY, theme);
}

/**
 * Toggle between light and dark themes.
 */
export function toggleTheme(): Theme {
  const current = getStoredTheme();
  const next: Theme = current === 'light' ? 'dark' : 'light';
  applyTheme(next);
  persistTheme(next);
  return next;
}

/**
 * Initialize theme on page load.
 * Reads localStorage and applies before hydration to prevent FOUC.
 */
export function initTheme(): Theme {
  const theme = getStoredTheme();
  applyTheme(theme);
  return theme;
}

/**
 * Returns an inline <script> string to place in <head> for FOUC prevention.
 * This runs before React hydration.
 */
export function getFoucPreventionScript(): string {
  return `
    (function() {
      var key = '${THEME_STORAGE_KEY}';
      var theme = localStorage.getItem(key);
      if (theme !== 'dark' && theme !== 'light') theme = 'light';
      document.documentElement.setAttribute('data-theme', theme);
    })();
  `;
}
