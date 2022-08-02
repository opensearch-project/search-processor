import { ILegacyClusterClient, IRouter } from '../../../../src/core/server';
import DSLFacet from '../services/facets/dsl_facet';
import { registerDslRoute } from './dsl';
import { RulesManagerRouter } from './rules_manager/rules_manager_route';

export function defineRoutes({
  router,
  client,
}: {
  router: IRouter;
  client: ILegacyClusterClient;
}) {
  registerDslRoute({ router, facet: new DSLFacet(client) });
  RulesManagerRouter(router);
}
