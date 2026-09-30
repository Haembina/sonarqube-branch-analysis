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
 *
 */
import { toast } from '@sonarsource/echoes-react';
import { translate } from '~sq-server-commons/helpers/l10n';
import { installErrorBoundary } from './error-boundary';

jest.mock('@sonarsource/echoes-react', () => ({
  ...jest.requireActual('@sonarsource/echoes-react'),
  toast: { error: jest.fn() },
}));

/** Fires a rejection nothing handled, as the browser reports one. */
function reject(reason: unknown) {
  window.dispatchEvent(Object.assign(new Event('unhandledrejection'), { reason }));
}

beforeAll(() => {
  // A second call is a no-op, so each event below is shown once.
  installErrorBoundary();
  installErrorBoundary();
});

beforeEach(() => {
  jest.mocked(toast.error).mockClear();
});

it('shows an uncaught error as a toast naming it', () => {
  window.dispatchEvent(new ErrorEvent('error', { error: new Error('render failed') }));

  expect(toast.error).toHaveBeenCalledTimes(1);
  expect(toast.error).toHaveBeenCalledWith({
    title: translate('default_error_message'),
    description: 'render failed',
  });
});

it('shows the event message when the error carries no object', () => {
  window.dispatchEvent(new ErrorEvent('error', { message: 'Script error.' }));

  expect(toast.error).toHaveBeenCalledWith(expect.objectContaining({ description: 'Script error.' }));
});

it('shows a rejected promise nothing handled', () => {
  reject('branch list unavailable');

  expect(toast.error).toHaveBeenCalledTimes(1);
  expect(toast.error).toHaveBeenCalledWith(
    expect.objectContaining({ description: 'branch list unavailable' }),
  );
});

it('shows a rejected object as JSON', () => {
  reject({ status: 'broken' });

  expect(toast.error).toHaveBeenCalledWith(
    expect.objectContaining({ description: '{"status":"broken"}' }),
  );
});

it('shows a rejection with no reason by its type', () => {
  reject(undefined);

  expect(toast.error).toHaveBeenCalledWith(expect.objectContaining({ description: 'undefined' }));
});

it('shows no second toast for an API failure the API layer has already shown', () => {
  reject(new Response(null, { status: 500 }));

  expect(toast.error).not.toHaveBeenCalled();
});
