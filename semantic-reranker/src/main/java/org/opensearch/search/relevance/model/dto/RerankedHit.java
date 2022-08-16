package org.opensearch.search.relevance.model.dto;

public class RerankedHit {
  private String id;
  private float score;

  public String getId() {
    return id;
  }

  public float getScore() {
    return score;
  }
}
