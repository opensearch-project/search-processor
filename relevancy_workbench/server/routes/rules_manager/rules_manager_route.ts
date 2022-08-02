import { schema } from '@osd/config-schema';
import { BASE_RELEVENCY_WORKBENCH_API, REWRITER_TYPE_MAP } from '../../../common';
import {
  ILegacyScopedClusterClient,
  IOpenSearchDashboardsResponse,
  IRouter,
  ResponseError,
} from '../../../../../src/core/server';
import { RulesManagerAdaptor } from '../../adaptors/rules_manager/rules_manager_adaptor';
import _ from 'lodash';

export function RulesManagerRouter(router: IRouter) {
  const rulesManager = new RulesManagerAdaptor();
  router.get(
    {
      path: BASE_RELEVENCY_WORKBENCH_API.RULES_MANAGER,
      validate: false,
    },
    async (
      context,
      request,
      response
    ): Promise<IOpenSearchDashboardsResponse<any | ResponseError>> => {
      const opensearchRelevancyClient: ILegacyScopedClusterClient = context.relevancy_plugin.relevancyWorkbenchClient.asScoped(
        request
      );
      console.log('reached the ROUTER @@@@@@@@@@@@@@@@@@@@@@@@@');
      try {
        const rewriterList = await rulesManager.fetchAllRewriters(opensearchRelevancyClient);
        return response.ok({
          body: {
            rewriters: rewriterList,
          },
        });
      } catch (error: any) {
        console.error('Issue in fetching rewriter list:', error);
        return response.custom({
          statusCode: error.statusCode || 500,
          body: error.message,
        });
      }
    }
  );

  router.get(
    {
      path: `${BASE_RELEVENCY_WORKBENCH_API.RULES_MANAGER}/{rewriterId}`,
      validate: {
        params: schema.object({
          rewriterId: schema.string(),
        }),
      },
    },
    async (
      context,
      request,
      response
    ): Promise<IOpenSearchDashboardsResponse<any | ResponseError>> => {
      const opensearchRelevancyClient: ILegacyScopedClusterClient = context.relevancy_plugin.relevancyWorkbenchClient.asScoped(
        request
      );
      console.log('reached the GET ID ROUTER @@@@@@@@@@@@@@@@@@@@@@@@@');
      try {
        const rewriter = await rulesManager.fetchRewriterById(
          opensearchRelevancyClient,
          request.params.rewriterId
        );
        return response.ok({
          body: {
            ...rewriter,
            class: _.findKey(REWRITER_TYPE_MAP, (v) => v === rewriter.class),
          },
        });
      } catch (error: any) {
        console.error('Issue in fetching rewriter list:', error);
        return response.custom({
          statusCode: error.statusCode || 500,
          body: error.message,
        });
      }
    }
  );

  router.put(
    {
      path: BASE_RELEVENCY_WORKBENCH_API.RULES_MANAGER,
      validate: {
        body: schema.object({
          rewriterId: schema.string(),
          rewriterType: schema.string(),
          config: schema.any(),
        }),
      },
    },
    async (
      context,
      request,
      response
    ): Promise<IOpenSearchDashboardsResponse<any | ResponseError>> => {
      const opensearchRelevancyClient: ILegacyScopedClusterClient = context.relevancy_plugin.relevancyWorkbenchClient.asScoped(
        request
      );
      console.log('reached the create ROUTER @@@@@@@@@@@@@@@@@@@@@@@@@');
      try {
        const newRewriterObject = {
          class: REWRITER_TYPE_MAP[request.body.rewriterType],
          config: request.body.config,
        };
        const createResponse = await rulesManager.createRewriter(
          opensearchRelevancyClient,
          request.body.rewriterId,
          newRewriterObject
        );
        return response.ok({
          body: createResponse,
        });
      } catch (error: any) {
        console.error('Issue in fetching rewriter list:', error);
        return response.custom({
          statusCode: error.statusCode || 500,
          body: error.message,
        });
      }
    }
  );

  router.post(
    {
      path: BASE_RELEVENCY_WORKBENCH_API.RULES_MANAGER_CREATE,
      validate: {
        body: schema.object({
          rewriterId: schema.string(),
          rewriterType: schema.string(),
        }),
      },
    },
    async (
      context,
      request,
      response
    ): Promise<IOpenSearchDashboardsResponse<any | ResponseError>> => {
      const opensearchRelevancyClient: ILegacyScopedClusterClient = context.relevancy_plugin.relevancyWorkbenchClient.asScoped(
        request
      );
      console.log('reached the create ROUTER @@@@@@@@@@@@@@@@@@@@@@@@@');
      try {
        const newRewriterObject = {
          class: REWRITER_TYPE_MAP[request.body.rewriterType],
          config: {
            rules: '',
          },
        };
        const createResponse = await rulesManager.createRewriter(
          opensearchRelevancyClient,
          request.body.rewriterId,
          newRewriterObject
        );
        return response.ok({
          body: createResponse,
        });
      } catch (error: any) {
        console.error('Issue in creating new rewriter:', error);
        return response.custom({
          statusCode: error.statusCode || 500,
          body: error.message,
        });
      }
    }
  );

  router.put(
    {
      path: BASE_RELEVENCY_WORKBENCH_API.RULES_MANAGER_RENAME,
      validate: {
        body: schema.object({
          currentRewriterId: schema.string(),
          newRewriterId: schema.string(),
        }),
      },
    },
    async (
      context,
      request,
      response
    ): Promise<IOpenSearchDashboardsResponse<any | ResponseError>> => {
      const opensearchRelevancyClient: ILegacyScopedClusterClient = context.relevancy_plugin.relevancyWorkbenchClient.asScoped(
        request
      );
      console.log('reached the RENAME ROUTER @@@@@@@@@@@@@@@@@@@@@@@@@');
      try {
        const renameResponse = await rulesManager.renameRewriter(
          opensearchRelevancyClient,
          request.body.currentRewriterId,
          request.body.newRewriterId
        );
        return response.ok({
          body: renameResponse,
        });
      } catch (error: any) {
        console.error('Issue in renaming rewriter list:', error);
        return response.custom({
          statusCode: error.statusCode || 500,
          body: error.message,
        });
      }
    }
  );

  router.post(
    {
      path: BASE_RELEVENCY_WORKBENCH_API.RULES_MANAGER_CLONE,
      validate: {
        body: schema.object({
          currentRewriterId: schema.string(),
          newRewriterId: schema.string(),
        }),
      },
    },
    async (
      context,
      request,
      response
    ): Promise<IOpenSearchDashboardsResponse<any | ResponseError>> => {
      const opensearchRelevancyClient: ILegacyScopedClusterClient = context.relevancy_plugin.relevancyWorkbenchClient.asScoped(
        request
      );
      console.log('reached the CLONE ROUTER @@@@@@@@@@@@@@@@@@@@@@@@@');
      try {
        const cloneResponse = await rulesManager.cloneRewriter(
          opensearchRelevancyClient,
          request.body.currentRewriterId,
          request.body.newRewriterId
        );
        return response.ok({
          body: cloneResponse,
        });
      } catch (error: any) {
        console.error('Issue in cloning rewriter list:', error);
        return response.custom({
          statusCode: error.statusCode || 500,
          body: error.message,
        });
      }
    }
  );

  router.delete(
    {
      path: `${BASE_RELEVENCY_WORKBENCH_API.RULES_MANAGER}/{rewriterId}`,
      validate: {
        params: schema.object({
          rewriterId: schema.string(),
        }),
      },
    },
    async (
      context,
      request,
      response
    ): Promise<IOpenSearchDashboardsResponse<any | ResponseError>> => {
      const opensearchRelevancyClient: ILegacyScopedClusterClient = context.relevancy_plugin.relevancyWorkbenchClient.asScoped(
        request
      );
      console.log('reached the DELETE ROUTER @@@@@@@@@@@@@@@@@@@@@@@@@');
      try {
        const deleteResponse = await rulesManager.deleteRewriter(
          opensearchRelevancyClient,
          request.params.rewriterId
        );
        return response.ok({
          body: deleteResponse,
        });
      } catch (error: any) {
        console.error('Issue in deleting rewriter:', error);
        return response.custom({
          statusCode: error.statusCode || 500,
          body: error.message,
        });
      }
    }
  );
}
