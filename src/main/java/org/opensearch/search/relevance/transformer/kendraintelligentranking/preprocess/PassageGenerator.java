/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PassageGenerator {
  private static final int MAX_SENTENCE_LENGTH_IN_TOKENS = 35;
  private static final int MIN_PASSAGE_LENGTH_IN_TOKENS = 100;
  private static final int MAX_PASSAGE_COUNT = 10;

  private SentenceSplitter sentenceSplitter;
  private TextTokenizer textTokenizer;

  public PassageGenerator() {
    this.sentenceSplitter = new SentenceSplitter();
    this.textTokenizer = new TextTokenizer();
  }

  public List<List<String>> generatePassages(final String document, final int maxSentenceLengthInTokens,
      final int minPassageLengthInTokens, final int maxPassageCount) {
    if (document == null || document.isBlank()) {
      return new ArrayList<>();
    }

    List<List<String>> tokenizedSentences = generateTokenizedSentences(document, maxSentenceLengthInTokens);

    // To generate N passages with overlap, generate N/2 + 1 passages first, then overlap and exclude last passage
    List<List<List<String>>> passages = combineSentencesIntoPassages(tokenizedSentences, 
        minPassageLengthInTokens, (maxPassageCount / 2 + 1));

    return generatePassagesWithOverlap(passages);
  }


  List<List<String>> generatePassagesWithOverlap(final List<List<List<String>>> passages) {
    final int passageCount = passages.size();
    final List<Integer> passageSentenceCounts = passages.stream()
        .map(p -> p.size())
        .collect(Collectors.toList());

    // Generate list of passages, with each passage being a list of tokens, by combining sentences in each passage
    List<List<String>> passagesWithOverlap = new ArrayList<>();

    if (passageCount == 0) {
      return passagesWithOverlap;
    }

    if (passageCount == 1) {
      passagesWithOverlap.add(combineSentencesIntoSinglePassage(passages.get(0)));
      return passagesWithOverlap;
    }

    for (int i = 0; i < (passageCount - 1); ++i) {
      // Start at the middle sentence of the first passage
      final int firstPassageMidSentenceIndex = (int) Math.floor(passageSentenceCounts.get(i) / 2.0);

      // Stop at the middle sentence of the next passage. If there is only one sentence, take it
      final int nextPassageMidSentenceIndex = (int) Math.max(1, Math.floor(passageSentenceCounts.get(i + 1) / 2.0));

      // Add first passage to overall list, combining tokenized sentences into a single list of tokens
      passagesWithOverlap.add(combineSentencesIntoSinglePassage(passages.get(i)));

      // Generate the passage with overlap
      final List<String> newPassage = new ArrayList<>();
      // Use final integer values for stream operation
      newPassage.addAll(combineSentencesIntoSinglePassage(
          passages.get(i).subList(firstPassageMidSentenceIndex, passageSentenceCounts.get(i))));
      newPassage.addAll(combineSentencesIntoSinglePassage(
          passages.get(i + 1).subList(0, nextPassageMidSentenceIndex)));

      // Add passage with overlap to overall list
      passagesWithOverlap.add(newPassage);
    }

    // Do not add the last passage, in order to limit the overall
    return passagesWithOverlap;
  }

  List<String> combineSentencesIntoSinglePassage(final List<List<String>> tokenizedSentences) {
    return tokenizedSentences.stream()
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  /**
   * Combine sentences into passages with minimum length {@code MIN_PASSAGE_LENGTH_IN_TOKENS},
   * without splitting up sentences, upto a maximum number of passages
   * @param tokenizedSentences list of tokenized sentences
   * @return List of passages, where each passage is a list of tokenized sentences
   */
  List<List<List<String>>> combineSentencesIntoPassages(final List<List<String>> tokenizedSentences, 
      int minPassageLengthInTokens,
      int maxPassageCount) {
    final int sentenceCount = tokenizedSentences.size();
    // Maintain list of lengths of each sentence
    final List<Integer> sentenceLengthsInTokens = tokenizedSentences.stream()
        .map(s -> s.size())
        .collect(Collectors.toList());

    int currentPassageLengthInTokens = 0;
    // Each passage is a list of tokenized sentences, tokens from all sentences are not collapsed
    // into a single string because sentences are required for further processing
    List<List<String>> currentPassage = new ArrayList<>();
    List<List<List<String>>> passages = new ArrayList<>();

    for (int i = 0; i < sentenceCount; ++i) {
      // Add the sentence to the current passage
      currentPassage.add(tokenizedSentences.get(i));
      currentPassageLengthInTokens += sentenceLengthsInTokens.get(i);

      // If the token count from all remaining sentences is less than half the minimum passage size,
      // append all remaining sentence to current passage and end
      if (i < (sentenceCount - 1)) {
        final int tokenCountFromRemainingSentences = sentenceLengthsInTokens.subList(i + 1, sentenceCount).stream()
            .reduce(0, Integer::sum);
        if (tokenCountFromRemainingSentences <= (minPassageLengthInTokens / 2)) {
          currentPassage.addAll(tokenizedSentences.subList(i + 1, sentenceCount));
          passages.add(currentPassage);
          break;
        }
      }

      // If min passage length is reached, or this is the last sentence, add current passage to list of passages
      if (currentPassageLengthInTokens >= minPassageLengthInTokens || i == (sentenceCount - 1)) {
        passages.add(currentPassage);
        // Reset current passage and it length
        currentPassage = new ArrayList<>();
        currentPassageLengthInTokens = 0;
      }
      
      // If max number of passages is reached, end
      if (passages.size() == maxPassageCount) {
        break;
      }
    }

    return passages;
  }

  /**
   * Split a text document into tokenized sentences, while breaking up large sentences
   * @param document input document
   * @return List, where each member of the list is a list of tokens
   */
  List<List<String>> generateTokenizedSentences(final String document, final int maxSentenceLengthInTokens) {
    List<List<String>> tokenizedSentences = new ArrayList<>();

    List<String> sentences = sentenceSplitter.split(document);
    for (String sentence: sentences) {
      List<String> currentSentence = textTokenizer.tokenize(sentence);
      if (currentSentence.isEmpty()) {
        continue;
      }
      // Break up long sentences
      if (currentSentence.size() <= maxSentenceLengthInTokens) {
        tokenizedSentences.add(currentSentence);
      } else {
        final int sentenceLengthInTokens = currentSentence.size();
        for (int i = 0; i < sentenceLengthInTokens; i += maxSentenceLengthInTokens) {
          final int tokensRemainingInSentence =
              sentenceLengthInTokens - (i + maxSentenceLengthInTokens);
          // If the remaining text is too short, add it to the current sentence and end
          if (tokensRemainingInSentence <= (maxSentenceLengthInTokens / 2)) {
            tokenizedSentences.add(currentSentence.subList(i, sentenceLengthInTokens));
            break;
          }
          tokenizedSentences.add(currentSentence.subList(i,
              Math.min(sentenceLengthInTokens, i + maxSentenceLengthInTokens)));
        }
      }
    }
    return tokenizedSentences;
  }
}
