import { NavigationPublicPluginStart } from '../../../src/plugins/navigation/public';

export interface RelevancyWorkbenchPluginSetup {
  getGreeting: () => string;
}
// eslint-disable-next-line @typescript-eslint/no-empty-interface
export interface RelevancyWorkbenchPluginStart {}

export interface AppPluginStartDependencies {
  navigation: NavigationPublicPluginStart;
}
