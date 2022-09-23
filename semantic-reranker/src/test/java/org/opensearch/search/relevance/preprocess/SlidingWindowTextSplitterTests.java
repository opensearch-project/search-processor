/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.preprocess;

import static org.opensearch.common.io.PathUtils.get;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import org.opensearch.test.OpenSearchTestCase;

public class SlidingWindowTextSplitterTests extends OpenSearchTestCase {
  private static final String TEST_INPUT =
      "This is a test 1. This is a test 2. This is a test 3. This is a test 4. This is a test 5.";
  private static final String EXPECTED_SPLIT_1 = "This is a test 1. This is a test 2. ";
  private static final String EXPECTED_SPLIT_2 = "This is a test 2. This is a test 3. ";
  private static final String EXPECTED_SPLIT_3 = "This is a test 3. This is a test 4. ";
  private static final String EXPECTED_SPLIT_4 = "This is a test 4. This is a test 5.";
  private static final String EXPECTED_SPLIT_4_WITH_END_SPACE = "This is a test 4. This is a test 5. ";

  private static final String EXPECTED_LARGE_SPLIT_1 = "This is a test 3. This is a test 4. This is a test 5.";

  private static final String TEST_FILE_PATH = "splitter/input.txt";

  public void testConstructWithInvalidInputs() {
    // Step size cannot be larger than window size
    assertThrows(IllegalArgumentException.class, () -> new SlidingWindowTextSplitter(3, 4));
  }

  public void testSplitWithEmptyInput() {
    SlidingWindowTextSplitter splitter = new SlidingWindowTextSplitter(10, 4);
    assertEquals(Collections.emptyList(), splitter.split(""));
  }

  public void testSetSlidingWindowWithInvalidStepSize() {
    SlidingWindowTextSplitter splitter = new SlidingWindowTextSplitter(10, 4);
    assertThrows(IllegalArgumentException.class, () -> splitter.setSlidingWindow(2, 4));
  }

  public void testSetSlidingWindowUpdatesSplitter() {
    SlidingWindowTextSplitter splitter = new SlidingWindowTextSplitter(30, 10);
    List<String> splitText = splitter.split(TEST_INPUT);

    assertEquals(4, splitText.size());
    assertEquals(Arrays.asList(EXPECTED_SPLIT_1, EXPECTED_SPLIT_2, EXPECTED_SPLIT_3, EXPECTED_SPLIT_4),
        splitText);

    // double window size, expect each passage to be loonger
    splitter.setSlidingWindow(60, 10);
    splitText = splitter.split(TEST_INPUT);

    assertEquals(2, splitText.size());
    assertEquals(Arrays.asList(EXPECTED_SPLIT_1 + EXPECTED_SPLIT_3, EXPECTED_SPLIT_2 + EXPECTED_SPLIT_4),
        splitText);

    // double
    splitter.setSlidingWindow(60, 30);
    splitText = splitter.split(TEST_INPUT);

    assertEquals(2, splitText.size());
    assertEquals(Arrays.asList(EXPECTED_SPLIT_1 + EXPECTED_SPLIT_3,
            EXPECTED_LARGE_SPLIT_1),
        splitText);
  }

  public void testSplitWhenLastSplitIsShorterThanWindow() {
    final String shortText = "Short text";
    final String inputText = String.join(" ", TEST_INPUT, shortText);
    final String expectedFinalSplit = String.join(" ", "This is a test 5.", shortText);

    SlidingWindowTextSplitter splitter = new SlidingWindowTextSplitter(30, 10);
    List<String> splitText = splitter.split(inputText);

    assertEquals(5, splitText.size());
    assertEquals(Arrays.asList(EXPECTED_SPLIT_1, EXPECTED_SPLIT_2, EXPECTED_SPLIT_3,
            EXPECTED_SPLIT_4_WITH_END_SPACE, expectedFinalSplit),
        splitText);
  }

  public void testSplitWithOverlap() throws IOException {
    final int windowSize = 1500;
    final int stepSize = 1300;
    // Because of respecting sentence boundaries, actual overlap might be smaller.
    // Provide a buffer of 20 characters
    final int expectedOverlap = windowSize - stepSize - 20;

    SlidingWindowTextSplitter splitter = new SlidingWindowTextSplitter(windowSize, stepSize);
    final String input = loadTestInput();

    List<String> splitText = splitter.split(input);

    // Overlap means that we get more splits
    assertTrue(splitText.size() >= input.length() / windowSize);

    // Verify overlap for every pair of splits
    for(int i = 0; i < splitText.size() - 1; ++i) {
      assertTrue(splitText.get(i).contains(splitText.get(i + 1).substring(0, expectedOverlap)));
    }
  }

  public void testSplitUsesLineBreakWhenSentenceIsTooLong() throws IOException {
    final int windowSize = 1500;
    final int stepSize = 1300;

    SlidingWindowTextSplitter splitter = new SlidingWindowTextSplitter(windowSize, stepSize);
    final String input = loadTestInput();
    String inputWithoutSentenceBoundaries = input.replace(".", " ");

    List<String> splitText = splitter.split(inputWithoutSentenceBoundaries);

    // Ensure that text is split
    assertTrue(splitText.size() > 1);
  }

  private String loadTestInput() throws IOException {
    final Scanner scanner = new Scanner(SlidingWindowTextSplitterTests.class.getClassLoader()
        .getResourceAsStream(TEST_FILE_PATH),
        StandardCharsets.UTF_8.name()).useDelimiter("\\A");
    return scanner.hasNext() ? scanner.next() : "";
  }
}
