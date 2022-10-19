import React from 'react';
import { I18nProvider } from '@osd/i18n/react';
import { HashRouter, Route, Switch } from 'react-router-dom';
import { ChromeBreadcrumb, CoreStart, Toast } from '../../../../src/core/public';
import { NavigationPublicPluginStart } from '../../../../src/plugins/navigation/public';
import DSLService from '../services/dsl';
import { Home as QueryExplorerHome } from './querqy_explorer/home';
import { Home as RulesManager } from './rules_manager/home';
import { Home as QueryCompareHome } from './query_compare/home';
import { PLUGIN_COMPARE_URL, PLUGIN_EXPLORER_URL, PLUGIN_RULES_MANAGER_URL } from '../../common';
import { useState } from 'react';
import { EuiGlobalToastList } from '@elastic/eui';
import {PLUGIN_NAME} from '../../common'

interface SearchRelevanceAppDeps {
  notifications: CoreStart['notifications'];
  http: CoreStart['http'];
  navigation: NavigationPublicPluginStart;
  dslService: DSLService;
  chrome: CoreStart['chrome'];
}

export const SearchRelevanceApp = ({
  notifications,
  http,
  navigation,
  dslService,
  chrome,
}: SearchRelevanceAppDeps) => {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const [toastRightSide, setToastRightSide] = useState<boolean>(true);

  // Render the application DOM.
  // Note that `navigation.ui.TopNavMenu` is a stateful component exported on the `navigation` plugin's start contract.
  const parentBreadCrumbs = [{ text: PLUGIN_NAME, href: '#' }];
  const setToast = (title: string, color = 'success', text?: ReactChild, side?: string) => {
    if (!text) text = '';
    setToastRightSide(!side ? true : false);
    setToasts([...toasts, { id: new Date().toISOString(), title, text, color } as Toast]);
  };
  return (
    <HashRouter>
      <I18nProvider>
        <>
          <EuiGlobalToastList
            toasts={toasts}
            dismissToast={(removedToast) => {
              setToasts(toasts.filter((toast) => toast.id !== removedToast.id));
            }}
            side={toastRightSide ? 'right' : 'left'}
            toastLifeTimeMs={6000}
          />
          <Switch>
            <Route
              path={['/', `${PLUGIN_COMPARE_URL}`]}
              render={(props) => {
                return (
                  <QueryCompareHome
                    parentBreadCrumbs={parentBreadCrumbs}
                    notifications={notifications}
                    http={http}
                    navigation={navigation}
                    setBreadcrumbs={chrome.setBreadcrumbs}
                    dslService={dslService}
                    setToast={setToast}
                    chrome={chrome}
                  />
                );
              }}
            />
          </Switch>
        </>
      </I18nProvider>
    </HashRouter>
  );
};
