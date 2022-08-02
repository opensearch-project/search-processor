import './index.scss';

import { RelevancyWorkbenchPlugin } from './plugin';

// This exports static code and TypeScript types,
// as well as, OpenSearch Dashboards Platform `plugin()` initializer.
export function plugin() {
  return new RelevancyWorkbenchPlugin();
}
export { RelevancyWorkbenchPluginSetup, RelevancyWorkbenchPluginStart } from './types';
