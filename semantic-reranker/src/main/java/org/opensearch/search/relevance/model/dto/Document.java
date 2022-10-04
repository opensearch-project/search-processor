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
public class Document {
  
  private String id;
  private String title;
  private List<String> passages;
  private List<String> tokenizedTitle;
  private List<List<String>> tokenizedBodyPassages;
  private Float originalScore;

  public Document(String id, String title, List<String> passages, List<String> tokenizedTitle, List<List<String>> tokenizedBodyPassages,
      Float originalScore) {
    this.id = id;
    this.title = title;
    this.passages = passages;
    this.tokenizedTitle = tokenizedTitle;
    this.tokenizedBodyPassages = tokenizedBodyPassages;
    this.originalScore = originalScore;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public List<String> getPassages() {
    return passages;
  }

  public void setPassages(List<String> passages) {
    this.passages = passages;
  }

  public List<String> getTokenizedTitle() {
    return tokenizedTitle;
  }

  public void setTokenizedTitle(List<String> tokenizedTitle) {
    this.tokenizedTitle = tokenizedTitle;
  }

  public List<List<String>> getTokenizedBodyPassages() {
    return tokenizedBodyPassages;
  }

  public void setTokenizedBodyPassages(List<List<String>> tokenizedBodyPassages) {
    this.tokenizedBodyPassages = tokenizedBodyPassages;
  }

  public Float getOriginalScore() {
    return originalScore;
  }

  public void setOriginalScore(Float originalScore) {
    this.originalScore = originalScore;
  }
}
