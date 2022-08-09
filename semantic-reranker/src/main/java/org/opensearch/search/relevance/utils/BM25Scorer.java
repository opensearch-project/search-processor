/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class BM25Scorer {
  private Map<String, Integer> wordToDocumentCount = new HashMap<>();
  private double b;
  private double k1;
  private int totalNumberOfDocs;
  private double averageDocumentLength; // avdl

  /**
   * Initialize dataset.
   *
   * @param b         free parameter for BM25
   * @param k1        free parameter for BM25
   * @param documents list of documents, each document is represented by a list of words
   */
  public BM25Scorer(double b, double k1, List<List<String>> documents) {
    this.b = b;
    this.k1 = k1;
    this.totalNumberOfDocs = documents.size();

    double totalDocumentLength = 0;
    for (List<String> document : documents) {
      totalDocumentLength += document.size();

      Set<String> uniqueWordsInDocument = new HashSet<>(document); // add to set to remove duplicates
      for (String term : uniqueWordsInDocument) {
        String lowerCased = term.toLowerCase(Locale.ENGLISH);
        wordToDocumentCount.put(lowerCased, wordToDocumentCount.getOrDefault(lowerCased, 0) + 1);
      }
    }
    this.averageDocumentLength = totalDocumentLength / documents.size();
  }

  /**
   * Calculate the BM25 score of a document given a query.
   *
   * @param query    query represented as a list of words
   * @param document document represented as a list of words
   * @return the BM25 score
   */
  public double score(List<String> query, List<String> document) {
    double score = 0;

    Map<String, Integer> documentWordCounts = new HashMap<>();
    for (String word : document) {
      String lowerCased = word.toLowerCase(Locale.ENGLISH);
      documentWordCounts.put(lowerCased, documentWordCounts.getOrDefault(lowerCased, 0) + 1);
    }

    for (String queryWord : query) {
      if (!documentWordCounts.containsKey(queryWord)) {
        continue;
      }
      double termFrequency = (double) documentWordCounts.get(queryWord) / document.size();
      double denominator = termFrequency + k1 * (1 - b + b * document.size() / averageDocumentLength);
      double idf = idf(queryWord);
      double numerator = idf * termFrequency * (k1 + 1);
      score += numerator / denominator;
    }
    return score;
  }

  /**
   * Calculate the idf (inverse document frequency) of a word in the dataset.
   *
   * @param word word to calculate idf on
   * @return idf value
   */
  private double idf(String word) {
    return totalNumberOfDocs > 0 ? Math.log10((double) totalNumberOfDocs / wordToDocumentCount.get(word)) : 0;
  }
}
