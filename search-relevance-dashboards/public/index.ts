import './index.scss';

import { SearchRelevancePlugin } from './plugin';

// This exports static code and TypeScript types,
// as well as, OpenSearch Dashboards Platform `plugin()` initializer.
export function plugin() {
  return new SearchRelevancePlugin();
}
export { SearchRelevancePluginSetup, SearchRelevancePluginStart } from './types';
