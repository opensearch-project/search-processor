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
import org.opensearch.test.OpenSearchTestCase;

public class SentenceSplitterTests extends OpenSearchTestCase {

  private static final String TEXT_1 = "What is the capital of the United States?";
  private static final String TEXT_2 = "OPENSEARCH IS OPEN SOURCE SEARCH AND ANALYTICS SUITE.";
  private static final String TEXT_3 =
      "You can install OpenSearch by following instructions at https://opensearch.org/docs/latest/opensearch/install/index/.";
  private static final String TEXT_4 = "Testing lots of spaces    ! and a long word Pneumonoultramicroscopicsilicovolcanoconiosis";

  private SentenceSplitter sentenceSplitter = new SentenceSplitter();

  public void testSplit_BlankInput() {
    assertEquals(Collections.emptyList(), sentenceSplitter.split(null));
    assertEquals(Collections.emptyList(), sentenceSplitter.split(""));
    assertEquals(Collections.emptyList(), sentenceSplitter.split("  "));
  }

  public void testSplit() {
    final String text = String.join(" ", TEXT_1 + "   ", TEXT_2, TEXT_3, TEXT_4);
    List<String> splitSentences = sentenceSplitter.split(text);

    assertEquals(Arrays.asList(TEXT_1, TEXT_2, TEXT_3,
        "Testing lots of spaces    !",
        "and a long word Pneumonoultramicroscopicsilicovolcanoconiosis"), splitSentences);
  }
}
