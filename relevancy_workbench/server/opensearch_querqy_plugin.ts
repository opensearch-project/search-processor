/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

import { OPENSEARCH_RELEVANCY_API } from '../common/index';

export function OpenSearchQuerqyPlugin(Client: any, config: any, components: any) {
  const clientAction = components.clientAction.factory;

  Client.prototype.querqy = components.clientAction.namespaceFactory();
  const querqy = Client.prototype.querqy.prototype;

  // Get Rewiter
  querqy.getRewriter = clientAction({
    url: {
      fmt: OPENSEARCH_RELEVANCY_API.REWRITER,
    },
    method: 'GET',
  });

  // Get Rewiter by Id
  querqy.getRewriterById = clientAction({
    url: {
      fmt: `${OPENSEARCH_RELEVANCY_API.REWRITER}/<%=rewriterId%>`,
      req: {
        rewriterId: {
          type: 'string',
          required: true,
        },
      },
    },
    method: 'GET',
  });

  // Create new Rewiter
  querqy.createRewriter = clientAction({
    url: {
      fmt: `${OPENSEARCH_RELEVANCY_API.REWRITER}/<%=rewriterId%>`,
      req: {
        rewriterId: {
          type: 'string',
          required: true,
        },
      },
    },
    method: 'PUT',
    needBody: true,
  });

  // Delete Rewiter by Id
  querqy.deleteRewriter = clientAction({
    url: {
      fmt: `${OPENSEARCH_RELEVANCY_API.REWRITER}/<%=rewriterId%>`,
      req: {
        rewriterId: {
          type: 'string',
          required: true,
        },
      },
    },
    method: 'DELETE',
  });
}
