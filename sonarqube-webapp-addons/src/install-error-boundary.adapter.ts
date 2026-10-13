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

let installed = false;

function describe(reason: unknown): string {
  if (reason instanceof Error) {
    return reason.message;
  }

  if (typeof reason === 'string') {
    return reason;
  }

  return JSON.stringify(reason) ?? typeof reason;
}

function show(reason: unknown) {
  toast.error({ title: translate('default_error_message'), description: describe(reason) });
}

export function installErrorBoundary() {
  if (installed) {
    return;
  }

  installed = true;
  window.addEventListener('error', (event) => show(event.error ?? event.message));
  window.addEventListener('unhandledrejection', (event) => {
    if (!(event.reason instanceof Response)) {
      show(event.reason);
    }
  });
}
