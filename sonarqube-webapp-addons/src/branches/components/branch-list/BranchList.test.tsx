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

import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { last } from 'lodash';
import MessagesServiceMock from '~sq-server-commons/api/mocks/MessagesServiceMock';
import NewCodeDefinitionServiceMock from '~sq-server-commons/api/mocks/NewCodeDefinitionServiceMock';
import {
  listBranchesNewCodeDefinition,
  resetNewCodeDefinition,
  setNewCodeDefinition,
} from '~sq-server-commons/api/newCodeDefinition';
import { mockBranch, mockMainBranch } from '~sq-server-commons/helpers/mocks/branch-like';
import { mockComponent } from '~sq-server-commons/helpers/mocks/component';
import {
  mockNewCodePeriod,
  mockNewCodePeriodBranch,
} from '~sq-server-commons/helpers/mocks/new-code-definition';
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import { Branch } from '~sq-server-commons/types/branch-like';
import {
  NewCodeDefinition,
  NewCodeDefinitionType,
} from '~sq-server-commons/types/new-code-definition';
import BranchListSection from './BranchListSection';

jest.mock('~sq-server-commons/api/messages');
jest.mock('~sq-server-commons/api/newCodeDefinition');

const messagesHandler = new MessagesServiceMock();
const newCodeHandler = new NewCodeDefinitionServiceMock();

// Built afresh for each render, because the list mutates the branch objects it is given.
const branchList = (): Branch[] => [
  mockMainBranch({ name: 'main' }),
  mockBranch({ name: 'feature' }),
  mockBranch({ name: 'release' }),
];

beforeEach(() => {
  messagesHandler.reset();
  newCodeHandler.reset();
});

function renderSection(
  projectNewCodeDefinition: NewCodeDefinition = mockNewCodePeriod(),
  branches: Branch[] = branchList(),
) {
  return renderComponent(
    <BranchListSection
      branchList={branches}
      component={mockComponent()}
      globalNewCodeDefinition={mockNewCodePeriod({ type: NewCodeDefinitionType.NumberOfDays })}
      projectNewCodeDefinition={projectNewCodeDefinition}
    />,
  );
}

interface Settle {
  reject: (reason: Error) => void;
  resolve: () => void;
}

const rowOf = async (name: string) =>
  (await screen.findByRole('cell', { name: new RegExp(`^${name}`) })).closest('tr') as HTMLElement;

describe('capability set-branch-new-code: give a branch its own new code definition', () => {
  it('lists each branch with its own setting, or the default it inherits', async () => {
    renderSection();

    expect(screen.getByRole('heading', { name: 'project_baseline.configure_branches' }))
      .toBeInTheDocument();
    expect(
      within(await rowOf('feature')).getByText('new_code_definition.number_days: 1'),
    ).toBeInTheDocument();
    expect(within(await rowOf('main')).getByText('branch_list.default_setting')).toBeInTheDocument();
    expect(within(await rowOf('main')).getByText('branches.main_branch')).toBeInTheDocument();
  });

  it('describes every kind of setting a branch can have', async () => {
    newCodeHandler.setListBranchesNewCode([
      mockNewCodePeriodBranch({ branchKey: 'main', type: NewCodeDefinitionType.PreviousVersion }),
      mockNewCodePeriodBranch({
        branchKey: 'feature',
        type: NewCodeDefinitionType.SpecificAnalysis,
        value: 'analysis-1',
        effectiveValue: '2026-01-10T10:00:00+0000',
      }),
      mockNewCodePeriodBranch({
        branchKey: 'release',
        type: NewCodeDefinitionType.SpecificAnalysis,
        value: 'analysis-2',
      }),
    ]);
    renderSection();

    expect(
      within(await rowOf('main')).getByText('new_code_definition.previous_version'),
    ).toBeInTheDocument();
    expect(
      within(await rowOf('feature')).getByText(/baseline\.specific_analysis/),
    ).toBeInTheDocument();
    expect(within(await rowOf('release')).getByText(/\?/)).toBeInTheDocument();
  });

  it('warns when a branch references a branch that is gone', async () => {
    newCodeHandler.setListBranchesNewCode([
      mockNewCodePeriodBranch({
        branchKey: 'feature',
        type: NewCodeDefinitionType.ReferenceBranch,
        value: 'deleted',
      }),
    ]);
    const user = userEvent.setup();
    renderSection(
      mockNewCodePeriod({ type: NewCodeDefinitionType.ReferenceBranch, value: 'main' }),
    );

    await user.hover(
      within(await rowOf('feature')).getByText('baseline.reference_branch: deleted'),
    );
    expect(
      await screen.findByRole('tooltip', {
        name: 'baseline.reference_branch.does_not_exist.deleted',
      }),
    ).toBeInTheDocument();
  });

  it('warns when a branch would inherit itself as its reference', async () => {
    const user = userEvent.setup();
    renderSection(
      mockNewCodePeriod({ type: NewCodeDefinitionType.ReferenceBranch, value: 'main' }),
    );

    await user.hover(within(await rowOf('main')).getByText('branch_list.default_setting'));
    expect(
      await screen.findByRole('tooltip', {
        name: 'baseline.reference_branch.invalid_branch_setting.main',
      }),
    ).toBeInTheDocument();
  });

  it('sets a number of days for a branch that inherits', async () => {
    const user = userEvent.setup();
    renderSection();

    await user.click(await screen.findByRole('button', { name: 'branch_list.edit_for_x.release' }));
    const dialog = screen.getByRole('dialog');
    const save = within(dialog).getByRole('button', { name: 'save' });
    expect(save).toBeDisabled();

    await user.click(
      within(dialog).getByRole('radio', {
        name: /new_code_definition.specific_setting.number_of_days.label/,
      }),
    );
    await user.clear(within(dialog).getByRole('spinbutton'));
    await user.type(within(dialog).getByRole('spinbutton'), '15');
    await user.click(save);

    expect(setNewCodeDefinition).toHaveBeenCalledWith({
      branch: 'release',
      project: 'my-project',
      type: NewCodeDefinitionType.NumberOfDays,
      value: '15',
    });
    expect(
      await within(await rowOf('release')).findByText('new_code_definition.number_days: 15'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('points a branch at another branch, never at itself', async () => {
    const user = userEvent.setup();
    renderSection();

    await user.click(await screen.findByRole('button', { name: 'branch_list.edit_for_x.release' }));
    const dialog = screen.getByRole('dialog');
    await user.click(
      last(
        within(dialog).getAllByRole('radio', {
          name: /new_code_definition.specific_setting.reference_branch.label/,
        }),
      ) as HTMLElement,
    );
    await user.click(
      within(dialog).getByRole('combobox', {
        name: 'new_code_definition.specific_setting.reference_branch.input.label',
      }),
    );
    expect(screen.queryByRole('option', { name: /release/ })).not.toBeInTheDocument();
    await user.click(screen.getByRole('option', { name: /feature/ }));
    await user.click(within(dialog).getByRole('button', { name: 'save' }));

    expect(
      await within(await rowOf('release')).findByText('baseline.reference_branch: feature'),
    ).toBeInTheDocument();
  });

  it('keeps the dialog open when saving fails, and closes it on cancel', async () => {
    const user = userEvent.setup();
    jest.mocked(setNewCodeDefinition).mockRejectedValueOnce(new Error('refused'));
    renderSection();

    await user.click(await screen.findByRole('button', { name: 'branch_list.edit_for_x.release' }));
    const dialog = screen.getByRole('dialog');
    await user.click(
      within(dialog).getByRole('radio', {
        name: /new_code_definition.specific_setting.previous_version.label/,
      }),
    );
    await user.click(within(dialog).getByRole('button', { name: 'save' }));

    expect(await within(dialog).findByRole('button', { name: 'save' })).toBeEnabled();
    expect(dialog).toBeInTheDocument();

    await user.click(within(dialog).getByRole('button', { name: 'cancel' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(
      within(await rowOf('release')).getByText('branch_list.default_setting'),
    ).toBeInTheDocument();
  });

  it('resets a branch to the default, and edits one that has its own setting', async () => {
    const user = userEvent.setup();
    renderSection();

    await user.click(
      await screen.findByRole('button', { name: 'branch_list.show_actions_for_x.feature' }),
    );
    await user.click(screen.getByRole('menuitem', { name: 'reset_to_default' }));
    expect(
      await within(await rowOf('feature')).findByText('branch_list.default_setting'),
    ).toBeInTheDocument();

    newCodeHandler.reset();
    await user.click(await screen.findByRole('button', { name: 'branch_list.edit_for_x.feature' }));
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('offers the edit of a branch with its own setting from its menu', async () => {
    const user = userEvent.setup();
    renderSection();

    await user.click(
      await screen.findByRole('button', { name: 'branch_list.show_actions_for_x.feature' }),
    );
    await user.click(screen.getByRole('menuitem', { name: 'edit' }));

    expect(
      screen.getByRole('heading', { name: 'baseline.new_code_period_for_branch_x.feature' }),
    ).toBeInTheDocument();
  });

  it('disables the reset while the project setting is not compliant', async () => {
    const user = userEvent.setup();
    renderSection(mockNewCodePeriod({ type: NewCodeDefinitionType.NumberOfDays, value: '365' }));

    await user.click(
      await screen.findByRole('button', { name: 'branch_list.show_actions_for_x.feature' }),
    );

    expect(screen.getByRole('menuitem', { name: 'reset_to_default' })).toHaveAttribute(
      'aria-disabled',
      'true',
    );
  });

  it('shows nothing when the project has no branches, or the settings cannot be read', async () => {
    const { container, unmount } = renderSection(mockNewCodePeriod(), []);
    expect(await screen.findByRole('heading')).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
    unmount();

    jest.mocked(listBranchesNewCodeDefinition).mockRejectedValueOnce(new Error('forbidden'));
    renderSection();
    expect(await screen.findByRole('heading')).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
    expect(container).toBeEmptyDOMElement();
  });

  it('shows every branch at its default when the project answers no branch settings', async () => {
    jest.mocked(listBranchesNewCodeDefinition).mockResolvedValueOnce({ newCodePeriods: [] });
    jest.mocked(listBranchesNewCodeDefinition).mockResolvedValueOnce(
      {} as Awaited<ReturnType<typeof listBranchesNewCodeDefinition>>,
    );
    const { rerender } = renderSection();

    expect(
      within(await rowOf('feature')).getByText('branch_list.default_setting'),
    ).toBeInTheDocument();

    // A new branch list refetches the settings, showing the spinner meanwhile.
    rerender(
      <BranchListSection
        branchList={branchList()}
        component={mockComponent()}
        globalNewCodeDefinition={mockNewCodePeriod()}
        projectNewCodeDefinition={mockNewCodePeriod()}
      />,
    );
    expect(
      within(await rowOf('release')).getByText('branch_list.default_setting'),
    ).toBeInTheDocument();
  });

  it('names a setting it has no words for by its type', async () => {
    newCodeHandler.setListBranchesNewCode([
      mockNewCodePeriodBranch({ branchKey: 'feature', type: NewCodeDefinitionType.Inherited }),
      mockNewCodePeriodBranch({ branchKey: 'release', type: undefined, value: '1' }),
    ]);
    renderSection();

    expect(within(await rowOf('feature')).getByText('INHERITED')).toBeInTheDocument();
    expect(
      within(await rowOf('release')).getByText('new_code_definition.previous_version'),
    ).toBeInTheDocument();
  });

  it('offers no reference branch when the project has only the one branch', async () => {
    const user = userEvent.setup();
    renderSection(mockNewCodePeriod(), [mockMainBranch({ name: 'main' })]);

    await user.click(await screen.findByRole('button', { name: 'branch_list.edit_for_x.main' }));
    const dialog = screen.getByRole('dialog');
    await user.click(
      last(
        within(dialog).getAllByRole('radio', {
          name: /new_code_definition.specific_setting.reference_branch.label/,
        }),
      ) as HTMLElement,
    );

    expect(within(dialog).getByRole('button', { name: 'save' })).toBeDisabled();
  });

  it('saves nothing when the form is submitted before a setting is chosen', async () => {
    const user = userEvent.setup();
    renderSection();

    await user.click(await screen.findByRole('button', { name: 'branch_list.edit_for_x.release' }));
    const dialog = screen.getByRole('dialog');
    jest.mocked(setNewCodeDefinition).mockClear();
    fireEvent.submit(dialog.querySelector('form') as HTMLFormElement);

    expect(setNewCodeDefinition).not.toHaveBeenCalled();
    expect(within(dialog).getByRole('button', { name: 'save' })).toBeDisabled();
    expect(dialog).toBeInTheDocument();
  });

  it.each([
    ['saved', (settle: Settle) => settle.resolve()],
    ['refused', (settle: Settle) => settle.reject(new Error('refused'))],
  ])('shows no outcome for a save %s after the list is gone', async (_, finish) => {
    const user = userEvent.setup();
    const settle = {} as Settle;
    jest.mocked(setNewCodeDefinition).mockReturnValueOnce(
      new Promise<void>((resolve, reject) => {
        settle.resolve = resolve;
        settle.reject = reject;
      }),
    );
    const { unmount } = renderSection();

    await user.click(await screen.findByRole('button', { name: 'branch_list.edit_for_x.release' }));
    const dialog = screen.getByRole('dialog');
    await user.click(
      within(dialog).getByRole('radio', {
        name: /new_code_definition.specific_setting.previous_version.label/,
      }),
    );
    await user.click(within(dialog).getByRole('button', { name: 'save' }));
    unmount();
    finish(settle);

    await waitFor(() => {
      expect(setNewCodeDefinition).toHaveBeenCalled();
    });
    expect(screen.queryByText('project_baseline.update_success')).not.toBeInTheDocument();
  });

  it('keeps the list as it is when a reset lands after its branch left the list', async () => {
    const user = userEvent.setup();
    let finishReset: (() => void) | undefined;
    jest.mocked(resetNewCodeDefinition).mockReturnValueOnce(
      new Promise<void>((resolve) => {
        finishReset = resolve;
      }),
    );
    const { rerender } = renderSection();

    await user.click(
      await screen.findByRole('button', { name: 'branch_list.show_actions_for_x.feature' }),
    );
    await user.click(screen.getByRole('menuitem', { name: 'reset_to_default' }));
    rerender(
      <BranchListSection
        branchList={[mockMainBranch({ name: 'main' }), mockBranch({ name: 'release' })]}
        component={mockComponent()}
        globalNewCodeDefinition={mockNewCodePeriod({ type: NewCodeDefinitionType.NumberOfDays })}
        projectNewCodeDefinition={mockNewCodePeriod()}
      />,
    );
    await rowOf('release');
    finishReset?.();

    await waitFor(() => {
      expect(resetNewCodeDefinition).toHaveBeenCalledWith({ project: 'my-project', branch: 'feature' });
    });
    expect(screen.queryByRole('cell', { name: /^feature/ })).not.toBeInTheDocument();
    expect(await rowOf('release')).toBeInTheDocument();
  });

  it('announces branches whose number of days was brought into compliance', async () => {
    const user = userEvent.setup();
    newCodeHandler.setListBranchesNewCode([
      mockNewCodePeriodBranch({
        branchKey: 'feature',
        type: NewCodeDefinitionType.NumberOfDays,
        value: '90',
        previousNonCompliantValue: '120',
        updatedAt: 1_700_000_000_000,
      }),
    ]);
    renderSection();

    expect(
      await screen.findByText(/new_code_definition\.auto_update\.branch\.message/),
    ).toBeInTheDocument();

    await user.click(
      await screen.findByRole('button', { name: 'branch_list.show_actions_for_x.feature' }),
    );
    await user.click(screen.getByRole('menuitem', { name: 'edit' }));
    const dialog = screen.getByRole('dialog');
    await user.click(
      within(dialog).getByRole('radio', {
        name: /new_code_definition.specific_setting.previous_version.label/,
      }),
    );
    await user.click(within(dialog).getByRole('button', { name: 'save' }));

    expect(
      await within(await rowOf('feature')).findByText('new_code_definition.previous_version'),
    ).toBeInTheDocument();
    expect(
      screen.queryByText(/new_code_definition\.auto_update\.branch\.message/),
    ).not.toBeInTheDocument();
  });
});
