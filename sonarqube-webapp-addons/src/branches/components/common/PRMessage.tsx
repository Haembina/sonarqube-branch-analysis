/*
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * Copyright (C) 2026 Haembina
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

import { ReactNode } from 'react';
import { IntlShape, useIntl } from 'react-intl';

interface Props {
    almKey: string | undefined;
    children?: (transformedMessage: string) => ReactNode;
    message: string;
}

function getTranslation(intl: IntlShape, key: string, almKey: string | undefined): string {
    const defaultMessage = intl.formatMessage({ id: key });

    if (!almKey) {
        return defaultMessage;
    }

    // Try the ALM-specific translation first (e.g., "pull_request.github").
    // If it doesn't exist, fall back to the generic translation (e.g., "pull_request").
    return intl.formatMessage({
        id: `${key}.${almKey}`,
        defaultMessage,
    });
}

/**
 * Replaces `{pull_request}` and `{pull_requests}` in `message` with the platform's own term
 * (the `pull_request.<almKey>` translation), falling back to the generic term when `almKey` is
 * absent or has no translation. Returns the string, or passes it to `children` when given.
 */
export function PRMessage({ almKey, children, message }: Readonly<Props>) {
    const intl = useIntl();

    const pullRequestTranslation = getTranslation(intl, 'pull_request', almKey);
    const pullRequestsTranslation = getTranslation(intl, 'pull_requests', almKey);

    const transformedMessage = message
        .replaceAll(/\{pull_request\}/gim, pullRequestTranslation)
        .replaceAll(/\{pull_requests\}/gim, pullRequestsTranslation);

    return children ? children(transformedMessage) : transformedMessage;
}
