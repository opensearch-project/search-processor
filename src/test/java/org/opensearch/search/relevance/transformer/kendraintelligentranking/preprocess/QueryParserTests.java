/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.opensearch.index.query.MatchQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess.QueryParser.QueryParserResult;
import org.opensearch.test.OpenSearchTestCase;

public class QueryParserTests extends OpenSearchTestCase {

  private static final String TEST_BODY_FIELD = "bodyField";
  private static final String TEST_TITLE_FIELD = "titleField";
  private static final String TEST_QUERY_TEXT = "queryText";
  private static final String INVALID_FIELD = "invalidField";
  private QueryParser queryParser = new QueryParser();

  public void testParse_Match_RequestFieldMatchesSetting() {
    QueryParser.QueryParserResult queryParserResult = queryParser.parse(
        new MatchQueryBuilder(TEST_BODY_FIELD, TEST_QUERY_TEXT),
        Arrays.asList(TEST_BODY_FIELD),
        Arrays.asList(TEST_TITLE_FIELD));

    performQueryParserResultAssertions(queryParserResult, TEST_QUERY_TEXT, TEST_BODY_FIELD, null);
  }

  public void testParse_Match_RequestFieldDoesNotMatchSetting() {
    QueryParser.QueryParserResult queryParserResult = queryParser.parse(
        new MatchQueryBuilder(TEST_BODY_FIELD, TEST_QUERY_TEXT),
        Arrays.asList(INVALID_FIELD),
        Arrays.asList(TEST_TITLE_FIELD));

    Assert.assertNull(queryParserResult);
  }

  public void testParse_MultiMatch_RequestFieldMatchesSetting() {
    QueryParser.QueryParserResult queryParserResult = queryParser.parse(
        new MultiMatchQueryBuilder(TEST_QUERY_TEXT, TEST_TITLE_FIELD, TEST_BODY_FIELD),
        Arrays.asList(TEST_BODY_FIELD),
        Arrays.asList(TEST_TITLE_FIELD));

    performQueryParserResultAssertions(queryParserResult, TEST_QUERY_TEXT, TEST_BODY_FIELD, TEST_TITLE_FIELD);
  }

  public void testParse_MultiMatch_OnlyBodyFieldMatchesSetting() {
    QueryParser.QueryParserResult queryParserResult = queryParser.parse(
        new MultiMatchQueryBuilder(TEST_QUERY_TEXT, TEST_BODY_FIELD, INVALID_FIELD),
        Arrays.asList(TEST_BODY_FIELD),
        Arrays.asList(TEST_TITLE_FIELD));

    performQueryParserResultAssertions(queryParserResult, TEST_QUERY_TEXT, TEST_BODY_FIELD, null);
  }

  public void testParse_MultiMatch_RequestFieldMatchesSetting_WithFieldBoosting() {
    MultiMatchQueryBuilder multiMatchQueryBuilder = new MultiMatchQueryBuilder(TEST_QUERY_TEXT, TEST_TITLE_FIELD, TEST_BODY_FIELD);
    multiMatchQueryBuilder.field(TEST_BODY_FIELD, 1.5f);
    multiMatchQueryBuilder.field(TEST_TITLE_FIELD, 3f);
    QueryParser.QueryParserResult queryParserResult = queryParser.parse(
        multiMatchQueryBuilder,
        Arrays.asList(TEST_BODY_FIELD),
        Arrays.asList(TEST_TITLE_FIELD));

    performQueryParserResultAssertions(queryParserResult, TEST_QUERY_TEXT, TEST_BODY_FIELD, TEST_TITLE_FIELD);
  }

  public void testParse_MultiMatch_RequestFieldMatchesSetting_WithWildcards() {
    final List<String> wildcardsIncludingTitleAndBody = Arrays.asList("*Field", "*Field*", "^*ld$");
    final List<String> wildcardsIncludingBodyOnly = Arrays.asList("body*", "*dy*", "^bo*$");

    for (String fieldWildcard : wildcardsIncludingTitleAndBody) {
      QueryParser.QueryParserResult queryParserResult = queryParser.parse(
          new MultiMatchQueryBuilder(TEST_QUERY_TEXT, fieldWildcard),
          Arrays.asList(TEST_BODY_FIELD),
          Arrays.asList(TEST_TITLE_FIELD));

      performQueryParserResultAssertions(queryParserResult, TEST_QUERY_TEXT, TEST_BODY_FIELD, TEST_TITLE_FIELD);
    }

    for (String fieldWildcard : wildcardsIncludingBodyOnly) {
      QueryParser.QueryParserResult queryParserResult1 = queryParser.parse(
          new MultiMatchQueryBuilder(TEST_QUERY_TEXT, fieldWildcard),
          Arrays.asList(TEST_BODY_FIELD),
          Arrays.asList(TEST_TITLE_FIELD));

      performQueryParserResultAssertions(queryParserResult1, TEST_QUERY_TEXT, TEST_BODY_FIELD, null);

      QueryParser.QueryParserResult queryParserResult2 = queryParser.parse(
          new MultiMatchQueryBuilder(TEST_QUERY_TEXT, fieldWildcard, TEST_TITLE_FIELD),
          Arrays.asList(TEST_BODY_FIELD),
          Arrays.asList(TEST_TITLE_FIELD));

      performQueryParserResultAssertions(queryParserResult2, TEST_QUERY_TEXT, TEST_BODY_FIELD, TEST_TITLE_FIELD);
    }
  }

  public void testParse_MultiMatch_RequestFieldDoesNotMatchSetting() {
    final List<String> fieldConfigurationsExcludingBody =
        Arrays.asList(TEST_TITLE_FIELD, "*title*", "^title*$", "title*^3");

    for (String fieldConfig : fieldConfigurationsExcludingBody) {
      QueryParser.QueryParserResult queryParserResult = queryParser.parse(
          new MultiMatchQueryBuilder(TEST_QUERY_TEXT, fieldConfig),
          Arrays.asList(TEST_BODY_FIELD),
          Arrays.asList(TEST_TITLE_FIELD));

      Assert.assertNull(queryParserResult);
    }
  }

  public void testParse_UnsupportedQueryType() {
    QueryParser.QueryParserResult queryParserResult = queryParser.parse(
        new TermQueryBuilder(TEST_BODY_FIELD, TEST_QUERY_TEXT),
        Arrays.asList(TEST_BODY_FIELD),
        Arrays.asList(TEST_TITLE_FIELD));
    Assert.assertNull(queryParserResult);
  }

  private void performQueryParserResultAssertions(final QueryParserResult queryParserResult,
      final String query, final String bodyField, final String titleField) {
    Assert.assertEquals(query, queryParserResult.getQueryText());
    Assert.assertEquals(bodyField, queryParserResult.getBodyFieldName());
    Assert.assertEquals(titleField, queryParserResult.getTitleFieldName());
  }
}
