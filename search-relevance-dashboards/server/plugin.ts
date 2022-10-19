import {
  PluginInitializerContext,
  CoreSetup,
  CoreStart,
  Logger,
  Plugin,
  ILegacyClusterClient,
} from '../../../src/core/server';

import { SearchRelevancePluginSetup, SearchRelevancePluginStart } from './types';

export class SearchRelevancePlugin
  implements Plugin<SearchRelevancePluginSetup, SearchRelevancePluginStart> {
  private readonly logger: Logger;
  constructor(initializerContext: PluginInitializerContext) {
    this.logger = initializerContext.logger.get();
  }

  public setup(core: CoreSetup) {
    this.logger.debug('SearchRelevance: Setup');

    const opensearchSearchRelevanceClient: ILegacyClusterClient = core.opensearch.legacy.createClient(
      'opensearch_search_relevance',
    );

    // @ts-ignore
    core.http.registerRouteHandlerContext('search_relevance_plugin', (context, request) => {
      return {
        logger: this.logger,
        relevancyWorkbenchClient: opensearchSearchRelevanceClient,
      };
    });

    return {};
  }

  public start(core: CoreStart) {
    this.logger.debug('SearchRelevance: Started');
    return {};
  }

  public stop() {}
}
