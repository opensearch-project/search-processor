package org.opensearch.search.relevance.model;

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
