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
public class RerankResult {
  private String rerankId;
  private List<RerankResultItem> resultItems;

  public String getRerankId() {
    return rerankId;
  }

  public List<RerankResultItem> getResultItems() {
    return resultItems;
  }
}
