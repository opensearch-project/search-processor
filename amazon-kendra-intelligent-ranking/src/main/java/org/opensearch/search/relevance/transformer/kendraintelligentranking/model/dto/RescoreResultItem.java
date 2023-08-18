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

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class RescoreResultItem {
  private String documentId;
  private Float score;

  public String getDocumentId() {
    return documentId;
  }

  public Float getScore() {
    return score;
  }

  /**
   * Setter for unit tests.
   * @param documentId the ID of the rescored document.
   */
  public void setDocumentId(String documentId) {
    this.documentId = documentId;
  }

  /**
   * Setter for unit tests.
   * @param score the updated score of the document.
   */
  public void setScore(Float score) {
    this.score = score;
  }
}
