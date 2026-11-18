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

import { sortBy } from 'lodash';
import { useLayoutEffect, useRef, useState } from 'react';
import { useIntl } from 'react-intl';
import { Languages } from '~shared/types/languages';
import Tooltip from '~sq-server-commons/components/controls/Tooltip';
import withLanguages from '~sq-server-commons/context/languages/withLanguages';

export interface ProjectCardLanguagesProps {
  distribution?: string;
  languages: Languages;
}

const UNKNOWN_LANGUAGE = '<null>';

export function ProjectCardLanguages({ distribution, languages }: Readonly<ProjectCardLanguagesProps>) {
  const { formatMessage } = useIntl();
  const line = useRef<HTMLSpanElement>(null);
  const [isCut, setIsCut] = useState(false);

  useLayoutEffect(() => {
    const measure = () => setIsCut(line.current !== null && line.current.scrollWidth > line.current.clientWidth);
    measure();
    window.addEventListener('resize', measure);
    return () => window.removeEventListener('resize', measure);
  }, [distribution]);

  if (distribution === undefined) {
    return null;
  }

  const names = sortBy(
    distribution.split(';').map((item) => item.split('=')),
    ([, lines]) => -Number(lines),
  ).map(([key]) => (key === UNKNOWN_LANGUAGE ? formatMessage({ id: 'unknown' }) : (languages[key]?.name ?? key)));

  const list = (
    <span>
      {names.map((name) => (
        <span key={name}>
          {name}
          <br />
        </span>
      ))}
    </span>
  );

  return (
    <Tooltip content={isCut ? list : null}>
      <span className="sw:block sw:max-w-full sw:truncate" ref={line}>
        {names.join(', ')}
      </span>
    </Tooltip>
  );
}

export default withLanguages(ProjectCardLanguages);
