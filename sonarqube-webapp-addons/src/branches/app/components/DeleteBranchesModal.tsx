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

import { Button, ButtonVariety, MessageCallout } from '@sonarsource/echoes-react';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { Modal } from '~design-system';
import { isPullRequest } from '~shared/helpers/branch-like';
import { getBranchLikeDisplayName, getBranchLikeKey } from '~sq-server-commons/helpers/branch-like';
import { useDeletBranchMutation } from '~sq-server-commons/queries/branch';
import { BranchLike } from '~sq-server-commons/types/branch-like';
import { Component } from '~sq-server-commons/types/types';

export interface DeleteBranchesModalProps {
  branchLikes: BranchLike[];
  component: Component;
  onClose: () => void;
  onDone: (notDeleted: BranchLike[]) => void;
}

const FORM_ID = 'confirm-branches-delete-form';

const NAME_SEPARATOR = ', ';

export default function DeleteBranchesModal(props: Readonly<DeleteBranchesModalProps>) {
  const { branchLikes, component, onClose, onDone } = props;
  const { mutateAsync: deleteBranchLike } = useDeletBranchMutation();
  const [pending, setPending] = React.useState(false);
  const [failed, setFailed] = React.useState<BranchLike[]>([]);

  const handleSubmit = async (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    setPending(true);
    const results = await Promise.allSettled(
      branchLikes.map((branchLike) => deleteBranchLike({ branchLike, component })),
    );
    const notDeleted = branchLikes.filter((_, index) => results[index].status === 'rejected');
    setPending(false);
    setFailed(notDeleted);
    onDone(notDeleted);
  };

  const titleId = branchLikes.some((branchLike) => isPullRequest(branchLike))
    ? 'branch_analysis.delete_selected.title.pull_requests'
    : 'branch_analysis.delete_selected.title';

  return (
    <Modal
      headerTitle={<FormattedMessage id={titleId} values={{ count: branchLikes.length }} />}
      body={
        <form id={FORM_ID} onSubmit={handleSubmit}>
          {failed.length > 0 && (
            <MessageCallout className="sw-mb-4" variety="danger">
              <FormattedMessage
                id="branch_analysis.delete_selected.failed"
                values={{ names: failed.map((branchLike) => getBranchLikeDisplayName(branchLike)).join(NAME_SEPARATOR) }}
              />
            </MessageCallout>
          )}
          <p>
            <FormattedMessage id="branch_analysis.delete_selected.are_you_sure" />
          </p>
          <ul className="sw-mt-2 sw-list-disc sw-pl-6">
            {branchLikes.map((branchLike) => (
              <li key={getBranchLikeKey(branchLike)}>{getBranchLikeDisplayName(branchLike)}</li>
            ))}
          </ul>
        </form>
      }
      loading={pending}
      primaryButton={
        <Button type="submit" form={FORM_ID} variety={ButtonVariety.Danger}>
          <FormattedMessage id="delete" />
        </Button>
      }
      secondaryButtonLabel={<FormattedMessage id="cancel" />}
      onClose={onClose}
    />
  );
}
