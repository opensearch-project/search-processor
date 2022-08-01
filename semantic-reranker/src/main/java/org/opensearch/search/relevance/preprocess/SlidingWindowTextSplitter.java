/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.preprocess;

import com.google.common.collect.Lists;
import com.ibm.icu.text.BreakIterator;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Applies a sliding window to split input text into passages. However, splitting is aware of
 * sentence boundaries and hence the size of splits may be larger than the configured window size.
 */
public class SlidingWindowTextSplitter {

  /**
   * Minimum size of window of text to be selected in a split. Window size can be larger
   * in practice because of respecting sentence boundaries.
   */
  private Integer windowSize;

  /**
   * The minimum step size by which to move the sliding window after a split. Step size can be larger
   * in practice because of respecting sentence boundaries.
   */
  private Integer stepSize;

  //local parameters to control sentence/token iteration
  private Pair<Integer, Integer> lastSentenceBoundary;
  private boolean useTokenIterator;

  public SlidingWindowTextSplitter(int windowSize, int stepSize) {
    setSlidingWindow(windowSize, stepSize);
    this.useTokenIterator = Boolean.FALSE;
    this.lastSentenceBoundary = null;
  }

  /**
   * Set parameters of sliding window text splitter
   * @param updatedWindowSize text window size
   * @param updatedStepSize step size to move the window
   */
  public void setSlidingWindow(Integer updatedWindowSize, Integer updatedStepSize) {
    if (updatedStepSize > updatedWindowSize) {
      throw new IllegalArgumentException("Step size " + updatedStepSize + " is larger than window size " + updatedWindowSize);
    }

    this.windowSize = updatedWindowSize;
    this.stepSize = updatedStepSize;
  }

  /**
   * Obtains a list of split passages, aware of sentence boundaries, from the input text.
   * @param text Text to be split
   * @return List of passages extracted from input text
   */
  public List<String> split(String text) {
    if (text.isEmpty()) {
      return Lists.newArrayList();
    }

    if (text.length() <= windowSize) {
      return Arrays.asList(text);
    }

    List<String> splitText = new LinkedList<>();

    BreakIterator sentenceIterator = BreakIterator.getSentenceInstance(Locale.ENGLISH);
    BreakIterator tokenIterator = BreakIterator.getLineInstance(Locale.ENGLISH);
    sentenceIterator.setText(text);

    int startBoundaryIndex = 0;
    int endBoundaryIndex = 0;

    int nextStartBoundaryIndex = 0;
    boolean nextStartBoundaryIndexUpdated = false;

    while (endBoundaryIndex != BreakIterator.DONE) {
      // If current passage length is already larger than the step size,
      // use the end index as the start index for next window
      if (!nextStartBoundaryIndexUpdated && (endBoundaryIndex - startBoundaryIndex + 1) >= stepSize) {
        nextStartBoundaryIndexUpdated = true;
        nextStartBoundaryIndex = endBoundaryIndex;
      }

      if (endBoundaryIndex == text.length() ) {
        // End of the input text. Add the passage, irrespective of its length.
        splitText.add(text.substring(startBoundaryIndex, endBoundaryIndex));
      } else if ((endBoundaryIndex - startBoundaryIndex + 1) >= windowSize && endBoundaryIndex > nextStartBoundaryIndex ) {
        // If current passage length is greater than both step and window size, extend the window
        // such that all passages have some overlap
        splitText.add(text.substring(startBoundaryIndex, endBoundaryIndex));

        startBoundaryIndex = nextStartBoundaryIndex;

        // Check whether current end boundary index can be used as next start boundary index
        if ((endBoundaryIndex - startBoundaryIndex + 1) >= stepSize) {
          nextStartBoundaryIndex = endBoundaryIndex;
          nextStartBoundaryIndexUpdated = true;
        } else {
          nextStartBoundaryIndexUpdated = false;
        }
      }

      endBoundaryIndex = getNextEndBoundaryIndex(sentenceIterator, tokenIterator, endBoundaryIndex, text);
    }

    return splitText;
  }

  /**
   * Get the end boundary index of the next split. In common cases, move the iterator by a sentence.
   * If a sentence is too long, move the iterator by tokens rather than sentences.
   * @param sentenceIterator iterator respecting sentence boundaries
   * @param tokenIterator iterator respecting tokens
   * @param previousEndBoundaryIndex end boundary index of previous split
   * @param text input text to split
   */
  private int getNextEndBoundaryIndex(BreakIterator sentenceIterator, BreakIterator tokenIterator, int previousEndBoundaryIndex, String text) {
    if (!useTokenIterator) {
      int nextSentenceBoundary = sentenceIterator.next();
      int sentenceLength = nextSentenceBoundary - previousEndBoundaryIndex;
      if (nextSentenceBoundary == BreakIterator.DONE || sentenceLength < windowSize) {
        return nextSentenceBoundary;
      } else {
        // Sentence is too long, use tokenIterator
        lastSentenceBoundary = Pair.of(previousEndBoundaryIndex, nextSentenceBoundary);
        useTokenIterator = Boolean.TRUE;
        tokenIterator.setText(text.substring(previousEndBoundaryIndex, nextSentenceBoundary));
      }
    }

    // Always add the offset of previous sentence
    int nextTokenBoundary = lastSentenceBoundary.getLeft() + tokenIterator.next();
    if (nextTokenBoundary == lastSentenceBoundary.getRight()) {
      // Finished token iterations, unset booleans to start from next sentence.
      useTokenIterator = Boolean.FALSE;
      lastSentenceBoundary = null;
    }
    return nextTokenBoundary;
  }
}