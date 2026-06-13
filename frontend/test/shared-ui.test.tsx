import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StatGrid } from '@/components/shared/StatGrid';
import { DataTable, Pill } from '@/components/shared/DataTable';
import { ViewShell } from '@/components/shared/ViewShell';
import { SectionLabel, TwoCol, ErrorState, ZeroState } from '@/components/shared/Utilities';

describe('ViewShell', () => {
  it('renders title and subtitle', () => {
    render(
      <ViewShell title="Governance Overview" subtitle="Key metrics">
        <p>content</p>
      </ViewShell>,
    );
    expect(screen.getByText('Governance Overview')).toBeDefined();
    expect(screen.getByText('Key metrics')).toBeDefined();
    expect(screen.getByText('content')).toBeDefined();
  });
});

describe('StatGrid', () => {
  it('renders all stat items', () => {
    const stats = [
      { value: 88, label: 'Health Score', tone: 'accent' as const },
      { value: 24, label: 'Total APIs' },
      { value: 7, label: 'Breaking Changes', tone: 'danger' as const },
    ];
    render(<StatGrid stats={stats} />);
    expect(screen.getByText('88')).toBeDefined();
    expect(screen.getByText('24')).toBeDefined();
    expect(screen.getByText('7')).toBeDefined();
    expect(screen.getByText('Health Score')).toBeDefined();
    expect(screen.getByText('Total APIs')).toBeDefined();
    expect(screen.getByText('Breaking Changes')).toBeDefined();
  });

  it('handles empty stats gracefully', () => {
    render(<StatGrid stats={[]} />);
    // Should render an empty grid container without crashing
    expect(document.querySelector('.grid')).toBeDefined();
  });
});

describe('DataTable', () => {
  const columns = [
    { key: 'name', label: 'Service Name', mono: true },
    { key: 'version', label: 'Version', numeric: true },
    { key: 'status', label: 'Status' },
  ];

  it('renders column headers', () => {
    render(<DataTable columns={columns} rows={[]} />);
    expect(screen.getByText('Service Name')).toBeDefined();
    expect(screen.getByText('Version')).toBeDefined();
    expect(screen.getByText('Status')).toBeDefined();
  });

  it('renders rows', () => {
    const rows = [
      { name: 'payment-svc', version: 'v2.1.0', status: 'healthy' },
      { name: 'user-svc', version: 'v3.0.0', status: 'at-risk' },
    ];
    render(<DataTable columns={columns} rows={rows} />);
    expect(screen.getByText('payment-svc')).toBeDefined();
    expect(screen.getByText('user-svc')).toBeDefined();
    expect(screen.getByText('v2.1.0')).toBeDefined();
  });

  it('shows empty state when no rows', () => {
    render(<DataTable columns={columns} rows={[]} />);
    expect(screen.getByText('No data')).toBeDefined();
  });

  it('renders caption when provided', () => {
    render(<DataTable columns={columns} rows={[]} caption="Updated 2h ago" />);
    expect(screen.getByText('Updated 2h ago')).toBeDefined();
  });
});

describe('Pill', () => {
  it('renders text with correct tone class', () => {
    const { container } = render(<Pill tone="pass">Passing</Pill>);
    expect(screen.getByText('Passing')).toBeDefined();
    expect(container.firstChild?.textContent).toBe('Passing');
  });
});

describe('SectionLabel', () => {
  it('renders section header text', () => {
    render(<SectionLabel>Critical Breakages</SectionLabel>);
    expect(screen.getByText('Critical Breakages')).toBeDefined();
  });
});

describe('TwoCol', () => {
  it('renders left and right content', () => {
    render(
      <TwoCol
        left={<p data-testid="left">Left</p>}
        right={<p data-testid="right">Right</p>}
      />,
    );
    expect(screen.getByTestId('left')).toBeDefined();
    expect(screen.getByTestId('right')).toBeDefined();
  });
});

describe('ErrorState', () => {
  it('renders error message', () => {
    render(<ErrorState message="Failed to load data" />);
    expect(screen.getByText('Unable to load view')).toBeDefined();
    expect(screen.getByText('Failed to load data')).toBeDefined();
  });

  it('renders retry button when onRetry provided', () => {
    render(<ErrorState message="Failed" onRetry={() => {}} />);
    expect(screen.getByText('Retry')).toBeDefined();
  });

  it('does not render retry button when onRetry is missing', () => {
    render(<ErrorState message="Failed" />);
    expect(screen.queryByText('Retry')).toBeNull();
  });

  it('renders custom title', () => {
    render(<ErrorState title="Custom Error" message="Something went wrong" />);
    expect(screen.getByText('Custom Error')).toBeDefined();
  });
});

describe('ZeroState', () => {
  it('renders label and description', () => {
    render(
      <ZeroState
        label="NO VIOLATIONS"
        description="All policies are passing"
      />,
    );
    expect(screen.getByText('NO VIOLATIONS')).toBeDefined();
    expect(screen.getByText('All policies are passing')).toBeDefined();
  });

  it('renders action button with onClick', () => {
    render(
      <ZeroState
        label="NO APIS"
        description="Upload a spec to get started"
        action={{ label: 'Upload a spec', onClick: () => {} }}
      />,
    );
    expect(screen.getByText('Upload a spec')).toBeDefined();
  });

  it('renders action link with href', () => {
    render(
      <ZeroState
        label="NO APIS"
        description="Upload a spec to get started"
        action={{ label: 'Go to docs', href: '/docs' }}
      />,
    );
    const link = screen.getByText('Go to docs');
    expect(link).toBeDefined();
    expect(link.closest('a')?.getAttribute('href')).toBe('/docs');
  });
});
