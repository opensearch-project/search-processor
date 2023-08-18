/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking.model;

import java.io.IOException;
import org.opensearch.OpenSearchException;
import org.opensearch.core.common.io.stream.StreamInput;

public class KendraIntelligentRankingException extends OpenSearchException {
  public KendraIntelligentRankingException(StreamInput in) throws IOException {
    super(in);
  }

  public KendraIntelligentRankingException(String message) {
    super(message);
  }

}
