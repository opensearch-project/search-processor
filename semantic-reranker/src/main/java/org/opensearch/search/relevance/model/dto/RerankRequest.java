/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.model.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class RerankRequest {
  private String rerankingEndpointId;
  private String queryText;
  private List<Document> documents;

  public RerankRequest(String rerankingEndpointId, String queryText, List<Document> documents) {
    this.rerankingEndpointId = rerankingEndpointId;
    this.queryText = queryText;
    this.documents = documents;
  }

  public String getRerankingEndpointId() {
    return rerankingEndpointId;
  }

  public void setRerankingEndpointId(String rerankingEndpointId) {
    this.rerankingEndpointId = rerankingEndpointId;
  }

  public String getQueryText() {
    return queryText;
  }

  public void setQueryText(String queryText) {
    this.queryText = queryText;
  }

  public List<Document> getDocuments() {
    return documents;
  }

  public void setDocuments(List<Document> documents) {
    this.documents = documents;
  }
}
