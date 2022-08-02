import {
  PluginInitializerContext,
  CoreSetup,
  CoreStart,
  Logger,
  Plugin,
  ILegacyClusterClient,
} from '../../../src/core/server';

import { RelevancyWorkbenchPluginSetup, RelevancyWorkbenchPluginStart } from './types';
import { defineRoutes } from './routes';
import { OpenSearchQuerqyPlugin } from './opensearch_querqy_plugin';

export class RelevancyWorkbenchPlugin
  implements Plugin<RelevancyWorkbenchPluginSetup, RelevancyWorkbenchPluginStart> {
  private readonly logger: Logger;
  constructor(initializerContext: PluginInitializerContext) {
    this.logger = initializerContext.logger.get();
  }

  public setup(core: CoreSetup) {
    this.logger.debug('RelevancyWorkbench: Setup');
    const router = core.http.createRouter();

    const opensearchRevelancyWorkbenchClient: ILegacyClusterClient = core.opensearch.legacy.createClient(
      'opensearch_relevancy_workbench',
      {
        plugins: [OpenSearchQuerqyPlugin],
      }
    );

    // @ts-ignore
    core.http.registerRouteHandlerContext('relevancy_plugin', (context, request) => {
      return {
        logger: this.logger,
        relevancyWorkbenchClient: opensearchRevelancyWorkbenchClient,
      };
    });

    // Register server side APIs
    defineRoutes({ router, client: opensearchRevelancyWorkbenchClient });

    return {};
  }

  public start(core: CoreStart) {
    this.logger.debug('RelevancyWorkbench: Started');
    return {};
  }

  public stop() {}
}
