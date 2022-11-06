/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.common.settings.Setting;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.relevance.configuration.ResultTransformerConfiguration;
import org.opensearch.search.relevance.transformer.ResultTransformer;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.client.KendraHttpClient;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.KendraIntelligentRankingConfiguration;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.PassageScore;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.dto.Document;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.dto.RescoreRequest;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.dto.RescoreResult;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.dto.RescoreResultItem;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess.BM25Scorer;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess.QueryParser;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess.QueryParser.QueryParserResult;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess.SlidingWindowTextSplitter;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess.TextTokenizer;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.KendraIntelligentRankerSettings;

public class KendraIntelligentRanker implements ResultTransformer {

  private static final int PASSAGE_SIZE_LIMIT = 600;
  private static final int SLIDING_WINDOW_STEP = PASSAGE_SIZE_LIMIT - 50;
  private static final int MAXIMUM_PASSAGES = 10;
  private static final double BM25_B_VALUE = 0.75;
  private static final double BM25_K1_VALUE = 1.6;
  private static final int TOP_K_PASSAGES = 3;

  private static final Logger logger = LogManager.getLogger(KendraIntelligentRanker.class);

  private final KendraHttpClient kendraClient;
  private final TextTokenizer textTokenizer;
  private final QueryParser queryParser;

  public KendraIntelligentRanker(KendraHttpClient kendraClient) {
    this.kendraClient = kendraClient;
    this.textTokenizer = new TextTokenizer();
    this.queryParser = new QueryParser();
  }

  @Override
  public List<Setting<?>> getTransformerSettings() {
    return KendraIntelligentRankerSettings.getAllSettings();
  }

  /**
   * Check if search request is eligible for rescore
   * @param request Search Request
   * @return boolean decision on whether to re-rank
   */
  @Override
  public boolean shouldTransform(SearchRequest request, ResultTransformerConfiguration configuration) {
    if (request.source() == null) {
      return false;
    }

    // Skip if there is scroll, pagination, or sorting.
    if (request.scroll() != null || request.source().from() > 0 ||
        (request.source().sorts() != null && !request.source().sorts().isEmpty())) {
      return false;
    }
    return true;
  }

  /**
   *
   * @param hits Search hits to rerank with respect to query
   * @param request Search request
   * @return SearchHits reranked search hits
   */
  @Override
  public SearchHits transform(final SearchHits hits,
      final SearchRequest request,
      final ResultTransformerConfiguration configuration) {
    KendraIntelligentRankingConfiguration kendraConfig = (KendraIntelligentRankingConfiguration) configuration;
    QueryParserResult queryParserResult = queryParser.parse(
        request.source().query(),
        kendraConfig.getProperties().getBodyFields(),
        kendraConfig.getProperties().getTitleFields());
    if (queryParserResult == null) {
      return hits;
    }
    try {
      List<Document> originalHits = new ArrayList<>();
      for (SearchHit searchHit : hits.getHits()) {
        Map<String, Object> docSourceMap = searchHit.getSourceAsMap();
        SlidingWindowTextSplitter textSplitter = new SlidingWindowTextSplitter(PASSAGE_SIZE_LIMIT, SLIDING_WINDOW_STEP, MAXIMUM_PASSAGES);
        List<String> splitPassages = textSplitter.split(docSourceMap.get(queryParserResult.getBodyFieldName()).toString());
        List<List<String>> topPassages = getTopPassages(queryParserResult.getQueryText(), splitPassages);
        List<String> tokenizedTitle = null;
        if (queryParserResult.getTitleFieldName() != null) {
          tokenizedTitle = textTokenizer.tokenize(docSourceMap.get(queryParserResult.getTitleFieldName()).toString());
        }
        for (int i = 0; i < topPassages.size(); i++) {
          originalHits.add(
              new Document(searchHit.getId() + "@" + (i + 1), searchHit.getId(), tokenizedTitle, topPassages.get(i), searchHit.getScore())
          );
        }
      }

      final RescoreRequest rescoreRequest = new RescoreRequest(queryParserResult.getQueryText(), originalHits);
      final RescoreResult rescoreResult = kendraClient.rescore(rescoreRequest);
      Map<String, SearchHit> idToSearchHitMap = new HashMap<>();
      for (SearchHit searchHit : hits.getHits()) {
        idToSearchHitMap.put(searchHit.getId(), searchHit);
      }
      List<SearchHit> newSearchHits = new ArrayList<>();
      float maxScore = 0;
      for (RescoreResultItem rescoreResultItem : rescoreResult.getResultItems()) {
        SearchHit searchHit = idToSearchHitMap.get(rescoreResultItem.getDocumentId());
        if (searchHit == null) {
          logger.warn("Response from external service references hit id {}, which does not exist in original results. Skipping.",
              rescoreResultItem.getDocumentId());
          continue;
        }
        searchHit.score(rescoreResultItem.getScore());
        maxScore = Math.max(maxScore, rescoreResultItem.getScore());
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
