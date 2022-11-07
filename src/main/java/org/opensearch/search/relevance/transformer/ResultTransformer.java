/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer;

import java.util.List;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.common.settings.Setting;
import org.opensearch.search.SearchHits;
import org.opensearch.search.relevance.configuration.ResultTransformerConfiguration;

public interface ResultTransformer {

  /**
   * Get the list of settings required / supported by the transformer
   * @return list of transformer settings
   */
  List<Setting<?>> getTransformerSettings();

  /**
   * Decide whether to apply the transformer on the input request
   * @param request input request
   * @param configuration Configuration parameters for the transformer
   * @return boolean decision on whether to apply the transformer
   */
  boolean shouldTransform(final SearchRequest request, final ResultTransformerConfiguration configuration);

  /**
   * Rank hits based on the provided query
   * @param hits hits to be re-ranked
   * @param request Search request
   * @param configuration Configuration parameters for the transformer
   * @return SearchHits ordered by score generated by ranker
   */
  SearchHits transform(final SearchHits hits, final SearchRequest request, final ResultTransformerConfiguration configuration);
}