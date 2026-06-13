import type { Config } from 'tailwindcss';

const config: Config = {
  content: [
    './app/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  darkMode: ['selector', '[data-theme="dark"]'],
  theme: {
    extend: {
      colors: {
        bg: 'oklch(97% 0.012 80)',
        surface: 'oklch(99% 0.005 80)',
        fg: 'oklch(20% 0.02 60)',
        muted: 'oklch(48% 0.015 60)',
        border: 'oklch(89% 0.012 80)',
        accent: 'oklch(58% 0.16 35)',
        success: 'oklch(62% 0.15 145)',
        warn: 'oklch(72% 0.15 85)',
        danger: 'oklch(56% 0.19 25)',
      },
      fontFamily: {
        display: ['Iowan Old Style', 'Charter', 'Georgia', 'serif'],
        body: ['-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'system-ui', 'sans-serif'],
        mono: ['ui-monospace', 'SF Mono', 'JetBrains Mono', 'Menlo', 'monospace'],
      },
      fontSize: {
        'view-title': ['32px', { lineHeight: '1.15', letterSpacing: '-0.02em', fontWeight: '400' }],
        'card-title': ['18px', { lineHeight: '1.3', letterSpacing: '-0.01em', fontWeight: '400' }],
        'stat-value': ['36px', { lineHeight: '1.0', letterSpacing: '-0.02em', fontWeight: '400' }],
        body: ['13px', { lineHeight: '1.5', letterSpacing: '0' }],
        'body-sm': ['12px', { lineHeight: '1.5', letterSpacing: '0.01em' }],
        caption: ['11px', { lineHeight: '1.5', letterSpacing: '0.01em' }],
        'label-mono': ['10px', { lineHeight: '1.5', letterSpacing: '0.08em', fontWeight: '500' }],
        'meta-mono': ['10px', { lineHeight: '1.5', letterSpacing: '0.06em' }],
        'code-mono': ['12px', { lineHeight: '1.7', letterSpacing: '0.02em' }],
        'nav-item': ['13px', { lineHeight: '1.5', letterSpacing: '0.01em' }],
        pill: ['10px', { lineHeight: '1.5', letterSpacing: '0.06em' }],
      },
      spacing: {
        xs: '4px',
        sm: '8px',
        md: '16px',
        lg: '24px',
        xl: '32px',
        '2xl': '48px',
      },
      borderRadius: {
        none: '0px',
        sm: '2px',
        full: '999px',
      },
      borderWidth: {
        DEFAULT: '1px',
        '2': '2px',
        '3': '3px',
      },
    },
  },
};

export default config;
