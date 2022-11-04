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
public class Document {
  
  private String id;
  private String groupId;
  private List<String> tokenizedTitle;
  private List<String> tokenizedBody;
  private Float originalScore;

  public Document(String id, String groupId, List<String> tokenizedTitle, List<String> tokenizedBody,
      Float originalScore) {
    this.id = id;
    this.groupId = groupId;
    this.tokenizedTitle = tokenizedTitle;
    this.tokenizedBody = tokenizedBody;
    this.originalScore = originalScore;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public List<String> getTokenizedTitle() {
    return tokenizedTitle;
  }

  public void setTokenizedTitle(List<String> tokenizedTitle) {
    this.tokenizedTitle = tokenizedTitle;
  }

  public List<String> getTokenizedBody() {
    return tokenizedBody;
  }

  public void setTokenizedBody(List<String> tokenizedBody) {
    this.tokenizedBody = tokenizedBody;
  }

  public Float getOriginalScore() {
    return originalScore;
  }

  public void setOriginalScore(Float originalScore) {
    this.originalScore = originalScore;
  }
}
