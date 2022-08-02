import { PluginInitializerContext } from '../../../src/core/server';
import { RelevancyWorkbenchPlugin } from './plugin';

// This exports static code and TypeScript types,
// as well as, OpenSearch Dashboards Platform `plugin()` initializer.

export function plugin(initializerContext: PluginInitializerContext) {
  return new RelevancyWorkbenchPlugin(initializerContext);
}

export { RelevancyWorkbenchPluginSetup, RelevancyWorkbenchPluginStart } from './types';
