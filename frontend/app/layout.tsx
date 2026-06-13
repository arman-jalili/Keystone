import type { ReactNode } from 'react';
import { getFoucPreventionScript } from '@/lib/theme';
import './globals.css';

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: getFoucPreventionScript() }} />
      </head>
      <body>{children}</body>
    </html>
  );
}
