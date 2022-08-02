import { i18n } from '@osd/i18n';
import { AppMountParameters, CoreSetup, CoreStart, Plugin } from '../../../src/core/public';
import {
  RelevancyWorkbenchPluginSetup,
  RelevancyWorkbenchPluginStart,
  AppPluginStartDependencies,
} from './types';
import { PLUGIN_NAME } from '../common';
import DSLService from './services/dsl';

export class RelevancyWorkbenchPlugin
  implements Plugin<RelevancyWorkbenchPluginSetup, RelevancyWorkbenchPluginStart> {
  public setup(core: CoreSetup): RelevancyWorkbenchPluginSetup {
    // Register an application into the side navigation menu
    core.application.register({
      id: 'relevancyWorkbench',
      title: PLUGIN_NAME,
      category: {
        id: 'opensearch',
        label: 'OpenSearch Plugins',
        order: 2000,
      },
      async mount(params: AppMountParameters) {
        // Load application bundle
        const { renderApp } = await import('./application');
        // Get start services as specified in opensearch_dashboards.json
        const [coreStart, depsStart] = await core.getStartServices();
        const dslService = new DSLService(coreStart.http);
        // Render the application
        return renderApp(coreStart, depsStart as AppPluginStartDependencies, dslService, params);
      },
    });

    // Return methods that should be available to other plugins
    return {
      getGreeting() {
        return i18n.translate('relevancyWorkbench.greetingText', {
          defaultMessage: 'Hello from {name}!',
          values: {
            name: PLUGIN_NAME,
          },
        });
      },
    };
  }

  public start(core: CoreStart): RelevancyWorkbenchPluginStart {
    return {};
  }

  public stop() {}
}
