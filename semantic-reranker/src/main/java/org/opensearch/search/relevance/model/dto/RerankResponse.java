package org.opensearch.search.relevance.model.dto;

import java.util.List;

public class RerankResponse {
  private List<RerankedHit> hits;

  public List<RerankedHit> getHits() {
    return hits;
  }
}
