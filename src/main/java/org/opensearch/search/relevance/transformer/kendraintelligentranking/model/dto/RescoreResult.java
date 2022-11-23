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
public class RescoreResult {
  private String rescoreId;
  private List<RescoreResultItem> resultItems;

  public String getRescoreId() {
    return rescoreId;
  }

  public List<RescoreResultItem> getResultItems() {
    return resultItems;
  }

  /**
   * Setter used for unit tests.
   * @param rescoreId The identifier associated with the scores that Amazon Kendra Intelligent Ranking
   *                  gives to the results.
   */
  public void setRescoreId(String rescoreId) {
    this.rescoreId = rescoreId;
  }

  /**
   * Setter used for unit tests.
   * @param resultItems A list of result items for documents with new relevancy scores.
   */
  public void setResultItems(List<RescoreResultItem> resultItems) {
    this.resultItems = resultItems;
  }
}
