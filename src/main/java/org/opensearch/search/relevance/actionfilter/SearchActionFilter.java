/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.actionfilter;

import static org.opensearch.action.search.ShardSearchFailure.readShardSearchFailure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import org.opensearch.action.search.ShardSearchFailure;
import org.opensearch.action.support.ActionFilter;
import org.opensearch.action.support.ActionFilterChain;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.io.stream.NamedWriteableAwareStreamInput;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.aggregations.InternalAggregations;
import org.opensearch.search.internal.InternalSearchResponse;
import org.opensearch.search.profile.SearchProfileShardResults;
import org.opensearch.search.relevance.configuration.ConfigurationUtils;
import org.opensearch.search.relevance.client.OpenSearchClient;
import org.opensearch.search.relevance.configuration.ResultTransformerConfiguration;
import org.opensearch.search.relevance.transformer.ResultTransformer;
import org.opensearch.search.relevance.transformer.ResultTransformerType;
import org.opensearch.search.suggest.Suggest;
import org.opensearch.tasks.Task;

public class SearchActionFilter implements ActionFilter {
  private static final Logger logger = LogManager.getLogger(SearchActionFilter.class);

  private final int order;

  private final NamedWriteableRegistry namedWriteableRegistry;
  private final Map<ResultTransformerType, ResultTransformer> supportedResultTransformers;
  private final OpenSearchClient openSearchClient;

  public SearchActionFilter(Map<ResultTransformerType, ResultTransformer> supportedResultTransformers, OpenSearchClient openSearchClient) {
    order = 10; // TODO: Finalize this value
    namedWriteableRegistry = new NamedWriteableRegistry(Collections.emptyList());
    this.supportedResultTransformers = supportedResultTransformers;
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

    final SearchRequest searchRequest = (SearchRequest) request;

    final String[] indices = searchRequest.indices();
    // Skip if no, or more than 1, index is specified.
    if (indices == null || indices.length != 1) {
      chain.proceed(task, action, request, listener);
      return;
    }

    List<ResultTransformerConfiguration> resultTransformerConfigurations = getResultTransformerConfigurations(indices[0], searchRequest);

    LinkedHashMap<ResultTransformer, ResultTransformerConfiguration> orderedTransformersAndConfigs = new LinkedHashMap<>();
    for (ResultTransformerConfiguration config : resultTransformerConfigurations) {
      ResultTransformer resultTransformer = supportedResultTransformers.get(config.getType());
      if (resultTransformer.shouldTransform(searchRequest, config)) {
        orderedTransformersAndConfigs.put(resultTransformer, config);
      }
    }

    if (!orderedTransformersAndConfigs.isEmpty()) {
      // Source is returned in response hits by default. If disabled by the user, overwrite and enable
      // in order to access document contents for reranking, then suppress at response time.
      boolean suppressSourceOnResponse = false;
      if (searchRequest.source().fetchSource() != null && !searchRequest.source().fetchSource().fetchSource()) {
        searchRequest.source().fetchSource(true);
        suppressSourceOnResponse = true;
      }

      final ActionListener<Response> searchResponseListener = createSearchResponseListener(
          listener, startTime, searchRequest, orderedTransformersAndConfigs, suppressSourceOnResponse);
      chain.proceed(task, action, request, searchResponseListener);
      return;
    }

    chain.proceed(task, action, request, listener);
  }

  /**
   * Parse and return a list of result transformers from request and index level configurations
   * Request level configuration takes precedence over index level
   * @param indexName name of the OpenSearch index
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
    String[] settingNames = supportedResultTransformers.values()
        .stream()
        .map(t -> t.getTransformerSettings()
            .stream()
            .map(s -> s.getKey())
            .collect(Collectors.toList()))
        .flatMap(Collection::stream)
        .collect(Collectors.toList())
        .toArray(new String[0]);

    configs = ConfigurationUtils.getResultTransformersFromIndexConfiguration(
        openSearchClient.getIndexSettings(indexName, settingNames));

    return configs;
  }

  /**
   * Create a Listener that, during the OpenSearch response chain,
   * calls external service Kendra Ranking to rerank OpenSearch hits
   * @param listener default listened
   * @param startTime time when request was received, used to calculate latency added by reranking
   * @param searchRequest input search request
   * @param orderedTransformersAndConfigs transformers to apply, with their corresponding configurations
   * @param suppressSourceOnResponse boolean indicating whether to suppress the document source on response
   * @param <Response> OpenSearch response type
   * @return ActionListener with override for onResponse method
   */
  private <Response extends ActionResponse> ActionListener<Response> createSearchResponseListener(
      final ActionListener<Response> listener,
      final long startTime,
      final SearchRequest searchRequest,
      final LinkedHashMap<ResultTransformer, ResultTransformerConfiguration> orderedTransformersAndConfigs,
      final boolean suppressSourceOnResponse) {
    return new ActionListener<Response>() {

      @Override
      public void onResponse(final Response response) {
        final SearchResponse searchResponse = (SearchResponse) response;
        final long totalHits = searchResponse.getHits().getTotalHits().value;
        if (totalHits == 0) {
          logger.info("TotalHits = 0. Returning search response without re-ranking.");
          listener.onResponse(response);
          return;
        }

        logger.debug("Starting re-ranking for search response: {}", searchResponse);
        try {
          final BytesStreamOutput out = new BytesStreamOutput();
          searchResponse.writeTo(out);

          final StreamInput in = new NamedWriteableAwareStreamInput(out.bytes().streamInput(),
              namedWriteableRegistry);

          SearchHits hits = new SearchHits(in);
          for (Map.Entry<ResultTransformer, ResultTransformerConfiguration> entry : orderedTransformersAndConfigs.entrySet()) {
            hits = entry.getKey().transform(hits, searchRequest, entry.getValue());
          }

          if (suppressSourceOnResponse) {
            List<SearchHit> hitsWithModifiedSource = Arrays.stream(hits.getHits())
                .map(hit -> hit.sourceRef(null))
                .collect(Collectors.toList());
            hits = new SearchHits(
                hitsWithModifiedSource.toArray(new SearchHit[hitsWithModifiedSource.size()]),
                hits.getTotalHits(),
                hits.getMaxScore());
          }

          final InternalAggregations aggregations =
              in.readBoolean() ? InternalAggregations.readFrom(in) : null;
          final Suggest suggest = in.readBoolean() ? new Suggest(in) : null;
          final boolean timedOut = in.readBoolean();
          final Boolean terminatedEarly = in.readOptionalBoolean();
          final SearchProfileShardResults profileResults = in.readOptionalWriteable(
              SearchProfileShardResults::new);
          final int numReducePhases = in.readVInt();

          final SearchResponseSections internalResponse = new InternalSearchResponse(hits,
              aggregations, suggest,
              profileResults, timedOut, terminatedEarly, numReducePhases);

          final int totalShards = in.readVInt();
          final int successfulShards = in.readVInt();
          final int shardSearchFailureSize = in.readVInt();
          final ShardSearchFailure[] shardFailures;
          if (shardSearchFailureSize == 0) {
            shardFailures = ShardSearchFailure.EMPTY_ARRAY;
          } else {
            shardFailures = new ShardSearchFailure[shardSearchFailureSize];
            for (int i = 0; i < shardFailures.length; i++) {
              shardFailures[i] = readShardSearchFailure(in);
            }
          }

          final SearchResponse.Clusters clusters = new SearchResponse.Clusters(in.readVInt(),
              in.readVInt(), in.readVInt());
          final String scrollId = in.readOptionalString();
          final int skippedShards = in.readVInt();

          final long tookInMillis = (System.nanoTime() - startTime) / 1000000;
          final SearchResponse newResponse = new SearchResponse(internalResponse, scrollId,
              totalShards, successfulShards,
              skippedShards, tookInMillis, shardFailures, clusters);
          listener.onResponse((Response) newResponse);

          // TODO: Change this to a metric
          logger.info("Re-ranking overhead time: {}ms",
              tookInMillis - searchResponse.getTook().getMillis());
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
