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
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.opensearch.test.OpenSearchTestCase;

public class SlidingWindowTextSplitterTests extends OpenSearchTestCase {
  private static final int MAXIMUM_PASSAGES = 10;
  private static final String TEST_FILE_PATH = "splitter/input.txt";

  public void testConstructWithInvalidInputs() {
    // Step size cannot be larger than window size
    assertThrows(IllegalArgumentException.class, () -> new SlidingWindowTextSplitter(3, 4, MAXIMUM_PASSAGES));
  }

  public void testSplitWithEmptyInput() {
    SlidingWindowTextSplitter splitter = new SlidingWindowTextSplitter(10, 4, MAXIMUM_PASSAGES);
    assertEquals(Collections.emptyList(), splitter.split(""));
  }

  public void testSetSlidingWindowWithInvalidStepSize() {
    SlidingWindowTextSplitter splitter = new SlidingWindowTextSplitter(10, 4, MAXIMUM_PASSAGES);
    assertThrows(IllegalArgumentException.class, () -> splitter.setSlidingWindow(2, 4));
  }

  public void testSetSlidingWindowUpdatesSplitter() {
    SlidingWindowTextSplitter splitter = new SlidingWindowTextSplitter(30, 10, MAXIMUM_PASSAGES);
    final String testInput = generateTestInput(1, 5, false);
    List<String> splitText = splitter.split(testInput);

    assertEquals(4, splitText.size());
    assertEquals(Arrays.asList(
          generateTestInput(1, 2, true),
          generateTestInput(2, 3, true),
          generateTestInput(3, 4, true),
          generateTestInput(4, 5, false)),
        splitText);

    // double window size, expect each passage to be loonger
    splitter.setSlidingWindow(60, 10);
    splitText = splitter.split(testInput);

    assertEquals(2, splitText.size());
    assertEquals(Arrays.asList(
        generateTestInput(1, 4, true),
        generateTestInput(2, 5, false)),
        splitText);

    // double
    splitter.setSlidingWindow(60, 30);
    splitText = splitter.split(testInput);

    assertEquals(2, splitText.size());
    assertEquals(Arrays.asList(
            generateTestInput(1, 4, true),
            generateTestInput(3, 5, false)),
        splitText);
  }

  public void testSplitObeysMaximumPassagesLimit() {
    final int maximumPassages = 3;
    final String inputText = generateTestInput(1, 5, false);

    SlidingWindowTextSplitter splitter = new SlidingWindowTextSplitter(30, 10, maximumPassages);
    List<String> splitText = splitter.split(inputText);

    assertEquals(maximumPassages, splitText.size());
    assertEquals(Arrays.asList(
            generateTestInput(1, 2, true),
            generateTestInput(2, 3, true),
            generateTestInput(3, 4, true)),
        splitText);
  }

  public void testSplitWhenLastSplitIsShorterThanWindow() {
    final String shortText = "Short text";
    final String inputText = String.join(" ", generateTestInput(1, 5, false), shortText);
    final String expectedFinalSplit = String.join(" ", "This is a test 5.", shortText);

    SlidingWindowTextSplitter splitter = new SlidingWindowTextSplitter(30, 10, MAXIMUM_PASSAGES);
    List<String> splitText = splitter.split(inputText);

    assertEquals(5, splitText.size());
    assertEquals(Arrays.asList(
            generateTestInput(1, 2, true),
            generateTestInput(2, 3, true),
            generateTestInput(3, 4, true),
            generateTestInput(4, 5, true),
            expectedFinalSplit),
        splitText);
  }

  public void testSplitWithOverlap() throws IOException {
    final int windowSize = 1500;
    final int stepSize = 1300;
    // Because of respecting sentence boundaries, actual overlap might be smaller.
    // Provide a buffer of 20 characters
    final int expectedOverlap = windowSize - stepSize - 20;

    SlidingWindowTextSplitter splitter = new SlidingWindowTextSplitter(windowSize, stepSize, MAXIMUM_PASSAGES);
    final String input = loadTestInputFromFile();

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

    SlidingWindowTextSplitter splitter = new SlidingWindowTextSplitter(windowSize, stepSize, MAXIMUM_PASSAGES);
    final String input = loadTestInputFromFile();
    String inputWithoutSentenceBoundaries = input.replace(".", " ");

    List<String> splitText = splitter.split(inputWithoutSentenceBoundaries);

    // Ensure that text is split
    assertTrue(splitText.size() > 1);
  }

  private String generateTestInput(int start, int end, boolean addTerminalSpace) {
    final String testInput = IntStream.range(start, end + 1).boxed().map(
        i -> String.format(Locale.ENGLISH, "This is a test %s.", i)
    ).collect(Collectors.joining(" "));
    return addTerminalSpace ? testInput + " " : testInput;
  }

  private String loadTestInputFromFile() throws IOException {
    final Scanner scanner = new Scanner(SlidingWindowTextSplitterTests.class.getClassLoader()
        .getResourceAsStream(TEST_FILE_PATH),
        StandardCharsets.UTF_8.name()).useDelimiter("\\A");
    return scanner.hasNext() ? scanner.next() : "";
  }
}
