/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

import { IRouter } from '../../../../src/core/server';
import { schema } from '@osd/config-schema';
import { RequestParams } from '@opensearch-project/opensearch';

export function registerDslRoute({ router }: { router: IRouter }) {
  router.post(
    {
      path: `/api/relevancy/search`,
      validate: { body: schema.any() },
    },
    async (context, request, response) => {
      const { index, size, ...rest } = request.body;
      const params: RequestParams.Search = {
        index: index,
        size,
        body: rest,
      };
      try {
        const resp = await context.core.opensearch.legacy.client.callAsCurrentUser(
          'search',
          params
        );
        return response.ok({
          body: resp,
        });
      } catch (error) {
        if (error.statusCode !== 404) console.error(error);
        return response.custom({
          statusCode: error.statusCode || 500,
          body: error.message,
        });
      }
    }
  );
}
