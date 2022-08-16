package org.opensearch.search.relevance.model.dto;

import java.util.List;

public class RerankRequest {
  private String queryText;
  private List<OriginalHit> originalHits;

  public RerankRequest(String queryText, List<OriginalHit> originalHits) {
    this.queryText = queryText;
    this.originalHits = originalHits;
  }

  public String getQueryText() {
    return queryText;
  }

  public void setQueryText(String queryText) {
    this.queryText = queryText;
  }

  public List<OriginalHit> getOriginalHits() {
    return originalHits;
  }

  public void setOriginalHits(List<OriginalHit> originalHits) {
    this.originalHits = originalHits;
  }
}
