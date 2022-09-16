/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.model.dto;

public class RerankedHit {
  private String id;
  private float score;

  public String getId() {
    return id;
  }

  public float getScore() {
    return score;
  }
}
