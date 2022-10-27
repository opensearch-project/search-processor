/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess;

import static org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.Constants.BODY_FIELD;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.index.query.MatchQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.Constants;

public class QueryParser {
  private static final Logger logger = LogManager.getLogger(QueryParser.class);

  private static final String FILED_MISMATCH_ERROR_MESSAGE =
      "Mismatch: Field configured in %s property [%s] is not present in query fields [%s]. Will not apply Kendra Intelligent Ranking.";

  public QueryParserResult parse(
      final QueryBuilder query,
      final List<String> bodyFieldSetting,
      final List<String> titleFieldSetting) {
    final String bodyFieldFromSetting = bodyFieldSetting.isEmpty() ? null : bodyFieldSetting.get(0);
    final String titleFieldFromSetting = titleFieldSetting.isEmpty() ? null : titleFieldSetting.get(0);
    QueryParserResult result = null;

    if (query instanceof MatchQueryBuilder) {
      result = parseMatchQuery((MatchQueryBuilder) query, bodyFieldFromSetting);
    } else if (query instanceof MultiMatchQueryBuilder) {
      result = parseMultiMatchQuery((MultiMatchQueryBuilder) query, bodyFieldFromSetting, titleFieldFromSetting);
    } else {
      logger.warn(Constants.KENDRA_INTELLIGENT_RANKING + " does not support query type [" +
          query.queryName() + "]. Will not apply Kendra Intelligent Ranking.");
    }
    return result;
  }

  private QueryParserResult parseMatchQuery(MatchQueryBuilder matchQuery, String bodyFieldFromSetting) {
    QueryParserResult result = null;
    if (!bodyFieldFromSetting.equals(matchQuery.fieldName())) {
      logger.warn(String.format(Locale.ENGLISH, FILED_MISMATCH_ERROR_MESSAGE, BODY_FIELD,
          bodyFieldFromSetting, matchQuery.fieldName()));
    } else {
      result = new QueryParserResult(matchQuery.value().toString(), bodyFieldFromSetting);
    }
    return result;
  }

  private QueryParserResult parseMultiMatchQuery(MultiMatchQueryBuilder multiMatchQuery,
      String bodyFieldFromSetting, String titleFieldFromSetting) {

    QueryParserResult result = null;
    boolean configuredBodyFieldPresentInQuery = false;
    boolean configuredTitleFieldPresentInQuery = false;
    for (String field : multiMatchQuery.fields().keySet()) {
      Pattern fieldNameRegex = Pattern.compile(createRegexFromGlob(field));
      if (fieldNameRegex.matcher(bodyFieldFromSetting).matches()) {
        configuredBodyFieldPresentInQuery = true;
      }
      if (titleFieldFromSetting != null && fieldNameRegex.matcher(titleFieldFromSetting).matches()) {
        configuredTitleFieldPresentInQuery = true;
      }
    }
    if (!configuredBodyFieldPresentInQuery) {
      logger.warn(String.format(Locale.ENGLISH, FILED_MISMATCH_ERROR_MESSAGE, BODY_FIELD,
          bodyFieldFromSetting, multiMatchQuery.fields().keySet()));
    } else {
      final String titleFieldToUse = configuredTitleFieldPresentInQuery ? titleFieldFromSetting : null;
      result = new QueryParserResult(multiMatchQuery.value().toString(), bodyFieldFromSetting, titleFieldToUse);
    }
    return result;
  }

  private static String createRegexFromGlob(String glob) {
    StringBuilder out = new StringBuilder("^");
    for(int i = 0; i < glob.length(); ++i) {
      final char c = glob.charAt(i);
      switch(c) {
        case '*': out.append(".*"); break;
        case '?': out.append('.'); break;
        case '.': out.append("\\."); break;
        case '\\': out.append("\\\\"); break;
        default: out.append(c);
      }
    }
    out.append('$');
    return out.toString();
  }

  public class QueryParserResult {
    private String queryText;
    private String bodyFieldName;
    private String titleFieldName;

    public QueryParserResult(String queryText, String bodyFieldName) {
      this.queryText = queryText;
      this.bodyFieldName = bodyFieldName;
      this.titleFieldName = null;
    }

    public QueryParserResult(String queryText, String bodyFieldName, String titleFieldName) {
      this.queryText = queryText;
      this.bodyFieldName = bodyFieldName;
      this.titleFieldName = titleFieldName;
    }

    public String getQueryText() {
      return queryText;
    }

    public String getBodyFieldName() {
      return bodyFieldName;
    }

    public String getTitleFieldName() {
      return titleFieldName;
    }
  }
}
