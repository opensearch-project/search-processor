/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.preprocess;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.opensearch.test.OpenSearchTestCase;

public class TextTokenizerTests extends OpenSearchTestCase {

  public static final String TEXT_1 = "What is the capital of the United States?";
  public static final List<String> EXPECTED_1 = Arrays.asList("capital", "united", "states");
  public static final String TEXT_2 = "OPENSEARCH IS OPEN SOURCE SEARCH AND ANALYTICS SUITE.";
  public static final List<String> EXPECTED_2 = Arrays.asList("opensearch", "open", "source", "search", "analytics", "suite");
  private TextTokenizer textTokenizer = new TextTokenizer();
  
  public void testTokenize1() {
    List<String> actual = textTokenizer.tokenize(TEXT_1);
    Assert.assertEquals(EXPECTED_1, actual);
  }
  
  public void testTokenize2() {
    List<String> actual = textTokenizer.tokenize(TEXT_2);
    Assert.assertEquals(EXPECTED_2, actual);
  }
  
  public void testTokenizeMultiple() {
    List<List<String>> actual = textTokenizer.tokenize(Arrays.asList(TEXT_1, TEXT_2));
    Assert.assertEquals(Arrays.asList(EXPECTED_1, EXPECTED_2), actual);
  }
}
