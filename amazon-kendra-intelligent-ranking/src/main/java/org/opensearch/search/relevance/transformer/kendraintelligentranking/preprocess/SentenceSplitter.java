/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess;

import com.ibm.icu.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SentenceSplitter {

  /**
   * Split the input text into sentences
   * @param text input text
   * @return list of strings, each a sentence
   */
  public List<String> split(final String text) {
    if (text == null) {
      return new ArrayList<>();
    }

    final BreakIterator breakIterator = BreakIterator.getSentenceInstance(Locale.ENGLISH);
    breakIterator.setText(text);

    List<String> sentences = new ArrayList();
    int start = breakIterator.first();
    String currentSentence;

    for (int end = breakIterator.next(); end != BreakIterator.DONE; start = end, end = breakIterator.next()) {
      currentSentence = text.substring(start, end).stripTrailing();
      if (!currentSentence.isEmpty()) {
        sentences.add(currentSentence);
      }
    }
    return sentences;
  }
}
