/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking.model;

public class PassageScore {
  private double score;
  private int index;

  public PassageScore(double score, int index) {
    this.score = score;
    this.index = index;
  }

  public double getScore() {
    return score;
  }

  public int getIndex() {
    return index;
  }
}
