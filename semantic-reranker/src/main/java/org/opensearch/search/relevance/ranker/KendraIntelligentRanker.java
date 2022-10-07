package org.opensearch.search.relevance.ranker;

import java.util.ArrayList;
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

import org.opensearch.action.search.SearchRequest;
import org.opensearch.common.settings.Settings;
import org.opensearch.search.SearchExtBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.relevance.client.KendraHttpClient;
import org.opensearch.search.relevance.client.OpenSearchClient;
import org.opensearch.search.relevance.constants.Constants;
import org.opensearch.search.relevance.control.KendraSearchExtBuilder;
import org.opensearch.search.relevance.model.PassageScore;
import org.opensearch.search.relevance.model.dto.Document;
import org.opensearch.search.relevance.model.dto.RerankRequest;
import org.opensearch.search.relevance.model.dto.RerankResult;
import org.opensearch.search.relevance.model.dto.RerankResultItem;
import org.opensearch.search.relevance.preprocess.BM25Scorer;
import org.opensearch.search.relevance.preprocess.QueryParser;
import org.opensearch.search.relevance.preprocess.QueryParser.QueryParserResult;
import org.opensearch.search.relevance.preprocess.SlidingWindowTextSplitter;
import org.opensearch.search.relevance.preprocess.TextTokenizer;

public class KendraIntelligentRanker implements Ranker {

  private static final String TRUE = "true";
  private static final int PASSAGE_SIZE_LIMIT = 600;
  private static final int SLIDING_WINDOW_STEP = PASSAGE_SIZE_LIMIT - 50;
  private static final int MAXIMUM_PASSAGES = 10;
  private static final double BM25_B_VALUE = 0.75;
  private static final double BM25_K1_VALUE = 1.6;
  private static final int TOP_K_PASSAGES = 3;

  private static final Logger logger = LogManager.getLogger(KendraIntelligentRanker.class);

  private final OpenSearchClient openSearchClient;
  private final KendraHttpClient kendraClient;
  private final SlidingWindowTextSplitter slidingWindowTextSplitter;
  private final TextTokenizer textTokenizer;
  private final CloseableHttpClient httpClient;
  private final QueryParser queryParser;

  public KendraIntelligentRanker(OpenSearchClient openSearchClient,
      KendraHttpClient kendraClient) {
    this.openSearchClient = openSearchClient;
    this.kendraClient = kendraClient;
    this.slidingWindowTextSplitter = new SlidingWindowTextSplitter(PASSAGE_SIZE_LIMIT, SLIDING_WINDOW_STEP, MAXIMUM_PASSAGES);
    this.textTokenizer = new TextTokenizer();
    this.httpClient = HttpClientBuilder.create().build();
    this.queryParser = new QueryParser();
  }

  /**
   *
   * @param request
   * @return
   */
  @Override
  public boolean shouldRerank(SearchRequest request) {
    if (request.source() == null) {
      return false;
    }

    // Skip if there is scroll, pagination, or sorting.
    if (request.scroll() != null || request.source().from() > 0 ||
        (request.source().sorts() != null && !request.source().sorts().isEmpty())) {
      return false;
    }

    final String[] indices = request.indices();
    // Skip if no or more than 1 indices is specified.
    if (indices == null || indices.length != 1) {
      return false;
    }

    // Check request-level setting
    if (!request.source().ext().isEmpty()) {
      // Filter ext builders by name
      List<SearchExtBuilder> extBuilders = request.source().ext().stream()
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

  /**
   *
   * @param hits Search hits to rerank with respect to query
   * @param queryParserResult parsed query
   * @return SearchHits reranked search hits
   */
  @Override
  public SearchHits rank(final SearchHits hits,
      QueryParserResult queryParserResult) {
    try {
      List<Document> originalHits = new ArrayList<>();
      for (SearchHit searchHit : hits.getHits()) {
        Map<String, Object> docSourceMap = searchHit.getSourceAsMap();
        List<String> splitPassages = slidingWindowTextSplitter.split(docSourceMap.get(queryParserResult.getBodyFieldName()).toString());
        List<List<String>> topPassages = getTopPassages(queryParserResult.getQueryText(), splitPassages);
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
          logger.warn("Response from external service references hit id {}, which does not exist in original results. Skipping.",
              rerankResultItem.getDocumentId());
          continue;
        }
        searchHit.score(rerankResultItem.getScore());
        maxScore = Math.max(maxScore, rerankResultItem.getScore());
        newSearchHits.add(searchHit);
      }
      return new SearchHits(newSearchHits.toArray(new SearchHit[newSearchHits.size()]), hits.getTotalHits(), maxScore);
    } catch (Exception ex) {
      logger.error("Failed to re-rank. Returning original search results without re-ranking.", ex);
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
      logger.info("Passage {} has score {}", i, score);
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
}
