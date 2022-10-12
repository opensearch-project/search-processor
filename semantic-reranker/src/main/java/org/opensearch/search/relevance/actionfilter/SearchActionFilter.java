/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.actionfilter;

import static org.opensearch.action.search.ShardSearchFailure.readShardSearchFailure;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import org.opensearch.search.relevance.preprocess.QueryParser;
import org.opensearch.search.relevance.preprocess.QueryParser.QueryParserResult;
import org.opensearch.search.relevance.ranker.Ranker;
import org.opensearch.search.suggest.Suggest;
import org.opensearch.tasks.Task;

public class SearchActionFilter implements ActionFilter {
  private static final Logger logger = LogManager.getLogger(SearchActionFilter.class);

  private final int order;

  private final NamedWriteableRegistry namedWriteableRegistry;
  private final QueryParser queryParser;
  private final Ranker ranker;

  public SearchActionFilter(Ranker ranker) {
    order = 10; // TODO: Finalize this value
    namedWriteableRegistry = new NamedWriteableRegistry(Collections.emptyList());
    queryParser = new QueryParser();
    this.ranker = ranker;
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

    boolean shouldRerank = this.ranker.shouldRescore(searchRequest);
    if (shouldRerank) {
      // Source is returned in response hits by default. If disabled by the user, overwrite and enable
      // in order to access document contents for reranking, then suppress at response time.
      boolean suppressSourceOnResponse = false;
      if (searchRequest.source().fetchSource() != null && !searchRequest.source().fetchSource().fetchSource()) {
        searchRequest.source().fetchSource(true);
        suppressSourceOnResponse = true;
      }

      // Extract query string from request
      final QueryParserResult queryParserResult = ranker.parseQuery(searchRequest);
      if (queryParserResult != null) {
        final ActionListener<Response> searchResponseListener = createSearchResponseListener(
            listener, startTime, queryParserResult, suppressSourceOnResponse);
        chain.proceed(task, action, request, searchResponseListener);
        return;
      }
    }

    chain.proceed(task, action, request, listener);
  }

  private <Response extends ActionResponse> ActionListener<Response> createSearchResponseListener(
      final ActionListener<Response> listener,
      final long startTime,
      final QueryParser.QueryParserResult queryParserResult,
      final boolean suppressSourceOnResponse) {
    return new ActionListener<Response>() {

      @Override
      public void onResponse(final Response response) {
        final SearchResponse searchResponse = (SearchResponse) response;
        final long totalHits = searchResponse.getHits().getTotalHits().value;
        if (totalHits == 0) {
          logger.info("TotalHits = 0. Returning search response without re-ranking.", searchResponse);
          listener.onResponse(response);
          return;
        }

        logger.info("Starting re-ranking for search response: {}, parsed query: {}", searchResponse, queryParserResult);
        try {
          final BytesStreamOutput out = new BytesStreamOutput();
          searchResponse.writeTo(out);

          final StreamInput in = new NamedWriteableAwareStreamInput(out.bytes().streamInput(), namedWriteableRegistry);

          final SearchHits hits = new SearchHits(in);
          SearchHits newHits = ranker.rescore(hits, queryParserResult);
          if (suppressSourceOnResponse) {
            List<SearchHit> hitsWithModifiedSource = Arrays.stream(newHits.getHits())
                .map(hit -> hit.sourceRef(null))
                .collect(Collectors.toList());
            newHits = new SearchHits(hitsWithModifiedSource.toArray(new SearchHit[hitsWithModifiedSource.size()]),
                newHits.getTotalHits(),
                newHits.getMaxScore());
          }

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
          final Response newResponse = (Response) new SearchResponse(internalResponse, scrollId, totalShards, successfulShards,
              skippedShards, tookInMillis, shardFailures, clusters);
          listener.onResponse(newResponse);

          // TODO: Change this to a metric
          logger.info("Re-ranking overhead time: {}ms", tookInMillis, searchResponse.getTook().getMillis(),
              tookInMillis - searchResponse.getTook().getMillis());
        } catch (final Exception e) {
          logger.error("Failed to parse search response.", e);
          throw new OpenSearchException("Failed to parse a search response.", e);
        }
      }

      @Override
      public void onFailure(final Exception e) {
        listener.onFailure(e);
      }
    };
  }
}
