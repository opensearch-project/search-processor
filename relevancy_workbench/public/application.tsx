import React from 'react';
import ReactDOM from 'react-dom';
import { AppMountParameters, CoreStart } from '../../../src/core/public';
import { AppPluginStartDependencies } from './types';
import { RelevancyWorkbenchApp } from './components/app';
import DSLService from './services/dsl';

export const renderApp = (
  { notifications, http, chrome }: CoreStart,
  { navigation }: AppPluginStartDependencies,
  dslService: DSLService,
  { element }: AppMountParameters
) => {
  ReactDOM.render(
    <RelevancyWorkbenchApp
      notifications={notifications}
      http={http}
      navigation={navigation}
      dslService={dslService}
      chrome={chrome}
    />,
    element
  );

  return () => ReactDOM.unmountComponentAtNode(element);
};
