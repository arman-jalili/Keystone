'use client';

import { useState, useCallback } from 'react';
import { toggleTheme, getStoredTheme, applyTheme, persistTheme } from '@/lib/theme';
import type { Theme } from '@/lib/contracts/theme';

interface ThemeToggleProps {
  /** Optional initial theme for SSR consistency */
  initialTheme?: Theme;
}

/**
 * Theme toggle switch — CSS-only toggle knob.
 * 28×16px track with 12px knob. No JavaScript animations.
 */
export function ThemeToggle({ initialTheme }: ThemeToggleProps) {
  const [theme, setTheme] = useState<Theme>(initialTheme ?? getStoredTheme());

  const handleToggle = useCallback(() => {
    const next: Theme = theme === 'light' ? 'dark' : 'light';
    setTheme(next);
    applyTheme(next);
    persistTheme(next);
  }, [theme]);

  return (
    <button
      type="button"
      onClick={handleToggle}
      className="relative h-4 w-7 cursor-pointer rounded-full border-none bg-border transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-accent"
      aria-label={`Switch to ${theme === 'light' ? 'dark' : 'light'} theme`}
      title={`Switch to ${theme === 'light' ? 'dark' : 'light'} theme`}
    >
      <span
        className={`absolute left-0.5 top-0.5 h-3 w-3 rounded-full bg-fg transition-transform duration-200 ${
          theme === 'dark' ? 'translate-x-3' : 'translate-x-0'
        }`}
      />
    </button>
  );
}
