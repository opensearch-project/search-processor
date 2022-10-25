/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.index.query.MatchQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.Constants;

public class QueryParser {
  private static final Logger logger = LogManager.getLogger(QueryParser.class);

  public QueryParserResult parse(final QueryBuilder query, final List<String> bodyFieldSetting) {
    final String bodyFieldFromSetting = bodyFieldSetting.isEmpty() ? null : bodyFieldSetting.get(0);
    QueryParserResult result = null;

    if (query instanceof MatchQueryBuilder) {
      MatchQueryBuilder matchQuery = (MatchQueryBuilder) query;
      if (bodyFieldFromSetting != null && !bodyFieldFromSetting.equals(matchQuery.fieldName())) {
        logger.warn("Body field in query [" + matchQuery.fieldName() + "] is different from body field setting [" +
            bodyFieldFromSetting + "]. Will not apply Kendra Intelligent Ranking");
      } else {
        result = new QueryParserResult(matchQuery.value().toString(), matchQuery.fieldName());
      }
    } else {
      logger.warn(Constants.KENDRA_INTELLIGENT_RANKING + " does not support query type [" +
          query.queryName() + "]. Will not apply Kendra Intelligent Ranking.");
    }
    return result;
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
