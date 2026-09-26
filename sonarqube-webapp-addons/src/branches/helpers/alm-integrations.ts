/*
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * Copyright (C) 2026 Haembina
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

import { ComponentBase } from '~shared/types/component';

/** The DevOps platforms a project can be bound to, by the key the server reports. */
export enum AlmType {
    Azure = 'azure',
    Bitbucket = 'bitbucket',
    GitHub = 'github',
    GitLab = 'gitlab',
}

type AlmIntegrationDocLinkKey = AlmType.Bitbucket | AlmType.GitHub | AlmType.GitLab | 'microsoft';

/**
 * The documentation link key for the platform a component is bound to, or `undefined`
 * when the component has no binding.
 */
export function getComponentAlmKey(component: ComponentBase): AlmIntegrationDocLinkKey | undefined {
    return component.alm && sanitizeAlmId(component.alm.key);
}

/**
 * Normalizes a platform key to a documentation link key: any `bitbucket*` key becomes
 * `bitbucket`, any `azure*` key becomes `microsoft`, and other keys pass through unchecked.
 */
export function sanitizeAlmId(almKey: string): AlmIntegrationDocLinkKey {
    if (isBitbucket(almKey)) {
        return AlmType.Bitbucket;
    }

    if (isAzure(almKey)) {
        return 'microsoft';
    }

    return almKey as AlmIntegrationDocLinkKey;
}

/** Whether the key names a Bitbucket platform (Server or Cloud); `false` when absent. */
export function isBitbucket(almKey?: string): boolean {
    return Boolean(almKey?.startsWith('bitbucket'));
}

/** Whether the key names Azure DevOps (`microsoft` or `azure*`); `false` when absent. */
export function isAzure(almKey?: string): boolean {
    return Boolean(almKey === 'microsoft' || almKey?.startsWith('azure'));
}
