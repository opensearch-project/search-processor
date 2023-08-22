/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.configuration;

import static org.opensearch.search.relevance.configuration.Constants.ORDER;
import static org.opensearch.search.relevance.configuration.Constants.PROPERTIES;

import org.opensearch.common.io.stream.Writeable;
import org.opensearch.core.ParseField;
import org.opensearch.core.xcontent.ToXContentObject;

public abstract class TransformerConfiguration implements Writeable, ToXContentObject {
  protected static final ParseField TRANSFORMER_ORDER = new ParseField(ORDER);
  protected static final ParseField TRANSFORMER_PROPERTIES = new ParseField(PROPERTIES);

  protected int order;

  public int getOrder() {
    return this.order;
  }

  public void setOrder(final int order) {
    this.order = order;
  }
}
