/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.reranker;

import org.opensearch.search.SearchHits;

public interface PersonalizedRanker {

    /**
     * Re rank search hits
     * @param hits Search hits to re rank
     * @return Re ranked search hits
     */
    public SearchHits rerank(SearchHits hits);

}
