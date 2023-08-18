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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opensearch.test.OpenSearchTestCase;

public class PassageGeneratorTests extends OpenSearchTestCase {
  private static final int MAX_SENTENCE_LENGTH_IN_TOKENS = 4;
  private static final int MIN_PASSAGE_LENGTH_IN_TOKENS = 14;

  private static final List<String> SUFFIX_FOR_LONGER_SENTENCE_TOKENS = Arrays.asList("longer", "collection", "suffix");
  private static final String SUFFIX_FOR_LONGER_SENTENCE = String.join(" ", SUFFIX_FOR_LONGER_SENTENCE_TOKENS) + ".";
  private static final List<String> PASSAGE_1_SENTENCE_1_TOKENS = Arrays.asList("Words", "comprising", "passage1", "sentence1");
  private static final String PASSAGE_1_SENTENCE_1 = String.join(" ", PASSAGE_1_SENTENCE_1_TOKENS) + ".";
  private static final List<String> PASSAGE_1_SENTENCE_2_TOKENS = Arrays.asList("Words", "comprising", "passage1", "sentence2");
  private static final String PASSAGE_1_SENTENCE_2 = String.join(" ", PASSAGE_1_SENTENCE_2_TOKENS) + ".";
  private static final List<String> PASSAGE_1_SENTENCE_3_TOKENS = Arrays.asList("Words", "comprising", "passage1", "sentence3");
  private static final List<String> PASSAGE_1_LONG_SENTENCE_TOKENS = Stream.concat(PASSAGE_1_SENTENCE_3_TOKENS.stream(),
      SUFFIX_FOR_LONGER_SENTENCE_TOKENS.stream()).collect(Collectors.toList());
  private static final String PASSAGE_1_LONG_SENTENCE = String.join(" ", PASSAGE_1_LONG_SENTENCE_TOKENS) + ".";
  private static final List<String> PASSAGE_2_SENTENCE_1_TOKENS = Arrays.asList("Words", "comprising", "passage2", "sentence1");
  private static final String PASSAGE_2_SENTENCE_1 = String.join(" ", PASSAGE_2_SENTENCE_1_TOKENS) + ".";
  private static final List<String> PASSAGE_2_SENTENCE_2_TOKENS = Arrays.asList("Words", "comprising", "passage2", "sentence2");
  private static final String PASSAGE_2_SENTENCE_2 = String.join(" ", PASSAGE_2_SENTENCE_2_TOKENS) + ".";
  private static final List<String> PASSAGE_2_SENTENCE_3_TOKENS = Arrays.asList("Words", "comprising", "passage2", "sentence3");
  private static final List<String> PASSAGE_2_LONG_SENTENCE_TOKENS = Stream.concat(PASSAGE_2_SENTENCE_3_TOKENS.stream(),
      SUFFIX_FOR_LONGER_SENTENCE_TOKENS.stream()).collect(Collectors.toList());
  private static final String PASSAGE_2_LONG_SENTENCE = String.join(" ", PASSAGE_2_LONG_SENTENCE_TOKENS) + ".";

  private PassageGenerator passageGenerator = new PassageGenerator();

  public void testGeneratePassages_BlankDocument() {
    assertEquals(Collections.emptyList(), passageGenerator.generatePassages(
        null, MAX_SENTENCE_LENGTH_IN_TOKENS, MIN_PASSAGE_LENGTH_IN_TOKENS, 1));
    assertEquals(Collections.emptyList(), passageGenerator.generatePassages(
        "", MAX_SENTENCE_LENGTH_IN_TOKENS, MIN_PASSAGE_LENGTH_IN_TOKENS, 1));
    assertEquals(Collections.emptyList(), passageGenerator.generatePassages(
        " ", MAX_SENTENCE_LENGTH_IN_TOKENS, MIN_PASSAGE_LENGTH_IN_TOKENS, 1));
  }

  public void testGeneratePassages_ValidDocument() {
    final String document = String.join(" ", PASSAGE_1_SENTENCE_1, PASSAGE_1_SENTENCE_2,
        PASSAGE_1_LONG_SENTENCE, PASSAGE_2_SENTENCE_1, PASSAGE_2_SENTENCE_2, PASSAGE_2_LONG_SENTENCE);

    List<String> expectedPassage1 = new ArrayList<>();
    expectedPassage1.addAll(PASSAGE_1_SENTENCE_1_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));
    expectedPassage1.addAll(PASSAGE_1_SENTENCE_2_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));
    expectedPassage1.addAll(PASSAGE_1_SENTENCE_3_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));
    expectedPassage1.addAll(SUFFIX_FOR_LONGER_SENTENCE_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));

    List<String> expectedOverlapPassage = new ArrayList<>();
    expectedOverlapPassage.addAll(PASSAGE_1_SENTENCE_3_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));
    expectedOverlapPassage.addAll(SUFFIX_FOR_LONGER_SENTENCE_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));
    expectedOverlapPassage.addAll(PASSAGE_2_SENTENCE_1_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));
    expectedOverlapPassage.addAll(PASSAGE_2_SENTENCE_2_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));

    List<List<String>> expectedOutput = Arrays.asList(expectedPassage1, expectedOverlapPassage);

    List<List<String>> actualOutput = passageGenerator.generatePassages(
        document, MAX_SENTENCE_LENGTH_IN_TOKENS, MIN_PASSAGE_LENGTH_IN_TOKENS, 2);
    assertEquals(expectedOutput, actualOutput);

  }

  public void testGeneratePassagesWithOverlap_EmptyInput() {
    assertEquals(Collections.emptyList(), passageGenerator.generatePassagesWithOverlap(new ArrayList<>()));
  }

  public void testGeneratePassagesWithOverlap_SinglePassage() {
    List<List<List<String>>> passages = Arrays.asList(
      Arrays.asList(
        PASSAGE_1_SENTENCE_1_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
        PASSAGE_1_SENTENCE_2_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
        PASSAGE_1_SENTENCE_3_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
        SUFFIX_FOR_LONGER_SENTENCE_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList())
      )
    );

    List<String> expectedPassage = new ArrayList<>();
    expectedPassage.addAll(PASSAGE_1_SENTENCE_1_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));
    expectedPassage.addAll(PASSAGE_1_SENTENCE_2_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));
    expectedPassage.addAll(PASSAGE_1_SENTENCE_3_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));
    expectedPassage.addAll(SUFFIX_FOR_LONGER_SENTENCE_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));
    List<List<String>> expectedOutput = Arrays.asList(expectedPassage);

    List<List<String>> actualOutput = passageGenerator.generatePassagesWithOverlap(passages);
    assertEquals(expectedOutput, actualOutput);
  }

  public void testGeneratePassagesWithOverlap_TwoPassages() {
    List<List<List<String>>> passages = Arrays.asList(
        Arrays.asList(
            PASSAGE_1_SENTENCE_1_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
            PASSAGE_1_SENTENCE_2_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
            PASSAGE_1_SENTENCE_3_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
            SUFFIX_FOR_LONGER_SENTENCE_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList())
        ),
        Arrays.asList(
            PASSAGE_2_SENTENCE_1_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
            PASSAGE_2_SENTENCE_2_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
            PASSAGE_2_SENTENCE_3_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
            SUFFIX_FOR_LONGER_SENTENCE_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList())
        )
    );

    List<String> expectedPassage1 = new ArrayList<>();
    expectedPassage1.addAll(PASSAGE_1_SENTENCE_1_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));
    expectedPassage1.addAll(PASSAGE_1_SENTENCE_2_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));
    expectedPassage1.addAll(PASSAGE_1_SENTENCE_3_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));
    expectedPassage1.addAll(SUFFIX_FOR_LONGER_SENTENCE_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));

    List<String> expectedOverlapPassage = new ArrayList<>();
    expectedOverlapPassage.addAll(PASSAGE_1_SENTENCE_3_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));
    expectedOverlapPassage.addAll(SUFFIX_FOR_LONGER_SENTENCE_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));
    expectedOverlapPassage.addAll(PASSAGE_2_SENTENCE_1_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));
    expectedOverlapPassage.addAll(PASSAGE_2_SENTENCE_2_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));

    List<List<String>> expectedOutput = Arrays.asList(expectedPassage1, expectedOverlapPassage);

    List<List<String>> actualOutput = passageGenerator.generatePassagesWithOverlap(passages);
    assertEquals(expectedOutput, actualOutput);
  }

  public void testCombineSentencesIntoSinglePassage() {
    List<List<String>> tokenizedSentences = Arrays.asList(
        PASSAGE_1_SENTENCE_1_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
        PASSAGE_1_SENTENCE_2_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
        PASSAGE_1_SENTENCE_3_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
        SUFFIX_FOR_LONGER_SENTENCE_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList())
    );

    List<String> expected = new ArrayList<>();
    expected.addAll(PASSAGE_1_SENTENCE_1_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));
    expected.addAll(PASSAGE_1_SENTENCE_2_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));
    expected.addAll(PASSAGE_1_SENTENCE_3_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));
    expected.addAll(SUFFIX_FOR_LONGER_SENTENCE_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));

    List<String> actual = passageGenerator.combineSentencesIntoSinglePassage(tokenizedSentences);
    assertEquals(expected, actual);
  }

  public void testCombineSentencesIntoPassages() {
    List<List<String>> tokenizedSentences = Arrays.asList(
        PASSAGE_1_SENTENCE_1_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
        PASSAGE_1_SENTENCE_2_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
        PASSAGE_1_SENTENCE_3_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
        SUFFIX_FOR_LONGER_SENTENCE_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
        PASSAGE_2_SENTENCE_1_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
        PASSAGE_2_SENTENCE_2_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
        PASSAGE_2_SENTENCE_3_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
        SUFFIX_FOR_LONGER_SENTENCE_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList())
    );

    List<List<List<String>>> expected = Arrays.asList(
        Arrays.asList(
            PASSAGE_1_SENTENCE_1_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
            PASSAGE_1_SENTENCE_2_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
            PASSAGE_1_SENTENCE_3_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
            SUFFIX_FOR_LONGER_SENTENCE_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList())
        )
    );

    List<List<List<String>>> actual = passageGenerator.combineSentencesIntoPassages(
        tokenizedSentences, MIN_PASSAGE_LENGTH_IN_TOKENS, 1);
    assertEquals(expected, actual);
  }

  public void testGenerateTokenizedSentences() {
    final String document = String.join(" ", PASSAGE_1_SENTENCE_1, PASSAGE_1_SENTENCE_2,
        PASSAGE_1_LONG_SENTENCE, PASSAGE_2_SENTENCE_1, PASSAGE_2_SENTENCE_2, PASSAGE_2_LONG_SENTENCE);

    List<List<String>> expected = Arrays.asList(
        PASSAGE_1_SENTENCE_1_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
        PASSAGE_1_SENTENCE_2_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
        PASSAGE_1_SENTENCE_3_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
        SUFFIX_FOR_LONGER_SENTENCE_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
        PASSAGE_2_SENTENCE_1_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
        PASSAGE_2_SENTENCE_2_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
        PASSAGE_2_SENTENCE_3_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()),
        SUFFIX_FOR_LONGER_SENTENCE_TOKENS.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(Collectors.toList())
    );

    List<List<String>> actual = passageGenerator.generateTokenizedSentences(document, MAX_SENTENCE_LENGTH_IN_TOKENS);
    assertEquals(expected, actual);
  }
  

}
