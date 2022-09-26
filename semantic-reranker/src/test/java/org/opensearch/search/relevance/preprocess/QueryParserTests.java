/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.preprocess;

import org.junit.Assert;
import org.opensearch.index.query.MatchQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.test.OpenSearchTestCase;

public class QueryParserTests extends OpenSearchTestCase {

  private static final String TEST_BODY_FIELD = "bodyField";
  private static final String TEST_QUERY_TEXT = "queryText";
  private QueryParser queryParser = new QueryParser();

  public void testParse_Match() {
    QueryParser.QueryParserResult queryParserResult = queryParser.parse(new MatchQueryBuilder(TEST_BODY_FIELD, TEST_QUERY_TEXT));
    Assert.assertEquals(TEST_BODY_FIELD, queryParserResult.getBodyFieldName());
    Assert.assertEquals(TEST_QUERY_TEXT, queryParserResult.getQueryText());
  }

  public void testParse_NonMatch() {
    QueryParser.QueryParserResult queryParserResult = queryParser.parse(new MultiMatchQueryBuilder(TEST_BODY_FIELD, TEST_QUERY_TEXT));
    Assert.assertNull(queryParserResult);
  }
}
