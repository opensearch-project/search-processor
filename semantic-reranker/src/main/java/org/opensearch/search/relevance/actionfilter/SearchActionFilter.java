/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.actionfilter;

import static org.opensearch.action.search.ShardSearchFailure.readShardSearchFailure;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
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
import org.opensearch.common.settings.Settings;
import org.opensearch.search.SearchExtBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.aggregations.InternalAggregations;
import org.opensearch.search.internal.InternalSearchResponse;
import org.opensearch.search.profile.SearchProfileShardResults;
import org.opensearch.search.relevance.client.KendraHttpClient;
import org.opensearch.search.relevance.client.OpenSearchClient;
import org.opensearch.search.relevance.control.KendraSearchExtBuilder;
import org.opensearch.search.relevance.constants.Constants;
import org.opensearch.search.relevance.model.PassageScore;
import org.opensearch.search.relevance.model.dto.Document;
import org.opensearch.search.relevance.model.dto.RerankRequest;
import org.opensearch.search.relevance.model.dto.RerankResult;
import org.opensearch.search.relevance.model.dto.RerankResultItem;
import org.opensearch.search.relevance.preprocess.BM25Scorer;
import org.opensearch.search.relevance.preprocess.QueryParser;
import org.opensearch.search.relevance.preprocess.SlidingWindowTextSplitter;
import org.opensearch.search.relevance.preprocess.TextTokenizer;
import org.opensearch.search.suggest.Suggest;
import org.opensearch.tasks.Task;

public class SearchActionFilter implements ActionFilter {

  // TODO: Finalize passage length and sliding window step
  // TODO: Move configs to external config file.
  private static final int PASSAGE_SIZE_LIMIT = 600;
  private static final int SLIDING_WINDOW_STEP = PASSAGE_SIZE_LIMIT - 50;
  private static final int MAXIMUM_PASSAGES = 10;
  private static final double BM25_B_VALUE = 0.75;
  private static final double BM25_K1_VALUE = 1.6;
  private static final int TOP_K_PASSAGES = 3;
//  private static final String EXTERNAL_SERVICE_ENDPOINT = "https://8f56a049-4208-4581-928e-6ffda506bf5a.mock.pstmn.io/rerank"; // mock service
  private static final String TRUE = "true";

  // TODO: Update log levels where required
  private static final Logger LOGGER = LogManager.getLogger(SearchActionFilter.class);

  private final int order;

  private final NamedWriteableRegistry namedWriteableRegistry;
  private final SlidingWindowTextSplitter slidingWindowTextSplitter;
  private final TextTokenizer textTokenizer;
  private final ObjectMapper objectMapper;
  private final CloseableHttpClient httpClient;
  private final QueryParser queryParser;
  private final OpenSearchClient openSearchClient;
  private final KendraHttpClient kendraClient;

  public SearchActionFilter(OpenSearchClient openSearchClient, KendraHttpClient kendraClient) {
    order = 10; // TODO: Finalize this value
    namedWriteableRegistry = new NamedWriteableRegistry(Collections.emptyList());
    slidingWindowTextSplitter = new SlidingWindowTextSplitter(PASSAGE_SIZE_LIMIT, SLIDING_WINDOW_STEP, MAXIMUM_PASSAGES);
    textTokenizer = new TextTokenizer();
    objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    httpClient = HttpClientBuilder.create().build();
    queryParser = new QueryParser();
    this.openSearchClient = openSearchClient;
    this.kendraClient = kendraClient;
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

    boolean shouldDoSemanticRerank = shouldDoSemanticRerank(searchRequest);
    if (shouldDoSemanticRerank) {
      // If user has disabled fetching document source, overwrite and enable
      // in order to access document contents for reranking
      boolean pluginEnabledFetchSource = false;
      if (searchRequest.source().fetchSource() != null && !searchRequest.source().fetchSource().fetchSource()) {
        searchRequest.source().fetchSource(true);
        pluginEnabledFetchSource = true;
      }

      // Extract semantic query string from request
      final QueryParser.QueryParserResult queryParserResult = queryParser.parse(searchRequest.source().query());
      if (queryParserResult != null) {
        final ActionListener<Response> searchResponseListener = createSearchResponseListener(
            listener, startTime, queryParserResult, pluginEnabledFetchSource);
        chain.proceed(task, action, request, searchResponseListener);
        return;
      }
    }

    chain.proceed(task, action, request, listener);
  }

  private boolean shouldDoSemanticRerank(final SearchRequest searchRequest) {
    if (searchRequest.source() == null) {
      return false;
    }

    // Skip if there is scroll, pagination, or sorting.
    if (searchRequest.scroll() != null || searchRequest.source().from() > 0 ||
        (searchRequest.source().sorts() != null && !searchRequest.source().sorts().isEmpty())) {
      return false;
    }

    final String[] indices = searchRequest.indices();
    // Skip if no or more than 1 indices is specified.
    if (indices == null || indices.length != 1) {
      return false;
    }

    // Check request-level setting
    if (!searchRequest.source().ext().isEmpty()) {
      // Filter ext builders by name
      List<SearchExtBuilder> extBuilders = searchRequest.source().ext().stream()
          .filter(searchExtBuilder -> KendraSearchExtBuilder.NAME.equals(searchExtBuilder.getWriteableName()))
          .collect(Collectors.toList());
      if (!extBuilders.isEmpty()) {
        KendraSearchExtBuilder kendraSearchExtBuilder = (KendraSearchExtBuilder) extBuilders.get(0);
        return kendraSearchExtBuilder.isRankerEnabled();
      }
    }

    // Check index level setting
    Settings settings = openSearchClient.getIndexSettings(indices[0], new String[] { Constants.ENABLED_SETTING_NAME });
    // Skip if plugin enabled flag is not true.
    if (settings == null || !TRUE.equals(settings.get(Constants.ENABLED_SETTING_NAME))) {
      return false;
    }
    return true;
  }

  private <Response extends ActionResponse> ActionListener<Response> createSearchResponseListener(
      final ActionListener<Response> listener,
      final long startTime,
      final QueryParser.QueryParserResult queryParserResult,
      final boolean pluginEnabledFetchSource) {
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

          final SearchHits newHits = doSemanticRerank(queryParserResult, hits, pluginEnabledFetchSource);

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

  private SearchHits doSemanticRerank(
      final QueryParser.QueryParserResult queryParserResult,
      final SearchHits hits,
      final boolean pluginEnabledFetchSource) {
    try {
      LOGGER.info("Beginning semantic reranking of {} search hits", hits.getTotalHits());
      List<Document> originalHits = new ArrayList<>();
      for (SearchHit searchHit : hits.getHits()) {
        Map<String, Object> docSourceMap = searchHit.getSourceAsMap();
        LOGGER.info("Splitting document source into passages");
        List<String> splitPassages = slidingWindowTextSplitter.split(docSourceMap.get(queryParserResult.getBodyFieldName()).toString());
        LOGGER.info("Split document source into {} passages: {}", splitPassages.size(), splitPassages);
        List<List<String>> topPassages = getTopPassages(queryParserResult.getQueryText(), splitPassages);
        LOGGER.info("Top passages {}", topPassages);
        originalHits.add(
            new Document(searchHit.getId(), null, null, null, topPassages, searchHit.getScore())
        );
      }

      final RerankRequest rerankRequest = new RerankRequest(null, queryParserResult.getQueryText(), originalHits);
      final RerankResult rerankResult = kendraClient.rerank(rerankRequest);
      Map<String, SearchHit> idToSearchHitMap = new HashMap<>();
      for (SearchHit searchHit : hits.getHits()) {
        idToSearchHitMap.put(searchHit.getId(), searchHit);
      }
      List<SearchHit> newSearchHits = new ArrayList<>();
      float maxScore = 0;
      for (RerankResultItem rerankResultItem : rerankResult.getResultItems()) {
        SearchHit searchHit = idToSearchHitMap.get(rerankResultItem.getDocumentId());
        if (searchHit == null) {
          LOGGER.warn("Response from external service references hit id {}, which does not exist in original results. Skipping.",
              rerankResultItem.getDocumentId());
          continue;
        }
        if (pluginEnabledFetchSource) {
          // User disabled fetching document source, so remove from response
          searchHit.sourceRef(null);
        }
        searchHit.score(rerankResultItem.getScore());
        maxScore = Math.max(maxScore, rerankResultItem.getScore());
        newSearchHits.add(searchHit);
      }
      return new SearchHits(newSearchHits.toArray(new SearchHit[newSearchHits.size()]), hits.getTotalHits(), maxScore);
    } catch (Exception ex) {
      LOGGER.error("Failed to do semantic re-rank. Returning original search results without re-ranking.", ex);
      return hits;
    }
  }

  private List<List<String>> getTopPassages(final String queryText, final List<String> splitPassages) {
    List<String> query = textTokenizer.tokenize(queryText);
    List<List<String>> passages = textTokenizer.tokenize(splitPassages);
    BM25Scorer bm25Scorer = new BM25Scorer(BM25_B_VALUE, BM25_K1_VALUE, passages);
    PriorityQueue<PassageScore> pq = new PriorityQueue<>(Comparator.comparingDouble(x -> x.getScore()));

    for (int i = 0; i < passages.size(); i++) {
      double score = bm25Scorer.score(query, passages.get(i));
      LOGGER.info("Passage {} has score {}", i, score);
      pq.offer(new PassageScore(score, i));
      if (pq.size() > TOP_K_PASSAGES) {
        // Maintain heap of top K passages
        pq.poll();
      }
    }

    List<List<String>> topPassages = new ArrayList<>();
    while (!pq.isEmpty()) {
      topPassages.add(passages.get(pq.poll().getIndex()));
    }
    Collections.reverse(topPassages); // reverse to order from highest to lowest score
    return topPassages;
  }

//  private RerankResult callExternalServiceRerank(RerankRequest rerankRequest) {
//    return AccessController.doPrivileged(
//        (PrivilegedAction<RerankResult>) () -> {
//          CloseableHttpResponse response = null;
//          try {
//            HttpPost post = new HttpPost(EXTERNAL_SERVICE_ENDPOINT);
//            post.setEntity(new StringEntity(objectMapper.writeValueAsString(rerankRequest)));
//            post.setHeader("Content-type", "application/json");
//            response = httpClient.execute(post);
//            return objectMapper.readValue(response.getEntity().getContent(), RerankResult.class);
//          } catch (Exception ex) {
//            LOGGER.error("Exception executing request.", ex);
//            return null;
//          } finally {
//            if (response != null) {
//              try {
//                response.close();
//              } catch (IOException e) {
//                LOGGER.error("Exception closing response.", e);
//              }
//            }
//          }
//        });
//  }

}
