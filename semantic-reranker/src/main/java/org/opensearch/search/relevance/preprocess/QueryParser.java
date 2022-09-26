/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.preprocess;

import org.opensearch.index.query.MatchQueryBuilder;
import org.opensearch.index.query.QueryBuilder;

public class QueryParser {

  public QueryParserResult parse(QueryBuilder query) {
    if (query instanceof MatchQueryBuilder) {
      MatchQueryBuilder matchQuery = (MatchQueryBuilder) query;
      return new QueryParserResult(matchQuery.value().toString(), matchQuery.fieldName());
    }
    return null;
  }

  public class QueryParserResult {
    private String queryText;
    private String bodyFieldName;

    public QueryParserResult(String queryText, String bodyFieldName) {
      this.queryText = queryText;
      this.bodyFieldName = bodyFieldName;
    }

    public String getQueryText() {
      return queryText;
    }

    public String getBodyFieldName() {
      return bodyFieldName;
    }
  }
}
