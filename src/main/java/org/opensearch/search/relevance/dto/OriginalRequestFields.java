/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.dto;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.search.SearchService;
import org.opensearch.search.fetch.subphase.FetchSourceContext;

/**
 * This class is intended to be a temporary measure until https://github.com/opensearch-project/OpenSearch/issues/869
 * is implemented and a deep copy of a SearchRequest can easily be made. Meanwhile, all request fields that are over-written
 * in preprocessRequest of any {@link org.opensearch.search.relevance.transformer.ResultTransformer} should be included here
 */
public class OriginalRequestFields {
  private int from;
  private int size;
  private FetchSourceContext fetchSourceContext;

  public OriginalRequestFields(final SearchRequest searchRequest) {
    this.from = searchRequest.source().from() == -1 ? SearchService.DEFAULT_FROM : searchRequest.source().from();
    this.size = searchRequest.source().size() == -1 ? SearchService.DEFAULT_SIZE : searchRequest.source().size();
    this.fetchSourceContext = searchRequest.source().fetchSource();
  }

  public int from() {
    return this.from;
  }

  public int size() {
    return this.size;
  }

  public FetchSourceContext fetchSourceContext() {
    return this.fetchSourceContext;
  }
}
