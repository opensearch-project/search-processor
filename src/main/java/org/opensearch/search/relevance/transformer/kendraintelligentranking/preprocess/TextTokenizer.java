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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TextTokenizer {
  private static final int MINIMUM_WORD_LENGTH = 2;
  private static final int MAXIMUM_WORD_LENGTH = 25;
  private static final Set<String> STOP_WORDS = new HashSet<>(
      Arrays.asList("i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", "yourself", "yourselves", "he", "him", "his",
          "himself", "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who",
          "whom", "this", "that", "these", "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having", "do",
          "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while", "of", "at", "by", "for", "with",
          "about", "against", "between", "into", "through", "during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out",
          "on", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both",
          "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t",
          "can", "will", "just", "don", "should", "now"));
  private static final Pattern ALL_PUNCTUATIONS_REGEX = Pattern.compile("^\\p{Pc}+$|^\\p{Pd}+$|^\\p{Pe}+$|^\\p{Pf}+$|^\\p{Pi}+$|^\\p{Po}+$|^\\p{Ps}+$");
  private static final Pattern PUNCTUATIONS_REGEX_PATTERN = Pattern.compile("\\p{Pc}|\\p{Pd}|\\p{Pe}|\\p{Pf}|\\p{Pi}|\\p{Po}|\\p{Ps}");

  public List<List<String>> tokenize(List<String> texts) {
    if (texts == null) {
      return new ArrayList<>();
    }

    return texts.stream()
        .map(text -> tokenize(text))
        .collect(Collectors.toList());
  }

  /**
   * Split the input text into tokens, with post-processing to remove stop words, punctuation, etc.
   * @param text input text
   * @return list of tokens
   */
  public List<String> tokenize(String text) {
    if (text == null) {
      return new ArrayList<>();
    }

    final BreakIterator breakIterator = BreakIterator.getWordInstance(Locale.ENGLISH);
    breakIterator.setText(text);

    List<String> tokens = new ArrayList();
    int start = breakIterator.first();
    String currentWord;
    for (int end = breakIterator.next(); end != BreakIterator.DONE; start = end, end = breakIterator.next()) {
      currentWord = text.substring(start, end).stripTrailing().toLowerCase(Locale.ENGLISH);
      if (currentWord.isEmpty()) {
        continue;
      }
      // Split long words
      List<String> shortenedTokens = new ArrayList<>();
      if (currentWord.length() <= MAXIMUM_WORD_LENGTH) {
        shortenedTokens.add(currentWord);
      } else {
        for (int i = 0; i < currentWord.length(); i += MAXIMUM_WORD_LENGTH) {
          shortenedTokens.add(currentWord.substring(i, Math.min(currentWord.length(), i + MAXIMUM_WORD_LENGTH)));
        }
      }
      // Filter out punctuation, short words, numbers
      for (String shortenedToken : shortenedTokens) {
        if (!isWordAllPunctuation(shortenedToken) && !STOP_WORDS.contains(shortenedToken) &&
            shortenedToken.length() >= MINIMUM_WORD_LENGTH && !isNumeric(shortenedToken)) {
          String tokenWithInWordPunctuationRemoved = removeInWordPunctuation(shortenedToken);
          if (!tokenWithInWordPunctuationRemoved.isEmpty()) {
            tokens.add(tokenWithInWordPunctuationRemoved);
          }
        }
      }
    }
    return tokens;
  }

  boolean isWordAllPunctuation(final String token) {
    return (token != null) && ALL_PUNCTUATIONS_REGEX.matcher(token).matches();
  }

  boolean isNumeric(final String token) {
    if (token == null) {
      return false;
    }
    try {
      Double.parseDouble(token);
    } catch (NumberFormatException e) {
      return false;
    }
    return true;
  }

  String removeInWordPunctuation(String token) {
    if (token == null) {
      return null;
    }
    return PUNCTUATIONS_REGEX_PATTERN.matcher(token).replaceAll("");
  }
}
