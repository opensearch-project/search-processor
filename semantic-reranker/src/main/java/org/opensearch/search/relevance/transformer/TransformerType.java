/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer;

public enum TransformerType {
  RESULT_TRANSFORMER("result_transformer");

  private final String type;

  TransformerType(String type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return type;
  }

  public static TransformerType fromString(String type) {
    for (TransformerType transformerType : values()) {
      if (transformerType.type.equalsIgnoreCase(type)) {
        return transformerType;
      }
    }
    throw new IllegalArgumentException("Unrecognized Transformer type [" + type + "]");
  }
}
