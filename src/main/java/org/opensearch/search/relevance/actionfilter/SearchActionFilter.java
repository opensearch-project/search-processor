/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.actionfilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.OpenSearchException;
import org.opensearch.action.ActionListener;
import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.search.SearchAction;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchResponseSections;
import org.opensearch.action.support.ActionFilter;
import org.opensearch.action.support.ActionFilterChain;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.io.stream.NamedWriteableAwareStreamInput;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.settings.Setting;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.aggregations.InternalAggregations;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.fetch.subphase.FetchSourceContext;
import org.opensearch.search.internal.InternalSearchResponse;
import org.opensearch.search.profile.SearchProfileShardResults;
import org.opensearch.search.relevance.client.OpenSearchClient;
import org.opensearch.search.relevance.configuration.ConfigurationUtils;
import org.opensearch.search.relevance.configuration.ResultTransformerConfiguration;
import org.opensearch.search.relevance.configuration.ResultTransformerConfigurationFactory;
import org.opensearch.search.relevance.transformer.ResultTransformer;
import org.opensearch.tasks.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SearchActionFilter implements ActionFilter {
    private static final Logger logger = LogManager.getLogger(SearchActionFilter.class);

    private final int order;

    private final NamedWriteableRegistry namedWriteableRegistry;
    private final Map<String, ResultTransformer> resultTransformerMap;
    private final OpenSearchClient openSearchClient;

    public SearchActionFilter(Collection<ResultTransformer> supportedResultTransformers,
                              OpenSearchClient openSearchClient) {
        order = 10; // TODO: Finalize this value
        namedWriteableRegistry = new NamedWriteableRegistry(Collections.emptyList());
        resultTransformerMap = supportedResultTransformers.stream()
                .collect(Collectors.toMap(t -> t.getConfigurationFactory().getName(), t -> t));
        this.openSearchClient = openSearchClient;
    }

    @Override
    public int order() {
        return order;
    }

    @Override
    public <Request extends ActionRequest, Response extends ActionResponse> void apply(
            final Task task,
            final String action,
            final Request request,
            final ActionListener<Response> listener,
            final ActionFilterChain<Request, Response> chain) {

        final long startTime = System.nanoTime();

        if (!SearchAction.INSTANCE.name().equals(action)) {
            chain.proceed(task, action, request, listener);
            return;
        }

        SearchRequest searchRequest = (SearchRequest) request;

        // TODO: Remove originalSearchSource and replace with a deep copy of the SearchRequest object
        // once https://github.com/opensearch-project/OpenSearch/issues/869 is implemented
        SearchSourceBuilder originalSearchSource = null;
        if (searchRequest.source() != null) {
            originalSearchSource = searchRequest.source().shallowCopy();
            if (searchRequest.source().fetchSource() != null) {
                FetchSourceContext fetchSourceContext = searchRequest.source().fetchSource();
                // Clone the fetchSource
                originalSearchSource.fetchSource(new FetchSourceContext(fetchSourceContext.fetchSource(),
                        fetchSourceContext.includes(), fetchSourceContext.excludes()));
            }
        }

        final String[] indices = searchRequest.indices();
        // Skip if no, or more than 1, index is specified.
        if (indices == null || indices.length != 1) {
            chain.proceed(task, action, request, listener);
            return;
        }

        List<ResultTransformerConfiguration> resultTransformerConfigurations =
                getResultTransformerConfigurations(indices[0], searchRequest);

        LinkedHashMap<ResultTransformer, ResultTransformerConfiguration> orderedTransformersAndConfigs = new LinkedHashMap<>();
        for (ResultTransformerConfiguration config : resultTransformerConfigurations) {
            ResultTransformer resultTransformer = resultTransformerMap.get(config.getTransformerName());
            // TODO: Should transformers make a decision based on the original request or the request they receive in the chain
            if (resultTransformer.shouldTransform(searchRequest, config)) {
                searchRequest = resultTransformer.preprocessRequest(searchRequest, config);
                orderedTransformersAndConfigs.put(resultTransformer, config);
            }
        }

        if (!orderedTransformersAndConfigs.isEmpty()) {
            final ActionListener<Response> searchResponseListener = createSearchResponseListener(
                    listener, startTime, orderedTransformersAndConfigs, searchRequest, originalSearchSource);
            chain.proceed(task, action, request, searchResponseListener);
            return;
        }

        chain.proceed(task, action, request, listener);
    }

    /**
     * Parse and return a list of result transformers from request and index level configurations
     * Request level configuration takes precedence over index level
     *
     * @param indexName     name of the OpenSearch index
     * @param searchRequest input request
     * @return ordered and validated list of result transformers, empty list if not specified at
     * either request or index level
     */
    private List<ResultTransformerConfiguration> getResultTransformerConfigurations(
            final String indexName,
            final SearchRequest searchRequest) {

        List<ResultTransformerConfiguration> configs = new ArrayList<>();

        // Request level configuration takes precedence over index level
        configs = ConfigurationUtils.getResultTransformersFromRequestConfiguration(searchRequest);
        if (!configs.isEmpty()) {
            return configs;
        }

        // Fetch all index settings for this plugin
        String[] settingNames = resultTransformerMap.values()
                .stream()
                .flatMap(t -> t.getTransformerSettings()
                        .stream()
                        .map(Setting::getKey))
                .toArray(String[]::new);

        configs = ConfigurationUtils.getResultTransformersFromIndexConfiguration(
                openSearchClient.getIndexSettings(indexName, settingNames), resultTransformerMap);

        return configs;
    }

    /**
     * Create a Listener that, during the OpenSearch response chain,
     * calls external service Kendra Ranking to rerank OpenSearch hits
     *
     * @param listener                      default listened
     * @param startTime                     time when request was received, used to calculate latency added by reranking
     * @param searchRequest                 input search request
     * @param orderedTransformersAndConfigs transformers to apply, with their corresponding configurations
     * @param originalSearchSource          original search source without any modifications made by transformers
     * @param <Response>                    OpenSearch response type
     * @return ActionListener with override for onResponse method
     */
    private <Response extends ActionResponse> ActionListener<Response> createSearchResponseListener(
            final ActionListener<Response> listener,
            final long startTime,
            final LinkedHashMap<ResultTransformer, ResultTransformerConfiguration> orderedTransformersAndConfigs,
            final SearchRequest searchRequest,
            final SearchSourceBuilder originalSearchSource) {
        return new ActionListener<Response>() {

            @Override
            public void onResponse(final Response response) {
                final SearchResponse searchResponse = (SearchResponse) response;
                final long totalHits = searchResponse.getHits().getTotalHits().value;
                if (totalHits == 0) {
                    logger.info("TotalHits = 0. Returning search response without transforming.");
                    listener.onResponse(response);
                    return;
                }

                logger.debug("Starting re-ranking for search response: {}", searchResponse);
                try {

                    // Clone search hits (by serializing + deserializing) before transforming
                    final BytesStreamOutput out = new BytesStreamOutput();
                    searchResponse.getHits().writeTo(out);
                    final StreamInput in = new NamedWriteableAwareStreamInput(out.bytes().streamInput(),
                            namedWriteableRegistry);
                    SearchHits hits = new SearchHits(in);

                    for (Map.Entry<ResultTransformer, ResultTransformerConfiguration> entry : orderedTransformersAndConfigs.entrySet()) {
                        long startTime = System.nanoTime();
                        hits = entry.getKey().transform(hits, searchRequest, entry.getValue());
                        long timeTookMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                        logger.info(entry.getValue().getTransformerName() + ": took " + timeTookMillis + " ms");
                    }

                    List<SearchHit> searchHitsList = Arrays.asList(hits.getHits());
                    if (originalSearchSource != null) {
                        if (originalSearchSource.fetchSource() != null &&
                                !originalSearchSource.fetchSource().fetchSource()) {
                            searchHitsList = searchHitsList.stream()
                                    .map(hit -> hit.sourceRef(null))
                                    .collect(Collectors.toList());
                        }
                        if (originalSearchSource.from() >= 0 && originalSearchSource.size() >= 0) {
                            final int lastHitIndex = Math.min(searchHitsList.size(),
                                    (originalSearchSource.from() + originalSearchSource.size()));
                            if (originalSearchSource.from() > lastHitIndex) {
                                searchHitsList = Collections.emptyList();
                            } else {
                                searchHitsList = searchHitsList.subList(originalSearchSource.from(), lastHitIndex);
                            }
                        }
                    }

                    hits = new SearchHits(
                            searchHitsList.toArray(new SearchHit[0]),
                            hits.getTotalHits(),
                            hits.getMaxScore());

                    final SearchResponseSections internalResponse = new InternalSearchResponse(hits,
                            (InternalAggregations) searchResponse.getAggregations(), searchResponse.getSuggest(),
                            new SearchProfileShardResults(searchResponse.getProfileResults()), searchResponse.isTimedOut(),
                            searchResponse.isTerminatedEarly(), searchResponse.getNumReducePhases());

                    final long tookInMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                    final SearchResponse newResponse = new SearchResponse(internalResponse, searchResponse.getScrollId(),
                            searchResponse.getTotalShards(), searchResponse.getSuccessfulShards(),
                            searchResponse.getSkippedShards(), tookInMillis, searchResponse.getShardFailures(),
                            searchResponse.getClusters());
                    listener.onResponse((Response) newResponse);
                } catch (final Exception e) {
                    logger.error("Result transformer operations failed.", e);
                    throw new OpenSearchException("Result transformer operations failed.", e);
                }
            }

            @Override
            public void onFailure(final Exception e) {
                listener.onFailure(e);
            }
        };
    }
}
