import branches from './branches';
import * as entitlements from './feature-license/entitlements';

/**
 * The addon registry the SonarQube webapp loads: the branch and pull request UI, and the
 * entitlement hooks, which always answer "not entitled" in this community build.
 */
export const addons = { branches, entitlements };
