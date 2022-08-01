/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.actionfilter;

import org.opensearch.OpenSearchException;
import org.opensearch.action.ActionListener;
import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.admin.indices.analyze.AnalyzeAction.Response;
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
import org.opensearch.search.relevance.preprocess.SlidingWindowTextSplitter;
import org.opensearch.search.suggest.Suggest;
import org.opensearch.tasks.Task;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.opensearch.action.search.ShardSearchFailure.readShardSearchFailure;

public class SearchActionFilter implements ActionFilter {

  // TODO: Finalize passage length and sliding window step
  private static final int PASSAGE_SIZE_LIMIT = 600;
  static final int SLIDING_WINDOW_STEP = PASSAGE_SIZE_LIMIT - 50;

  // TODO: Update log levels where required
  private static final Logger LOGGER = LogManager.getLogger(SearchActionFilter.class);

  private final int order;

  private final NamedWriteableRegistry namedWriteableRegistry;
  private final SlidingWindowTextSplitter slidingWindowTextSplitter;

  public SearchActionFilter() {
    order = 10; // TODO: Finalize this value
    namedWriteableRegistry = new NamedWriteableRegistry(Collections.emptyList());
    slidingWindowTextSplitter = new SlidingWindowTextSplitter(PASSAGE_SIZE_LIMIT, SLIDING_WINDOW_STEP);
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

    // TODO: Double check tookTime calculation
    final long startTime = System.nanoTime();

    if (!SearchAction.INSTANCE.name().equals(action)) {
      chain.proceed(task, action, request, listener);
      return;
    }

    final SearchRequest searchRequest = (SearchRequest) request;
    LOGGER.info("Applying action filter on search request: " + searchRequest);

    final ActionListener<Response> searchResponseListener = shouldDoSemanticRerank(searchRequest) ?
        createSearchResponseListener(listener, startTime) : listener;
    chain.proceed(task, action, request, searchResponseListener);
  }

  private boolean shouldDoSemanticRerank(final SearchRequest searchRequest) {
    // TODO: Check index settings or query input to determine if semantic reranking is enabled

    // Don't re-rank if scroll search is enabled
    if (searchRequest.scroll() != null) {
      return false;
    }

    // TODO: Should we re-rank if request source is empty?
    if (searchRequest.source() == null) {
      return false;
    }

    // TODO: Should we re-rank if it is a multi-index request?
    final String[] indices = searchRequest.indices();
    if (indices == null || indices.length != 1) {
      return false;
    }

    return true;
  }

  private <Response extends ActionResponse> ActionListener<Response> createSearchResponseListener(
      final ActionListener<Response> listener, final long startTime) {
    return new ActionListener<Response>() {

      @Override
      public void onResponse(final Response response) {
        final SearchResponse searchResponse = (SearchResponse) response;
        final long totalHits = searchResponse.getHits().getTotalHits().value;
        if (totalHits == 0) {
          LOGGER.info("TotalHits = 0. Returning search response without semantic reranking: {}", searchResponse);
          listener.onResponse(response);
          return;
        }

        LOGGER.info("Starting semantic reranking for search response: {}", searchResponse);
        
        try {
          final BytesStreamOutput out = new BytesStreamOutput();
          searchResponse.writeTo(out);

          final StreamInput in = new NamedWriteableAwareStreamInput(out.bytes().streamInput(), namedWriteableRegistry);

          LOGGER.info("Extracting search hits");
          final SearchHits hits = new SearchHits(in);
          final SearchHits newHits = doSemanticRerank(hits);

          LOGGER.info("Extracting remaining response fields");
          final InternalAggregations aggregations = in.readBoolean() ? InternalAggregations.readFrom(in) : null;
          final Suggest suggest = in.readBoolean() ? new Suggest(in) : null;
          final boolean timedOut = in.readBoolean();
          final Boolean terminatedEarly = in.readOptionalBoolean();
          final SearchProfileShardResults profileResults = in.readOptionalWriteable(SearchProfileShardResults::new);
          final int numReducePhases = in.readVInt();

          final SearchResponseSections internalResponse = new InternalSearchResponse(newHits, aggregations, suggest,
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

          final SearchResponse.Clusters clusters = new SearchResponse.Clusters(in.readVInt(), in.readVInt(), in.readVInt());
          final String scrollId = in.readOptionalString();
          final int skippedShards = in.readVInt();

          final long tookInMillis = (System.nanoTime() - startTime) / 1000000;

          LOGGER.info("Creating new search response");
          @SuppressWarnings("unchecked") final Response newResponse = (Response) new SearchResponse(internalResponse, scrollId, totalShards, successfulShards,
              skippedShards, tookInMillis, shardFailures, clusters);
          listener.onResponse(newResponse);

          // TODO: Change this to a metric
          LOGGER.info("Rewriting overhead time: {} - {} = {}ms", tookInMillis, searchResponse.getTook().getMillis(),
              tookInMillis - searchResponse.getTook().getMillis());
        } catch (final Exception e) {
          LOGGER.error("Failed to parse search response.", e);
          throw new OpenSearchException("Failed to parse a search response.", e);
        }
      }

      @Override
      public void onFailure(final Exception e) {
        listener.onFailure(e);
      }
    };
  }

  private SearchHits doSemanticRerank(final SearchHits hits) {
    LOGGER.info("Beginning semantic reranking of {} search hits", hits.getTotalHits());
    for (SearchHit searchHit: hits.getHits()) {
      // TODO: Refactor to separate out preprocessing
      Map<String, Object> docSourceMap = searchHit.getSourceAsMap();
      // TODO: Fetch required document fields from index settings / query request fields
      LOGGER.info("Splitting document source into passages");
      List<String> splitPassages = slidingWindowTextSplitter.split(docSourceMap.get("text").toString());
      LOGGER.info("Split document source into {} passages: {}", splitPassages.size(), splitPassages);
    }

    // TODO: Implement rescoring and reordering of hits
    return new SearchHits(hits.getHits(), hits.getTotalHits(), hits.getMaxScore());
  }

}
