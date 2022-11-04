/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TextTokenizer {
  private static final Set<String> STOP_WORDS = new HashSet<>(
      Arrays.asList("i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", "yourself", "yourselves", "he", "him", "his",
          "himself", "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who",
          "whom", "this", "that", "these", "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having", "do",
          "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while", "of", "at", "by", "for", "with",
          "about", "against", "between", "into", "through", "during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out",
          "on", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both",
          "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t",
          "can", "will", "just", "don", "should", "now"));
  private static final Pattern SPLIT_PATTERN = Pattern.compile("[\\p{Punct}\\s]+");

  public List<List<String>> tokenize(List<String> texts) {
    return texts.stream()
        .map(text -> tokenize(text))
        .collect(Collectors.toList());
  }

  public List<String> tokenize(String text) {
    String[] tokens = text.split(SPLIT_PATTERN.pattern());
    List<String> validTokens = new ArrayList<>();
    for (String token : tokens) {
      if (token.length() == 0) {
        continue;
      }
      String lowerCased = token.toLowerCase(Locale.ENGLISH);
      if (!STOP_WORDS.contains(lowerCased)) {
        validTokens.add(lowerCased);
      }
    }
    return validTokens;
  }
}
