/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer;

public enum ResultTransformerType {
  KENDRA_INTELLIGENT_RANKING("kendra_intelligent_ranking");

  private final String type;

  ResultTransformerType(String type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return type;
  }

  public static ResultTransformerType fromString(String type) {
    for (ResultTransformerType resultTransformerType : values()) {
      if (resultTransformerType.type.equalsIgnoreCase(type)) {
        return resultTransformerType;
      }
    }
    throw new IllegalArgumentException("Unrecognized Result Transformer type [" + type + "]");
  }
}
