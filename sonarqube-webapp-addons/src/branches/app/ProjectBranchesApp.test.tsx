/*
 * Copyright (C) 2026 Haembina
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

import { act, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import BranchesServiceMock from '~sq-server-commons/api/mocks/BranchesServiceMock';
import SettingsServiceMock from '~sq-server-commons/api/mocks/SettingsServiceMock';
import { mockComponent } from '~sq-server-commons/helpers/mocks/component';
import { mockAppState } from '~sq-server-commons/helpers/testMocks';
import { renderAppWithComponentContext } from '~sq-server-commons/helpers/testReactTestingUtils';
import { Feature } from '~sq-server-commons/types/features';
import { SettingsKey } from '~sq-server-commons/types/settings';
// Loaded eagerly so the lazy route resolves at once rather than transforming the page on first render.
import './ProjectBranchesApp';
import routes from './routes';

const branchesHandler = new BranchesServiceMock();
const settingsHandler = new SettingsServiceMock();

beforeEach(() => {
  branchesHandler.reset();
  settingsHandler.reset();
});

function renderPage(canAdmin = true) {
  return renderAppWithComponentContext(
    'branches',
    routes,
    { appState: mockAppState({ canAdmin }), featureList: [Feature.BranchSupport] },
    { component: mockComponent() },
  );
}

/** Moves keyboard focus to an element, as a Tab walk would, inside React's act. */
const focus = (element: HTMLElement) => act(() => element.focus());

const rowOf = async (name: string) => (await screen.findByText(name)).closest('tr') as HTMLElement;

describe('capability list-branches: see every branch and pull request of a project', () => {
  it('lists the branches, main first, and switches to the pull requests', async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByText('delete-branch');
    const rows = screen.getAllByRole('row');
    expect(within(rows[1]).getByText('main')).toBeInTheDocument();
    expect(within(rows[1]).getByText('branches.main_branch')).toBeInTheDocument();
    expect(screen.getByText('delete-branch')).toBeInTheDocument();
    expect(screen.getByText('normal-branch')).toBeInTheDocument();
    expect(
      screen.getByText('project_branch_pull_request.branch.auto_deletion.keep_when_inactive'),
    ).toBeInTheDocument();

    await user.click(
      screen.getByRole('tab', { name: 'project_branch_pull_request.tabs.pull_requests' }),
    );

    expect(await screen.findByText('01 – TEST-191 update master')).toBeInTheDocument();
    expect(screen.getByText('02 – TEST-192 update normal-branch')).toBeInTheDocument();
    expect(screen.queryByText('delete-branch')).not.toBeInTheDocument();
    expect(
      screen.queryByText('project_branch_pull_request.branch.auto_deletion.keep_when_inactive'),
    ).not.toBeInTheDocument();
  });

  it('keeps a branch when it is inactive, and never offers that for the main branch', async () => {
    const user = userEvent.setup();
    renderPage();

    const branchSwitch = within(await rowOf('delete-branch')).getByRole('switch', {
      name: 'project_branch_pull_request.branch.auto_deletion.keep_when_inactive: delete-branch',
    });
    expect(branchSwitch).not.toBeChecked();
    await user.click(branchSwitch);
    expect(await within(await rowOf('delete-branch')).findByRole('switch')).toBeChecked();

    expect(
      within(await rowOf('main')).getByRole('switch', {
        name: 'project_branch_pull_request.branch.auto_deletion.keep_when_inactive: main',
      }),
    ).toBeDisabled();
  });

  it('says how long an inactive branch is kept, and links an admin to the setting', async () => {
    settingsHandler.set(SettingsKey.DaysBeforeDeletingInactiveBranchesAndPRs, '30');
    renderPage();

    expect(
      await screen.findByText(/project_branch_pull_request\.lifetime_information\.30/),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'settings.page' })).toHaveAttribute(
      'href',
      '/admin/settings?category=housekeeping#sonar.dbcleaner.daysBeforeDeletingInactiveBranchesAndPRs',
    );
  });
});

describe('capability delete-branch: delete a branch or pull request of a project', () => {
  it('deletes a branch after confirmation', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(
      within(await rowOf('delete-branch')).getByRole('button', {
        name: 'project_branch_pull_request.branch.actions_label.delete-branch',
      }),
    );
    await user.click(
      screen.getByRole('menuitem', { name: 'project_branch_pull_request.branch.delete' }),
    );
    const dialog = screen.getByRole('dialog');
    expect(
      within(dialog).getByText('project_branch_pull_request.branch.delete.are_you_sure.delete-branch'),
    ).toBeInTheDocument();

    await user.click(within(dialog).getByRole('button', { name: 'delete' }));

    expect(await screen.findByText('normal-branch')).toBeInTheDocument();
    expect(screen.queryByText('delete-branch')).not.toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('deletes a pull request using the keyboard alone', async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByText('delete-branch');
    focus(screen.getByRole('tab', { name: 'project_branch_pull_request.tabs.pull_requests' }));
    await user.keyboard('{Enter}');
    await screen.findByText('02 – TEST-192 update normal-branch');

    focus(
      screen.getByRole('button', {
        name: 'project_branch_pull_request.branch.actions_label.02 – TEST-192 update normal-branch',
      }),
    );
    await user.keyboard('{Enter}');
    expect(
      await screen.findByRole('menuitem', {
        name: 'project_branch_pull_request.pull_request.delete',
      }),
    ).toHaveFocus();
    await user.keyboard('{Enter}');

    const dialog = await screen.findByRole('dialog');
    expect(
      within(dialog).getByText(
        'project_branch_pull_request.pull_request.delete.are_you_sure.02 – TEST-192 update normal-branch',
      ),
    ).toBeInTheDocument();
    focus(within(dialog).getByRole('button', { name: 'delete' }));
    await user.keyboard('{Enter}');

    expect(await screen.findByText('01 – TEST-191 update master')).toBeInTheDocument();
    expect(screen.queryByText('02 – TEST-192 update normal-branch')).not.toBeInTheDocument();
  });
});

describe('capability rename-main-branch: rename the main branch of a project', () => {
  it('renames the main branch, and cannot submit an unchanged or empty name', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(
      within(await rowOf('main')).getByRole('button', {
        name: 'project_branch_pull_request.branch.actions_label.main',
      }),
    );
    await user.click(
      screen.getByRole('menuitem', { name: 'project_branch_pull_request.branch.rename' }),
    );

    const dialog = screen.getByRole('dialog');
    const input = within(dialog).getByRole('textbox');
    const submit = within(dialog).getByRole('button', { name: 'rename' });
    expect(input).toHaveFocus();
    expect(submit).toBeDisabled();

    await user.clear(input);
    expect(submit).toBeDisabled();

    await user.type(input, 'trunk');
    expect(submit).toBeEnabled();
    await user.keyboard('{Enter}');

    expect(await screen.findByText('trunk')).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('puts focus back on the row’s actions button when the dialog closes', async () => {
    const user = userEvent.setup();
    renderPage();
    const actions = within(await rowOf('main')).getByRole('button', {
      name: 'project_branch_pull_request.branch.actions_label.main',
    });

    await user.click(actions);
    await user.click(
      screen.getByRole('menuitem', { name: 'project_branch_pull_request.branch.rename' }),
    );
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'cancel' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(actions).toHaveFocus();
  });
});

describe('capability set-main-branch: make another branch the main branch of a project', () => {
  it('makes a branch the main branch and reloads the project', async () => {
    const user = userEvent.setup();
    const fetchComponent = jest.fn();
    renderAppWithComponentContext(
      'branches',
      routes,
      { appState: mockAppState({ canAdmin: true }), featureList: [Feature.BranchSupport] },
      { component: mockComponent(), fetchComponent },
    );

    await user.click(
      within(await rowOf('normal-branch')).getByRole('button', {
        name: 'project_branch_pull_request.branch.actions_label.normal-branch',
      }),
    );
    await user.click(
      screen.getByRole('menuitem', { name: 'project_branch_pull_request.branch.set_main' }),
    );

    const dialog = screen.getByRole('dialog');
    expect(
      within(dialog).getByText('project_branch_pull_request.branch.main_branch.requires_reindex'),
    ).toBeInTheDocument();
    await user.click(
      within(dialog).getByRole('button', { name: 'project_branch_pull_request.branch.set_main' }),
    );

    expect(
      await within(await rowOf('normal-branch')).findByText('branches.main_branch'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(fetchComponent).toHaveBeenCalled();
  });

  it('closes the dialog on cancel and changes nothing', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(
      within(await rowOf('normal-branch')).getByRole('button', {
        name: 'project_branch_pull_request.branch.actions_label.normal-branch',
      }),
    );
    await user.click(
      screen.getByRole('menuitem', { name: 'project_branch_pull_request.branch.set_main' }),
    );
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'cancel' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(within(await rowOf('main')).getByText('branches.main_branch')).toBeInTheDocument();
  });
});
