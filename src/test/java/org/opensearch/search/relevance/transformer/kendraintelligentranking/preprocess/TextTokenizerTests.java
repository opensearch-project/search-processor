/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.opensearch.test.OpenSearchTestCase;

public class TextTokenizerTests extends OpenSearchTestCase {

  private static final String TEXT_1 = "What is the capital of the United States?   ";
  private static final List<String> EXPECTED_1 = Arrays.asList("capital", "united", "states");
  private static final String TEXT_2 = "OPENSEARCH IS OPEN SOURCE SEARCH AND ANALYTICS SUITE NUMBER1.";
  private static final List<String> EXPECTED_2 = Arrays.asList("opensearch", "open", "source", "search", "analytics", "suite", "number1");
  private static final String TEXT_3 =
      "You can install OpenSearch by following instructions at https://opensearch.org/docs/latest/opensearch/install/index/";
  private static final List<String> EXPECTED_3 = Arrays.asList("install", "opensearch", "following",
      "instructions", "https", "opensearchorg", "docs", "latest", "opensearch", "install", "index");
  private static final String TEXT_4 = "Testing lots of spaces    and a long word Pneumonoultramicroscopicsilicovolcanoconiosis";
  private static final List<String> EXPECTED_4 = Arrays.asList("testing", "lots", "spaces", "long", "word",
      "pneumonoultramicroscopics", "ilicovolcanoconiosis");

  private TextTokenizer textTokenizer = new TextTokenizer();
  
  public void testTokenize() {
    List<String> testCases = Arrays.asList(null, "", TEXT_1, TEXT_2, TEXT_3, TEXT_4);
    List<List<String>> expectedResults = Arrays.asList(Collections.emptyList(), Collections.emptyList(), EXPECTED_1, EXPECTED_2, EXPECTED_3, EXPECTED_4);
    for (int i = 0; i < testCases.size(); ++i) {
      assertEquals("Test case " + testCases.get(i) + " failed", expectedResults.get(i), textTokenizer.tokenize(testCases.get(i)));
    }
  }
  
  public void testTokenizeMultiple() {
    List<List<String>> actual = textTokenizer.tokenize(Arrays.asList(TEXT_1, TEXT_2, TEXT_3, TEXT_4));
    assertEquals(Arrays.asList(EXPECTED_1, EXPECTED_2, EXPECTED_3, EXPECTED_4), actual);
  }

  public void testIsWordAllPunctuation() {
    List<String> testCases = Arrays.asList(null, "", " ", "!@./?");
    List<Boolean> expectedResults = Arrays.asList(false, false, false, true);
    for (int i = 0; i < testCases.size(); ++i) {
      assertEquals("Test case " + testCases.get(i) + " failed", expectedResults.get(i), textTokenizer.isWordAllPunctuation(testCases.get(i)));
    }
  }

  public void testIsNumeric() {
    List<String> testCases = Arrays.asList(null, "", " ", "!@./?", "abc", "  22  ", "22", "5.028", "-20.0d");
    List<Boolean> expectedResults = Arrays.asList(false, false, false, false, false, true, true, true, true);
    for (int i = 0; i < testCases.size(); ++i) {
      assertEquals("Test case " + testCases.get(i) + " failed", expectedResults.get(i), textTokenizer.isNumeric(testCases.get(i)));
    }
  }

  public void testRemoveInWordPunctuation() {
    List<String> testCases = Arrays.asList(null, "", " ", "!@./?", "ab!!c", "a b!c,22");
    List<String> expectedResults = Arrays.asList(null, "", " ", "", "abc", "a bc22");
    for (int i = 0; i < testCases.size(); ++i) {
      assertEquals("Test case " + testCases.get(i) + " failed", expectedResults.get(i), textTokenizer.removeInWordPunctuation(testCases.get(i)));
    }
  }
}
