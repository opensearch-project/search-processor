package org.opensearch.search.relevance.model.dto;

import java.util.List;

public class OriginalHit {
  private String id;
  private float score;
  private List<String> topPassages;

  public OriginalHit(String id, float score, List<String> topPassages) {
    this.id = id;
    this.score = score;
    this.topPassages = topPassages;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public float getScore() {
    return score;
  }

  public void setScore(float score) {
    this.score = score;
  }

  public List<String> getTopPassages() {
    return topPassages;
  }

  public void setTopPassages(List<String> topPassages) {
    this.topPassages = topPassages;
  }
}
