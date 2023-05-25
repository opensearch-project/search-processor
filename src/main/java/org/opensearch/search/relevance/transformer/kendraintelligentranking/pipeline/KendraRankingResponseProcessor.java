/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.relevance.transformer.kendraintelligentranking.pipeline;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchResponseSections;
import org.opensearch.ingest.ConfigurationUtils;
import org.opensearch.search.SearchHits;
import org.opensearch.search.aggregations.InternalAggregations;
import org.opensearch.search.internal.InternalSearchResponse;
import org.opensearch.search.pipeline.Processor;
import org.opensearch.search.pipeline.SearchResponseProcessor;
import org.opensearch.search.profile.SearchProfileShardResults;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.KendraIntelligentRanker;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.client.KendraClientSettings;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.client.KendraHttpClient;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.KendraIntelligentRankingConfiguration;

import static org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.Constants.KENDRA_DEFAULT_DOC_LIMIT;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This is a {@link SearchResponseProcessor} that applies kendra intelligence ranking
 */
public class KendraRankingResponseProcessor implements SearchResponseProcessor {
    /**
     * key to reference this processor type from a search pipeline
     */
    public static final String TYPE = "kendra_ranking";
    private final List<String> titleField;
    private final List<String> bodyField;
    private final int docLimit;
    private final String tag;
    private final String description;
    private final KendraHttpClient kendraClient;

    private static final Logger logger = LogManager.getLogger(KendraRankingResponseProcessor.class);

    /**
     * Constructor that apply configuration for kendra re-ranking
     *
     * @param tag            processor tag
     * @param description    processor description
     * @param titleField     titleField applied to kendra re-ranking
     * @param bodyField      bodyField applied to kendra re-ranking
     * @param inputDocLimit  docLimit applied to kendra re-ranking
     * @param kendraClient   kendraClient to connect with kendra
     */
    public KendraRankingResponseProcessor(String tag, String description, List<String> titleField, List<String> bodyField, Integer inputDocLimit, KendraHttpClient kendraClient) {
        super();
        this.titleField = titleField;
        this.bodyField = bodyField;
        this.tag = tag;
        this.description = description;
        this.kendraClient = kendraClient;
        int docLimit;
        if (inputDocLimit == null) {
            docLimit = KENDRA_DEFAULT_DOC_LIMIT;
        } else {
            docLimit = inputDocLimit;
        }
        this.docLimit = docLimit;
    }

    /**
     * Gets the type of the processor.
     */
    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Gets the tag of a processor.
     */
    @Override
    public String getTag() {
        return tag;
    }

    /**
     * Gets the description of a processor.
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Transform the response hit and apply kendra re-ranking logic
     */
    @Override
    public SearchResponse processResponse(SearchRequest request, SearchResponse response) throws Exception {
        SearchHits hits = response.getHits();

        if (hits.getHits().length == 0) {
            // Avoid call to re-rank empty results
            logger.info("TotalHits = 0. Returning search response without transforming.");
            return response;
        }

        KendraIntelligentRankingConfiguration.KendraIntelligentRankingProperties properties = new KendraIntelligentRankingConfiguration.KendraIntelligentRankingProperties(bodyField, titleField, docLimit);
        KendraIntelligentRankingConfiguration configuration = new KendraIntelligentRankingConfiguration(1, properties);
        KendraIntelligentRanker ranker = new KendraIntelligentRanker(this.kendraClient);
        SearchRequest processedRequest = ranker.preprocessRequest(request, configuration);

        if (ranker.shouldTransform(processedRequest, configuration)) {
            long startTime = System.nanoTime();
            SearchHits reRankedSearchHits = ranker.transform(hits, processedRequest, configuration);
            long timeTookMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            final SearchResponseSections internalResponse = new InternalSearchResponse(reRankedSearchHits,
                    (InternalAggregations) response.getAggregations(), response.getSuggest(),
                    new SearchProfileShardResults(response.getProfileResults()), response.isTimedOut(),
                    response.isTerminatedEarly(), response.getNumReducePhases());

            final SearchResponse newResponse = new SearchResponse(internalResponse, response.getScrollId(),
                    response.getTotalShards(), response.getSuccessfulShards(),
                    response.getSkippedShards(), timeTookMillis, response.getShardFailures(),
                    response.getClusters());
            logger.info("kendra ranking processor took " + timeTookMillis + " ms");
            return newResponse;
        } else
            return response;
    }

    /**
     * This is a factor that creates the KendraRankingResponseProcessor
     */
    public static final class Factory implements Processor.Factory<SearchResponseProcessor>  {

        private final KendraClientSettings clientSettings;

        /**
         * Constructor for factory
         * @param kendraClientSettings credentials to create kendra client
         */
        public Factory(KendraClientSettings kendraClientSettings) {
            this.clientSettings = kendraClientSettings;
        }

        public KendraRankingResponseProcessor create(
                Map<String, Processor.Factory<SearchResponseProcessor>> processorFactories,
                String tag,
                String description,
                Map<String, Object> config
        ) throws Exception {
            List<String> titleField = Collections.singletonList(ConfigurationUtils.readOptionalStringProperty(TYPE, tag, config, "title_field"));
            List<String> bodyField = Collections.singletonList(ConfigurationUtils.readStringProperty(TYPE, tag, config, "body_field"));
            String inputDocLimit = ConfigurationUtils.readOptionalStringOrIntProperty(TYPE, tag, config, "doc_limit");
            KendraHttpClient kendraClient = new KendraHttpClient(this.clientSettings);
            int docLimit;
            if (inputDocLimit == null) {
                docLimit = KENDRA_DEFAULT_DOC_LIMIT;
            } else {
                docLimit = Integer.parseInt(inputDocLimit);
            }
            return new KendraRankingResponseProcessor(tag, description, titleField, bodyField, docLimit, kendraClient);
        }
    }
}
