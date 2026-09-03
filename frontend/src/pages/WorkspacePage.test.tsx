import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import WorkspacePage from './WorkspacePage';

describe('WorkspacePage', () => {
  it('renders an enabled workspace entry button', () => {
    render(
      <MemoryRouter initialEntries={['/workspace']}>
        <WorkspacePage />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Workspace' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Open workspace/i })).toBeEnabled();
  });

  it('enters the permission-aware main route without client tenant state', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={['/workspace']}>
        <Routes>
          <Route path="workspace" element={<WorkspacePage />} />
          <Route path="/" element={<h1>Main application</h1>} />
        </Routes>
      </MemoryRouter>,
    );

    await user.click(screen.getByRole('button', { name: /Open workspace/i }));

    expect(await screen.findByRole('heading', { name: 'Main application' })).toBeInTheDocument();
    expect(localStorage.getItem('tenantId')).toBeNull();
    expect(localStorage.getItem('selectedWorkspace')).toBeNull();
  });
});
