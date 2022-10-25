/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking.model.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class RescoreRequest {
  private String rescoreExecutionPlanId;
  private String searchQuery;
  private List<Document> documents;

  public RescoreRequest(String rescoreExecutionPlanId, String searchQuery, List<Document> documents) {
    this.rescoreExecutionPlanId = rescoreExecutionPlanId;
    this.searchQuery = searchQuery;
    this.documents = documents;
  }

  public String getRescoreExecutionPlanId() {
    return rescoreExecutionPlanId;
  }

  public void setRescoreExecutionPlanId(String rescoreExecutionPlanId) {
    this.rescoreExecutionPlanId = rescoreExecutionPlanId;
  }

  public String getSearchQuery() {
    return searchQuery;
  }

  public void setSearchQuery(String searchQuery) {
    this.searchQuery = searchQuery;
  }

  public List<Document> getDocuments() {
    return documents;
  }

  public void setDocuments(List<Document> documents) {
    this.documents = documents;
  }
}
